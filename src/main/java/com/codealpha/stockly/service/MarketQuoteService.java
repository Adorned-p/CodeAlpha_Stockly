package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.MarketCandleResponse;
import com.codealpha.stockly.dto.MarketQuoteResponse;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.MarketQuote;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.repository.InstrumentRepository;
import com.codealpha.stockly.repository.MarketQuoteRepository;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketQuoteService {

    private final MarketQuoteRepository marketQuoteRepository;
    private final InstrumentRepository instrumentRepository;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;

    public MarketQuoteService(
            MarketQuoteRepository marketQuoteRepository,
            InstrumentRepository instrumentRepository,
            StockPriceHistoryRepository stockPriceHistoryRepository
    ) {
        this.marketQuoteRepository = marketQuoteRepository;
        this.instrumentRepository = instrumentRepository;
        this.stockPriceHistoryRepository = stockPriceHistoryRepository;
    }

    // =========================================================
    // CURRENT QUOTE
    // =========================================================

    public MarketQuoteResponse getQuote(String symbol) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        Instrument instrument =
                instrumentRepository
                        .findBySymbol(normalizedSymbol)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Instrument not found: "
                                                + normalizedSymbol
                                )
                        );

        MarketQuote quote =
                marketQuoteRepository
                        .findByInstrument(instrument)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Market quote not available for "
                                                + normalizedSymbol
                                )
                        );

        BigDecimal lastPrice =
                quote.getLastPrice();

        if (lastPrice == null ||
                lastPrice.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Invalid market price available for "
                            + normalizedSymbol
            );
        }

        Stock stock =
                instrument.getStock();

        BigDecimal openingPrice =
                stock != null &&
                        stock.getOpeningPrice() != null
                        ? stock.getOpeningPrice()
                        : lastPrice;

        BigDecimal previousClose =
                stock != null &&
                        stock.getPreviousClose() != null
                        ? stock.getPreviousClose()
                        : lastPrice;

        BigDecimal high =
                stock != null &&
                        stock.getDayHigh() != null
                        ? stock.getDayHigh()
                        : lastPrice;

        BigDecimal low =
                stock != null &&
                        stock.getDayLow() != null
                        ? stock.getDayLow()
                        : lastPrice;

        BigDecimal change =
                lastPrice.subtract(previousClose);

        BigDecimal percentChange =
                BigDecimal.ZERO;

        if (previousClose.compareTo(BigDecimal.ZERO) > 0) {

            percentChange =
                    change
                            .multiply(BigDecimal.valueOf(100))
                            .divide(
                                    previousClose,
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }

        return new MarketQuoteResponse(
                instrument.getSymbol(),
                instrument.getName(),
                instrument.getExchange(),
                instrument.getCurrency(),
                lastPrice,
                openingPrice,
                previousClose,
                high,
                low,
                change,
                percentChange,
                null
        );
    }

    // =========================================================
    // UPDATE QUOTE
    // =========================================================

    @Transactional
    public MarketQuote updateQuote(
            String symbol,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            BigDecimal lastPrice
    ) {

        validatePrices(
                bidPrice,
                askPrice,
                lastPrice
        );

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Symbol is required"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        Instrument instrument =
                instrumentRepository
                        .findBySymbol(normalizedSymbol)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Instrument not found: "
                                                + normalizedSymbol
                                )
                        );

        MarketQuote quote =
                marketQuoteRepository
                        .findByInstrument(instrument)
                        .orElseGet(() -> {

                            MarketQuote newQuote =
                                    new MarketQuote();

                            newQuote.setInstrument(
                                    instrument
                            );

                            return newQuote;
                        });

        quote.setBidPrice(bidPrice);
        quote.setAskPrice(askPrice);
        quote.setLastPrice(lastPrice);
        quote.setUpdatedAt(LocalDateTime.now());

        instrument.setCurrentPrice(lastPrice);

        instrumentRepository.save(instrument);

        return marketQuoteRepository.save(quote);
    }

    // =========================================================
