package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.AlphaVantageDailyResponse;
import com.codealpha.stockly.dto.ExternalQuoteResponse;
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
import java.util.Map;
import java.util.List;

@Service
public class AlphaVantageDailyDataScheduler {

    private final InstrumentRepository instrumentRepository;
    private final MarketQuoteRepository marketQuoteRepository;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;
    private final StockRepository stockRepository;
    private final MarketDataProviderService marketDataProviderService;
    private final AlphaVantageMarketDataClient alphaVantageMarketDataClient;

    private int currentInstrumentIndex = 0;

    /*
     * Alpha Vantage is still used for historical backfill.
     *
     * We keep the existing safety limit for historical
     * Alpha Vantage requests.
     */
    private static final int MAX_DAILY_REQUESTS = 20;

    private LocalDate requestCountDate = LocalDate.now();

    private int dailyRequestCount = 0;

    public AlphaVantageDailyDataScheduler(
            InstrumentRepository instrumentRepository,
            MarketQuoteRepository marketQuoteRepository,
            StockPriceHistoryRepository stockPriceHistoryRepository,
            StockRepository stockRepository,
            MarketDataProviderService marketDataProviderService,
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

        this.marketDataProviderService =
                marketDataProviderService;

        this.alphaVantageMarketDataClient =
                alphaVantageMarketDataClient;
    }

    // =========================================================
    // MARKET DATA REFRESH
    // =========================================================

    /*
     * Run once every hour.
     *
     * Only ONE active instrument is requested per execution.
     *
     * The selected provider stored inside Instrument
     * gets the first attempt.
     *
     * If that provider fails, MarketDataProviderService
     * may use a fallback provider.
     */
    @Transactional
    public void refreshDailyMarketData() {

        resetDailyCounterIfNecessary();

        List<Instrument> instruments =
                instrumentRepository
                        .findByActiveTrue()
                        .stream()
                        .filter(this::hasUsableProvider)
                        .toList();

        if (instruments.isEmpty()) {

            System.out.println(
                    "No active instruments with usable "
                            + "market-data provider mappings."
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

        try {

            System.out.println(
                    "Refreshing market data for "
                            + instrument.getSymbol()
                            + " ("
                            + instrument.getExchange()
                            + ") using selected provider: "
                            + instrument.getMarketDataProvider()
            );

            /*
             * MarketDataProviderService decides which provider
             * to use and handles fallback providers.
             */
            ExternalQuoteResponse quote =
                    marketDataProviderService.getQuote(
                            instrument
                    );

            if (!isValidQuote(quote)) {

                System.err.println(
                        "No valid market data received for "
                                + instrument.getSymbol()
                                + " ("
                                + instrument.getExchange()
                                + ")"
                );

                return;
            }

            processQuote(
                    instrument,
                    quote
            );

        } catch (Exception exception) {

            System.err.println(
                    "Market data update failed for "
                            + instrument.getSymbol()
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // PROCESS QUOTE
    // =========================================================

    protected void processQuote(
            Instrument instrument,
            ExternalQuoteResponse quote
    ) {

        if (!isValidQuote(quote)) {

            System.err.println(
                    "Invalid quote for "
                            + instrument.getSymbol()
            );

            return;
        }

        BigDecimal close =
                quote.getClose();

        BigDecimal open =
                quote.getOpen() != null
                        ? quote.getOpen()
                        : close;

        BigDecimal previousClose =
                quote.getPreviousClose() != null
                        ? quote.getPreviousClose()
                        : close;

        BigDecimal high =
                quote.getHigh() != null
                        ? quote.getHigh()
                        : close;

        BigDecimal low =
                quote.getLow() != null
                        ? quote.getLow()
                        : close;

        // =====================================================
        // UPDATE INSTRUMENT
        // =====================================================

        instrument.setCurrentPrice(
                close
        );

        instrument.setMarketDataUpdatedAt(
                LocalDateTime.now()
        );

        instrumentRepository.save(
                instrument
        );

        // =====================================================
        // UPDATE STOCK
        // =====================================================

        Stock stock =
                instrument.getStock();

        if (stock != null) {

            stock.setCurrentPrice(
                    close
            );

            stock.setOpeningPrice(
                    open
            );

            stock.setPreviousClose(
                    previousClose
            );

            stock.setDayHigh(
                    high
            );

            stock.setDayLow(
                    low
            );

            stockRepository.save(
                    stock
            );

            // =================================================
            // SAVE DAILY HISTORY
            // =================================================

            LocalDateTime recordedAt =
                    LocalDateTime.of(
                            LocalDate.now(),
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

        MarketQuote marketQuote =
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
         * If the external provider does not provide bid/ask,
         * use the latest close as the internal simulated
         * bid/ask value.
         */
        marketQuote.setBidPrice(
                close
        );

        marketQuote.setAskPrice(
                close
        );

        marketQuote.setLastPrice(
                close
        );

        marketQuote.setUpdatedAt(
                LocalDateTime.now()
        );

        marketQuoteRepository.save(
                marketQuote
        );

        // =====================================================
        // ACTUAL PROVIDER LOG
        // =====================================================

        String actualProvider =
                quote.getProvider();

        if (actualProvider == null ||
                actualProvider.isBlank()) {

            actualProvider =
                    "UNKNOWN";
        }

        System.out.println(
                "Market data updated successfully: "
                        + instrument.getSymbol()
                        + " = "
                        + close
                        + " using actual provider: "
                        + actualProvider
        );
    }

    // =========================================================
    // HISTORICAL DATA BACKFILL
    // =========================================================

    /*
     * Historical data remains explicitly handled by
     * Alpha Vantage for now.
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
                    "No active instruments with Alpha Vantage "
                            + "symbols available for historical backfill."
            );

            return;
        }

        int processed = 0;

        for (Instrument instrument : instruments) {

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
    // CHECK PROVIDER MAPPING
    // =========================================================

    private boolean hasUsableProvider(
            Instrument instrument
    ) {

        if (instrument == null) {
            return false;
        }

        String provider =
                instrument.getMarketDataProvider();

        if (provider == null ||
                provider.isBlank()) {

            return false;
        }

        String normalizedProvider =
                provider.trim().toUpperCase();

        switch (normalizedProvider) {

            case "EODHD":

                return instrument.getEodhdSymbol() != null
                        && !instrument
                        .getEodhdSymbol()
                        .isBlank();

            case "TWELVE_DATA":

                return instrument.getTwelveDataSymbol() != null
                        && !instrument
                        .getTwelveDataSymbol()
                        .isBlank();

            case "ALPHA_VANTAGE":

                return instrument.getAlphaVantageSymbol() != null
                        && !instrument
                        .getAlphaVantageSymbol()
                        .isBlank();

            case "INDIAN":
            case "INDIAN_MARKET":
            case "INDIAN_MARKET_DATA":

                return instrument.getSymbol() != null
                        && !instrument.getSymbol().isBlank()
                        && instrument.getExchange() != null
                        && !instrument.getExchange().isBlank();

            default:

                return false;
        }
    }

    // =========================================================
    // QUOTE VALIDATION
    // =========================================================

    private boolean isValidQuote(
            ExternalQuoteResponse response
    ) {

        return response != null
                && response.getClose() != null
                && response.getClose()
                .compareTo(
                        BigDecimal.ZERO
                ) > 0;
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