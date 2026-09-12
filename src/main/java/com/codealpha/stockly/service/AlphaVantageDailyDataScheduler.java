package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.AlphaVantageDailyResponse;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.MarketQuote;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.repository.InstrumentRepository;
import com.codealpha.stockly.repository.MarketQuoteRepository;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import com.codealpha.stockly.repository.StockRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AlphaVantageDailyDataScheduler {

    private final InstrumentRepository instrumentRepository;
    private final MarketQuoteRepository marketQuoteRepository;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;
    private final StockRepository stockRepository;
    private final AlphaVantageMarketDataClient alphaVantageMarketDataClient;

    private int currentInstrumentIndex = 0;

    /*
     * Alpha Vantage free tier safety limit.
     */
    private static final int MAX_DAILY_REQUESTS = 20;

    private LocalDate requestCountDate = LocalDate.now();

    private int dailyRequestCount = 0;

    public AlphaVantageDailyDataScheduler(
            InstrumentRepository instrumentRepository,
            MarketQuoteRepository marketQuoteRepository,
            StockPriceHistoryRepository stockPriceHistoryRepository,
            StockRepository stockRepository,
            AlphaVantageMarketDataClient alphaVantageMarketDataClient
    ) {
        this.instrumentRepository =
                instrumentRepository;

        this.marketQuoteRepository =
                marketQuoteRepository;

        this.stockPriceHistoryRepository =
                stockPriceHistoryRepository;

        this.stockRepository =
                stockRepository;

        this.alphaVantageMarketDataClient =
                alphaVantageMarketDataClient;
    }

    // =========================================================
    // DAILY MARKET DATA
    // =========================================================

    /*
     * Run once every hour.
     *
     * Only ONE instrument is requested per execution.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void refreshDailyMarketData() {

        resetDailyCounterIfNecessary();

        if (dailyRequestCount >= MAX_DAILY_REQUESTS) {

            System.out.println(
                    "Alpha Vantage daily request limit reached. "
                            + "Skipping until tomorrow."
            );

            return;
        }

        List<Instrument> instruments =
                instrumentRepository
                        .findByActiveTrue()
                        .stream()
                        .filter(instrument ->
                                instrument.getAlphaVantageSymbol() != null
                                        &&
                                        !instrument
                                                .getAlphaVantageSymbol()
                                                .isBlank()
                        )
                        .toList();

        if (instruments.isEmpty()) {

            System.out.println(
                    "No active instruments with Alpha Vantage symbols."
            );

            return;
        }

        if (currentInstrumentIndex >= instruments.size()) {

            currentInstrumentIndex = 0;
        }

        Instrument instrument =
                instruments.get(
                        currentInstrumentIndex
                );

        currentInstrumentIndex++;

        String alphaVantageSymbol =
                instrument.getAlphaVantageSymbol();

        try {

            dailyRequestCount++;

            System.out.println(
                    "Alpha Vantage daily request "
                            + dailyRequestCount
                            + "/"
                            + MAX_DAILY_REQUESTS
                            + " for "
                            + instrument.getSymbol()
                            + " -> "
                            + alphaVantageSymbol
            );

            AlphaVantageDailyResponse response =
                    alphaVantageMarketDataClient
                            .getDailyHistory(
                                    alphaVantageSymbol
                            );

            processDailyData(
                    instrument,
                    response
            );

        } catch (Exception exception) {

            System.err.println(
                    "Alpha Vantage daily update failed for "
                            + instrument.getSymbol()
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // PROCESS DAILY DATA
    // =========================================================

    protected void processDailyData(
            Instrument instrument,
            AlphaVantageDailyResponse response
    ) {

        if (response == null ||
                response.getTimeSeries() == null ||
                response.getTimeSeries().isEmpty()) {

            System.err.println(
                    "No daily data returned for "
                            + instrument.getSymbol()
            );

            return;
        }

        Map<String, AlphaVantageDailyResponse.DailyData>
                timeSeries =
                response.getTimeSeries();

        /*
         * Sort dates newest -> oldest.
         */
        List<String> dates =
                new ArrayList<>(
                        timeSeries.keySet()
                );

        dates.sort(
                Comparator.reverseOrder()
        );

        String latestDateText =
                dates.get(0);

        AlphaVantageDailyResponse.DailyData latest =
                timeSeries.get(
                        latestDateText
                );

        if (latest == null) {
            return;
        }

        BigDecimal close =
                toBigDecimal(
                        latest.getClose()
                );

        BigDecimal open =
                toBigDecimal(
                        latest.getOpen()
                );

        BigDecimal high =
                toBigDecimal(
                        latest.getHigh()
                );

        BigDecimal low =
                toBigDecimal(
                        latest.getLow()
                );

        if (close == null ||
                close.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            System.err.println(
                    "Invalid Alpha Vantage close price for "
                            + instrument.getSymbol()
            );

            return;
        }

        // =====================================================
        // PREVIOUS TRADING DAY CLOSE
        // =====================================================

        BigDecimal previousClose = null;

        if (dates.size() > 1) {

            AlphaVantageDailyResponse.DailyData previous =
                    timeSeries.get(
                            dates.get(1)
                    );

            if (previous != null) {

                previousClose =
                        toBigDecimal(
                                previous.getClose()
                        );
            }
        }

        // =====================================================
        // UPDATE INSTRUMENT
        // =====================================================

        instrument.setCurrentPrice(
                close
        );

        instrumentRepository.save(
                instrument
        );

        Stock stock =
                instrument.getStock();

        if (stock != null) {

            // =================================================
            // UPDATE STOCK
            // =================================================

            stock.setCurrentPrice(
                    close
            );

            if (open != null) {

                stock.setOpeningPrice(
                        open
                );
            }

            if (previousClose != null) {

                stock.setPreviousClose(
                        previousClose
                );
            }

            if (high != null) {

                stock.setDayHigh(
                        high
                );
            }

            if (low != null) {

                stock.setDayLow(
                        low
                );
            }

            stockRepository.save(
                    stock
            );

            // =================================================
            // SAVE LATEST DAILY HISTORY
            // =================================================

            LocalDate latestTradingDate =
                    LocalDate.parse(
                            latestDateText
                    );

            LocalDateTime recordedAt =
                    LocalDateTime.of(
                            latestTradingDate,
                            LocalTime.MAX
                    );

            saveDailyHistoryIfNeeded(
                    stock,
                    close,
                    recordedAt
            );
        }

        // =====================================================
        // UPDATE MARKET QUOTE
        // =====================================================

        MarketQuote quote =
                marketQuoteRepository
                        .findByInstrument(
                                instrument
                        )
                        .orElseGet(() -> {

                            MarketQuote newQuote =
                                    new MarketQuote();

                            newQuote.setInstrument(
                                    instrument
                            );

                            return newQuote;
                        });

        /*
         * Alpha Vantage daily data doesn't provide
         * a real bid/ask feed.
         *
         * Therefore use EOD close internally.
         */
        quote.setBidPrice(
                close
        );

        quote.setAskPrice(
                close
        );

        quote.setLastPrice(
                close
        );

        quote.setUpdatedAt(
                LocalDateTime.of(
                        LocalDate.parse(
                                latestDateText
                        ),
                        LocalTime.MAX
                )
        );

        marketQuoteRepository.save(
                quote
        );

        System.out.println(
                "Alpha Vantage daily data updated: "
                        + instrument.getSymbol()
                        + " = "
                        + close
                        + " ("
                        + latestDateText
                        + ")"
        );
    }

    // =========================================================
    // HISTORICAL DATA BACKFILL
    // =========================================================

    /*
     * Downloads the available Alpha Vantage daily history
     * for every eligible instrument.
     *
     * Each instrument requires only ONE API request.
     *
     * The existing daily request limit is respected.
     */
    @Transactional
    public void backfillDailyHistory() {

        resetDailyCounterIfNecessary();

        if (dailyRequestCount >= MAX_DAILY_REQUESTS) {

            System.out.println(
                    "Alpha Vantage daily request limit already reached. "
                            + "Historical backfill skipped."
            );

            return;
        }

        List<Instrument> instruments =
                instrumentRepository
                        .findByActiveTrue()
                        .stream()
                        .filter(instrument ->
                                instrument.getAlphaVantageSymbol() != null
                                        &&
                                        !instrument
                                                .getAlphaVantageSymbol()
                                                .isBlank()
                        )
                        .toList();

        if (instruments.isEmpty()) {

            System.out.println(
                    "No active instruments with Alpha Vantage symbols "
                            + "available for historical backfill."
            );

            return;
        }

        int processed = 0;

        for (Instrument instrument : instruments) {

            /*
             * Never exceed our daily safety limit.
             */
            if (dailyRequestCount >= MAX_DAILY_REQUESTS) {

                System.out.println(
                        "Alpha Vantage daily request limit reached "
                                + "during historical backfill."
                );

                break;
            }

            String alphaVantageSymbol =
                    instrument.getAlphaVantageSymbol();

            try {

                dailyRequestCount++;

                System.out.println(
                        "Alpha Vantage historical request "
                                + dailyRequestCount
                                + "/"
                                + MAX_DAILY_REQUESTS
                                + " for "
                                + instrument.getSymbol()
                                + " -> "
                                + alphaVantageSymbol
                );

                AlphaVantageDailyResponse response =
                        alphaVantageMarketDataClient
                                .getDailyHistory(
                                        alphaVantageSymbol
                                );

                int saved =
                        processHistoricalData(
                                instrument,
                                response
                        );

                processed++;

                System.out.println(
                        "Historical backfill completed for "
                                + instrument.getSymbol()
                                + ". Records added: "
                                + saved
                );

            } catch (Exception exception) {

                System.err.println(
                        "Historical backfill failed for "
                                + instrument.getSymbol()
                                + ": "
                                + exception.getMessage()
                );
            }
        }

        System.out.println(
                "Alpha Vantage historical backfill finished. "
                        + "Instruments processed: "
                        + processed
                        + ", Requests used today: "
                        + dailyRequestCount
                        + "/"
                        + MAX_DAILY_REQUESTS
        );
    }

    // =========================================================
    // PROCESS HISTORICAL DATA
    // =========================================================

    /*
     * Process ALL daily records returned by Alpha Vantage.
     *
     * This method is used only by the historical backfill.
     */
    private int processHistoricalData(
            Instrument instrument,
            AlphaVantageDailyResponse response
    ) {

        if (response == null ||
                response.getTimeSeries() == null ||
                response.getTimeSeries().isEmpty()) {

            System.err.println(
                    "No historical data returned for "
                            + instrument.getSymbol()
            );

            return 0;
        }

        Stock stock =
                instrument.getStock();

        if (stock == null) {

            System.err.println(
                    "No stock linked to instrument "
                            + instrument.getSymbol()
            );

            return 0;
        }

        Map<String, AlphaVantageDailyResponse.DailyData>
                timeSeries =
                response.getTimeSeries();

        int saved = 0;

        for (
                Map.Entry<
                        String,
                        AlphaVantageDailyResponse.DailyData
                        > entry :
                timeSeries.entrySet()
        ) {

            String dateText =
                    entry.getKey();

            AlphaVantageDailyResponse.DailyData dailyData =
                    entry.getValue();

            if (dailyData == null) {
                continue;
            }

            BigDecimal close =
                    toBigDecimal(
                            dailyData.getClose()
                    );

            if (close == null ||
                    close.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                continue;
            }

            LocalDate tradingDate;

            try {

                tradingDate =
                        LocalDate.parse(
                                dateText
                        );

            } catch (Exception exception) {

                System.err.println(
                        "Invalid Alpha Vantage date "
                                + dateText
                                + " for "
                                + instrument.getSymbol()
                );

                continue;
            }

            LocalDateTime recordedAt =
                    LocalDateTime.of(
                            tradingDate,
                            LocalTime.MAX
                    );

            /*
             * Don't create duplicate history records.
             */
            if (stockPriceHistoryRepository
                    .existsByStockAndRecordedAt(
                            stock,
                            recordedAt
                    )) {

                continue;
            }

            StockPriceHistory history =
                    new StockPriceHistory();

            history.setStock(
                    stock
            );

            history.setPrice(
                    close
            );

            history.setRecordedAt(
                    recordedAt
            );

            stockPriceHistoryRepository.save(
                    history
            );

            saved++;
        }

        return saved;
    }

    // =========================================================
    // SAVE DAILY HISTORY
    // =========================================================

    private void saveDailyHistoryIfNeeded(
            Stock stock,
            BigDecimal close,
            LocalDateTime recordedAt
    ) {

        boolean alreadyExists =
                stockPriceHistoryRepository
                        .existsByStockAndRecordedAt(
                                stock,
                                recordedAt
                        );

        if (alreadyExists) {
            return;
        }

        StockPriceHistory dailyHistory =
                new StockPriceHistory();

        dailyHistory.setStock(
                stock
        );

        dailyHistory.setPrice(
                close
        );

        dailyHistory.setRecordedAt(
                recordedAt
        );

        stockPriceHistoryRepository.save(
                dailyHistory
        );
    }

    // =========================================================
    // BIG DECIMAL CONVERSION
    // =========================================================

    private BigDecimal toBigDecimal(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        try {

            return new BigDecimal(
                    value.trim()
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    // =========================================================
    // RESET DAILY REQUEST COUNTER
    // =========================================================

    private void resetDailyCounterIfNecessary() {

        LocalDate today =
                LocalDate.now();

        if (!today.equals(
                requestCountDate
        )) {

            requestCountDate =
                    today;

            dailyRequestCount =
                    0;

            currentInstrumentIndex =
                    0;

            System.out.println(
                    "Alpha Vantage daily request counter reset."
            );
        }
    }

    // =========================================================
    // ALPHA VANTAGE SYMBOL BACKFILL
    // =========================================================

    @Transactional
    public void backfillAlphaVantageSymbols() {

        List<Instrument> instruments =
                instrumentRepository.findAll();

        int updated = 0;

        for (Instrument instrument : instruments) {

            /*
             * Never overwrite an existing provider symbol.
             */
            if (instrument.getAlphaVantageSymbol() != null &&
                    !instrument
                            .getAlphaVantageSymbol()
                            .isBlank()) {

                continue;
            }

            String symbol =
                    instrument.getSymbol();

            String exchange =
                    instrument.getExchange();

            if (symbol == null ||
                    exchange == null) {

                continue;
            }

            String normalizedSymbol =
                    symbol
                            .trim()
                            .toUpperCase();

            String normalizedExchange =
                    exchange
                            .trim()
                            .toUpperCase();

            String alphaSymbol = null;

            /*
             * US exchanges use the normal ticker.
             */
            if (normalizedExchange.equals("NASDAQ") ||
                    normalizedExchange.equals("NYSE")) {

                alphaSymbol =
                        normalizedSymbol;
            }

            /*
             * BSE uses .BSE.
             */
            else if (normalizedExchange.equals("BSE")) {

                alphaSymbol =
                        normalizedSymbol + ".BSE";
            }

            /*
             * NSE intentionally remains NULL.
             */
            else if (normalizedExchange.equals("NSE")) {

                System.out.println(
                        "Skipping NSE instrument: "
                                + normalizedSymbol
                );
            }

            if (alphaSymbol != null) {

                instrument.setAlphaVantageSymbol(
                        alphaSymbol
                );

                instrumentRepository.save(
                        instrument
                );

                updated++;

                System.out.println(
                        "Alpha Vantage symbol assigned: "
                                + normalizedSymbol
                                + " -> "
                                + alphaSymbol
                );
            }
        }

        System.out.println(
                "Alpha Vantage symbol backfill completed. "
                        + "Updated: "
                        + updated
        );
    }
}