// HISTORICAL DATA
// =========================================================

    public List<MarketCandleResponse> getHistoricalData(
            String symbol,
            String interval,
            int outputSize
    ) {

        // ---------------------------------------------------------
        // VALIDATION
        // ---------------------------------------------------------

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Symbol is required"
            );
        }

        if (interval == null || interval.isBlank()) {
            throw new IllegalArgumentException(
                    "Interval is required"
            );
        }

        if (outputSize < 1 || outputSize > 5000) {
            throw new IllegalArgumentException(
                    "Output size must be between 1 and 5000"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedInterval =
                interval.trim().toLowerCase();

        // ---------------------------------------------------------
        // FIND INSTRUMENT
        // ---------------------------------------------------------

        Instrument instrument =
                instrumentRepository
                        .findBySymbol(normalizedSymbol)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Instrument not found: "
                                                + normalizedSymbol
                                )
                        );

        Stock stock =
                instrument.getStock();

        if (stock == null) {
            throw new IllegalArgumentException(
                    "No stock history is linked to "
                            + normalizedSymbol
            );
        }

        // ---------------------------------------------------------
        // GET HISTORY
        // ---------------------------------------------------------

        List<StockPriceHistory> history =
                stockPriceHistoryRepository
                        .findByStockOrderByRecordedAtDesc(stock);

        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException(
                    "Historical market data not available for "
                            + normalizedSymbol
            );
        }

        // ---------------------------------------------------------
        // CLEAN HISTORY
        // ---------------------------------------------------------

        history =
                history
                        .stream()
                        .filter(item ->
                                item != null
                                        && item.getRecordedAt() != null
                                        && item.getPrice() != null
                                        && item.getPrice().compareTo(
                                        BigDecimal.ZERO
                                ) > 0
                        )
                        .sorted(
                                java.util.Comparator.comparing(
                                        StockPriceHistory::getRecordedAt
                                )
                        )
                        .toList();

        if (history.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid historical market data available for "
                            + normalizedSymbol
            );
        }

        // ---------------------------------------------------------
        // ALL
        // ---------------------------------------------------------

        if ("all".equals(normalizedInterval)) {

            return history
                    .stream()
                    .map(this::toCandle)
                    .toList();
        }

        // ---------------------------------------------------------
        // CONFIGURATION
        // ---------------------------------------------------------

        CandleConfiguration configuration =
                getCandleConfiguration(
                        normalizedInterval
                );

        // ---------------------------------------------------------
        // CALCULATE DATE RANGE
        // ---------------------------------------------------------

        LocalDateTime end =
                LocalDateTime.now();

        LocalDateTime start =
                end.minus(
                        configuration.rangeAmount,
                        configuration.rangeUnit
                );

        /*
         * IMPORTANT:
         *
         * Use final variables inside the stream/lambda.
         * This fixes:
         *
         * "Variable used in lambda expression should be
         * final or effectively final"
         */

        final LocalDateTime filterStart = start;
        final LocalDateTime filterEnd = end;

        // ---------------------------------------------------------
        // FILTER HISTORY BY RANGE
        // ---------------------------------------------------------

        List<StockPriceHistory> filtered =
                history
                        .stream()
                        .filter(item -> {

                            LocalDateTime timestamp =
                                    item.getRecordedAt();

                            return !timestamp.isBefore(
                                    filterStart
                            )
                                    && !timestamp.isAfter(
                                    filterEnd
                            );
                        })
                        .toList();

        /*
         * If there is no data inside the requested range,
         * use the latest available records instead of returning
         * an empty chart.
         */

        if (filtered.isEmpty()) {

            int fromIndex =
                    Math.max(
                            0,
                            history.size() - outputSize
                    );

            filtered =
                    history
                            .subList(
                                    fromIndex,
                                    history.size()
                            );
        }

        // ---------------------------------------------------------
        // CREATE CANDLES
        // ---------------------------------------------------------

        List<MarketCandleResponse> candles;

        switch (configuration.type) {

            case RAW:

                candles =
                        filtered
                                .stream()
                                .map(this::toCandle)
                                .toList();

                break;

            case MINUTE:

                candles =
                        aggregateByMinutes(
                                filtered,
                                configuration.candleMinutes
                        );

                break;

            case HOURLY:

                candles =
                        aggregateHourly(
                                filtered
                        );

                break;

            case DAILY:

                candles =
                        aggregateDaily(
                                filtered
                        );

                break;

            case WEEKLY:

                candles =
                        aggregateWeekly(
                                filtered
                        );

                break;

            case MONTHLY:

                candles =
                        aggregateMonthly(
                                filtered
                        );

                break;

            default:

                throw new IllegalArgumentException(
                        "Unsupported candle type"
                );
        }

        // ---------------------------------------------------------
        // LIMIT OUTPUT
        // ---------------------------------------------------------

        if (candles.size() > outputSize) {

            candles =
                    candles
                            .subList(
                                    candles.size() - outputSize,
                                    candles.size()
                            );
        }

        return candles;
    }

    // =========================================================
    // MINUTE AGGREGATION
    // =========================================================

    private List<MarketCandleResponse> aggregateByMinutes(
            List<StockPriceHistory> history,
            int minutes
    ) {

        Map<LocalDateTime, List<StockPriceHistory>> buckets =
                new LinkedHashMap<>();

        for (StockPriceHistory item : history) {

            LocalDateTime timestamp =
                    item.getRecordedAt();

            int minute =
                    timestamp.getMinute();

            int bucketMinute =
                    (minute / minutes) * minutes;

            LocalDateTime bucket =
                    timestamp
                            .withMinute(bucketMinute)
                            .withSecond(0)
                            .withNano(0);

            buckets
                    .computeIfAbsent(
                            bucket,
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return buildCandles(buckets);
    }

    // =========================================================
    // HOURLY AGGREGATION
    // =========================================================

    private List<MarketCandleResponse> aggregateHourly(
            List<StockPriceHistory> history
    ) {

        Map<LocalDateTime, List<StockPriceHistory>> buckets =
                new LinkedHashMap<>();

        for (StockPriceHistory item : history) {

            LocalDateTime bucket =
                    item.getRecordedAt()
                            .truncatedTo(
                                    ChronoUnit.HOURS
                            );

            buckets
                    .computeIfAbsent(
                            bucket,
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return buildCandles(buckets);
    }

    // =========================================================
    // DAILY AGGREGATION
    // =========================================================

    private List<MarketCandleResponse> aggregateDaily(
            List<StockPriceHistory> history
    ) {

        Map<LocalDateTime, List<StockPriceHistory>> buckets =
                new LinkedHashMap<>();

        for (StockPriceHistory item : history) {

            LocalDateTime bucket =
                    item.getRecordedAt()
                            .toLocalDate()
                            .atStartOfDay();

            buckets
                    .computeIfAbsent(
                            bucket,
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return buildCandles(buckets);
    }

    // =========================================================
    // WEEKLY AGGREGATION
    // =========================================================

    private List<MarketCandleResponse> aggregateWeekly(
            List<StockPriceHistory> history
    ) {

        Map<LocalDateTime, List<StockPriceHistory>> buckets =
                new LinkedHashMap<>();

        for (StockPriceHistory item : history) {

            LocalDate date =
                    item.getRecordedAt()
                            .toLocalDate();

            /*
             * Monday is the first day of the week.
             */
            LocalDate monday =
                    date.with(
                            DayOfWeek.MONDAY
                    );

            LocalDateTime bucket =
                    monday.atStartOfDay();

            buckets
                    .computeIfAbsent(
                            bucket,
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return buildCandles(buckets);
    }

    // =========================================================
    // MONTHLY AGGREGATION
    // =========================================================

    private List<MarketCandleResponse> aggregateMonthly(
            List<StockPriceHistory> history
    ) {

        Map<LocalDateTime, List<StockPriceHistory>> buckets =
                new LinkedHashMap<>();

        for (StockPriceHistory item : history) {

            LocalDateTime bucket =
                    item.getRecordedAt()
                            .toLocalDate()
                            .withDayOfMonth(1)
                            .atStartOfDay();

            buckets
                    .computeIfAbsent(
                            bucket,
                            key -> new ArrayList<>()
                    )
                    .add(item);
        }

        return buildCandles(buckets);
    }

    // =========================================================
    // BUILD OHLC CANDLES
    // =========================================================

    private List<MarketCandleResponse> buildCandles(
            Map<LocalDateTime, List<StockPriceHistory>> buckets
    ) {

        List<MarketCandleResponse> result =
                new ArrayList<>();

        for (
                Map.Entry<
                        LocalDateTime,
                        List<StockPriceHistory>
                        > entry
                : buckets.entrySet()
        ) {

            List<StockPriceHistory> values =
                    entry.getValue();

            if (values == null || values.isEmpty()) {
                continue;
            }

            values =
                    values
                            .stream()
                            .filter(item ->
                                    item != null &&
                                            item.getRecordedAt() != null &&
                                            item.getPrice() != null
                            )
                            .sorted(
                                    Comparator.comparing(
                                            StockPriceHistory::getRecordedAt
                                    )
                            )
                            .toList();

            if (values.isEmpty()) {
                continue;
            }

            // -------------------------------------------------
            // OPEN
            // -------------------------------------------------

            BigDecimal open =
                    values
                            .get(0)
                            .getPrice();

            // -------------------------------------------------
            // CLOSE
            // -------------------------------------------------

            BigDecimal close =
                    values
                            .get(values.size() - 1)
                            .getPrice();

            // -------------------------------------------------
            // HIGH
            // -------------------------------------------------

            BigDecimal high =
                    values
                            .stream()
                            .map(
                                    StockPriceHistory::getPrice
                            )
                            .max(
                                    BigDecimal::compareTo
                            )
                            .orElse(close);

            // -------------------------------------------------
            // LOW
            // -------------------------------------------------

            BigDecimal low =
                    values
                            .stream()
                            .map(
                                    StockPriceHistory::getPrice
                            )
                            .min(
                                    BigDecimal::compareTo
                            )
                            .orElse(close);

            /*
             * StockPriceHistory currently stores price only.
             * There is no actual volume field available here.
             */
            BigDecimal volume =
                    BigDecimal.ZERO;

            /*
             * MarketCandleResponse expects String datetime,
             * therefore LocalDateTime is converted using toString().
             */
            String datetime =
                    entry
                            .getKey()
                            .toString();

            result.add(
                    new MarketCandleResponse(
                            datetime,
                            open,
                            high,
                            low,
                            close,
                            volume
                    )
            );
        }

        return result;
    }

    // =========================================================
    // RAW PRICE -> CANDLE
    // =========================================================

    private MarketCandleResponse toCandle(
            StockPriceHistory history
    ) {

        BigDecimal price =
                history.getPrice();

        String datetime =
                history
                        .getRecordedAt()
                        .toString();

        return new MarketCandleResponse(
                datetime,
                price,
                price,
                price,
                price,
                BigDecimal.ZERO
        );
    }

    // =========================================================
    // CANDLE CONFIGURATION
    // =========================================================

    private CandleConfiguration getCandleConfiguration(
            String interval
    ) {

        switch (interval) {

            case "1min":

                return new CandleConfiguration(
                        CandleType.MINUTE,
                        1,
                        ChronoUnit.DAYS,
                        1
                );

            case "5min":

                return new CandleConfiguration(
                        CandleType.MINUTE,
                        1,
                        ChronoUnit.DAYS,
                        5
                );

            case "15min":

                return new CandleConfiguration(
                        CandleType.MINUTE,
                        1,
                        ChronoUnit.DAYS,
                        15
                );

            case "30min":

                return new CandleConfiguration(
                        CandleType.MINUTE,
                        1,
                        ChronoUnit.DAYS,
                        30
                );

            case "1h":

                return new CandleConfiguration(
                        CandleType.HOURLY,
                        1,
                        ChronoUnit.DAYS
                );

            case "1d":
            case "1day":

                return new CandleConfiguration(
                        CandleType.DAILY,
                        1,
                        ChronoUnit.DAYS
                );

            case "1w":
            case "1week":

                return new CandleConfiguration(
                        CandleType.WEEKLY,
                        7,
                        ChronoUnit.DAYS
                );

            case "1m":
            case "1month":

                return new CandleConfiguration(
                        CandleType.MONTHLY,
                        30,
                        ChronoUnit.DAYS
                );

            case "3m":

                return new CandleConfiguration(
                        CandleType.DAILY,
                        90,
                        ChronoUnit.DAYS
                );

            case "6m":

                return new CandleConfiguration(
                        CandleType.DAILY,
                        180,
                        ChronoUnit.DAYS
                );

            case "1y":

                return new CandleConfiguration(
                        CandleType.WEEKLY,
                        365,
                        ChronoUnit.DAYS
                );

            default:

                throw new IllegalArgumentException(
                        "Unsupported historical interval: "
                                + interval
                                + ". Supported intervals: "
                                + "1min, 5min, 15min, 30min, "
                                + "1h, 1d, 1day, 1w, 1week, "
                                + "1m, 1month, 3m, 6m, 1y, all"
                );
        }
    }

    // =========================================================
    // PRICE VALIDATION
    // =========================================================

    private void validatePrices(
            BigDecimal bidPrice,
            BigDecimal askPrice,
            BigDecimal lastPrice
    ) {

        if (bidPrice == null ||
                askPrice == null ||
                lastPrice == null) {

            throw new IllegalArgumentException(
                    "Market prices cannot be null"
            );
        }

        if (bidPrice.compareTo(BigDecimal.ZERO) <= 0 ||
                askPrice.compareTo(BigDecimal.ZERO) <= 0 ||
                lastPrice.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Market prices must be greater than zero"
            );
        }

        if (bidPrice.compareTo(askPrice) > 0) {

            throw new IllegalArgumentException(
                    "Bid price cannot be greater than ask price"
            );
        }
    }

    // =========================================================
    // INTERNAL CONFIGURATION
    // =========================================================

    private enum CandleType {

        RAW,
        MINUTE,
        HOURLY,
        DAILY,
        WEEKLY,
        MONTHLY
    }

    private static class CandleConfiguration {

        private final CandleType type;

        /*
         * How far back we should look.
         */
        private final long rangeAmount;

        private final ChronoUnit rangeUnit;

        /*
         * Candle size for minute-based candles.
         */
        private final int candleMinutes;

        // For non-minute candles
        private CandleConfiguration(
                CandleType type,
                long rangeAmount,
                ChronoUnit rangeUnit
        ) {
            this.type = type;
            this.rangeAmount = rangeAmount;
            this.rangeUnit = rangeUnit;
            this.candleMinutes = 0;
        }

        // For minute-based candles
        private CandleConfiguration(
                CandleType type,
                long rangeAmount,
                ChronoUnit rangeUnit,
                int candleMinutes
        ) {
            this.type = type;
            this.rangeAmount = rangeAmount;
            this.rangeUnit = rangeUnit;
            this.candleMinutes = candleMinutes;
        }
    }
}