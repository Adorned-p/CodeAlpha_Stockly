package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.MarketDataStatus;
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
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RealMarketDataScheduler {

    private final InstrumentRepository instrumentRepository;
    private final MarketQuoteRepository marketQuoteRepository;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;
    private final StockRepository stockRepository;
    private final MarketDataProviderService marketDataProviderService;

    /*
     * Keeps track of which instrument should be updated next.
     *
     * Example:
     *
     * 0 -> TCS
     * 1 -> AAPL
     * 2 -> MSFT
     * 3 -> NVDA
     * 4 -> AMZN
     * 5 -> GOOGL
     * 6 -> back to TCS
     */
    private int currentInstrumentIndex = 0;

    public RealMarketDataScheduler(
            InstrumentRepository instrumentRepository,
            MarketQuoteRepository marketQuoteRepository,
            StockPriceHistoryRepository stockPriceHistoryRepository,
            StockRepository stockRepository,
            MarketDataProviderService marketDataProviderService
    ) {
        this.instrumentRepository = instrumentRepository;
        this.marketQuoteRepository = marketQuoteRepository;
        this.stockPriceHistoryRepository = stockPriceHistoryRepository;
        this.stockRepository = stockRepository;
        this.marketDataProviderService = marketDataProviderService;
    }

    // =========================================================
    // REAL MARKET DATA
    // =========================================================

    /*
     * Run once every 2 minutes.
     *
     * Only ONE instrument is requested per execution.
     *
     * This keeps Twelve Data usage within the free-plan
     * request limit while continuously rotating through
     * active instruments.
     */
    @Scheduled(fixedRate = 120000)
    @Transactional
    public void refreshMarketData() {

        System.out.println(
                "[REAL MARKET SCHEDULER] Running..."
        );

        List<Instrument> instruments =
                instrumentRepository.findByActiveTrue();

        if (instruments.isEmpty()) {

            System.out.println(
                    "No active instruments available."
            );

            return;
        }

        /*
         * Protect against the list changing while
         * the application is running.
         */
        if (currentInstrumentIndex >= instruments.size()) {

            currentInstrumentIndex = 0;
        }

        Instrument instrument =
                instruments.get(currentInstrumentIndex);

        currentInstrumentIndex++;


        try {

            updateInstrument(instrument);

        } catch (Exception exception) {

            String message =
                    exception.getMessage() == null
                            ? ""
                            : exception.getMessage();

            System.err.println(
                    "Failed to update market data for "
                            + instrument.getSymbol()
                            + ": "
                            + message
            );
        }
    }

    // =========================================================
    // UPDATE ONE INSTRUMENT
    // =========================================================

    private void updateInstrument(
            Instrument instrument
    ) {

        /*
         * Use the provider service instead of calling
         * Twelve Data directly.
         *
         * This gives us:
         *
         * Twelve Data
         *      ↓
         * Alpha Vantage fallback
         */
        ExternalQuoteResponse externalQuote =
                marketDataProviderService.getQuote(
                        instrument
                );

        if (externalQuote == null) {

            instrument.setMarketDataStatus(
                    MarketDataStatus.UNAVAILABLE
            );

            instrumentRepository.save(
                    instrument
            );

            System.err.println(
                    "No market data received for "
                            + instrument.getSymbol()
            );

            return;
        }

        BigDecimal lastPrice =
                externalQuote.getClose();

        if (lastPrice == null ||
                lastPrice.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            instrument.setMarketDataStatus(
                    MarketDataStatus.UNAVAILABLE
            );

            instrumentRepository.save(
                    instrument
            );

            System.err.println(
                    "Invalid price received for "
                            + instrument.getSymbol()
            );

            return;
        }

        LocalDateTime now =
                LocalDateTime.now();

        instrument.setMarketDataStatus(
                MarketDataStatus.LIVE
        );

        instrument.setMarketDataUpdatedAt(
                now
        );

        // =====================================================
        // UPDATE INSTRUMENT
        // =====================================================

        instrument.setCurrentPrice(
                lastPrice
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
                    lastPrice
            );

            // =====================================================
// OPENING PRICE
// =====================================================
//
// IndianAPI does not currently give us a verified
// opening price in the response we are using.
//
// Therefore:
// - use the provider opening price if available
// - otherwise use the latest market price
//
// This also prevents an old/stale database value
// from remaining in Stock.openingPrice.
//

            if (externalQuote.getOpen() != null &&
                    externalQuote.getOpen().compareTo(
                            BigDecimal.ZERO
                    ) > 0) {

                stock.setOpeningPrice(
                        externalQuote.getOpen()
                );

            } else {

                stock.setOpeningPrice(
                        lastPrice
                );
            }

            if (externalQuote.getPreviousClose() != null) {

                stock.setPreviousClose(
                        externalQuote.getPreviousClose()
                );
            }

            if (externalQuote.getHigh() != null) {

                stock.setDayHigh(
                        externalQuote.getHigh()
                );
            }

            if (externalQuote.getLow() != null) {

                stock.setDayLow(
                        externalQuote.getLow()
                );
            }

            stockRepository.save(stock);

            // =================================================
            // SAVE PRICE HISTORY
            // =================================================

            StockPriceHistory history =
                    new StockPriceHistory();

            history.setStock(stock);

            history.setPrice(
                    lastPrice
            );

            history.setRecordedAt(
                    now
            );

            stockPriceHistoryRepository.save(
                    history
            );
        }

        // =====================================================
        // UPDATE MARKET QUOTE
        // =====================================================

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

        /*
         * Twelve Data quote data currently gives us
         * the latest traded price rather than a true
         * bid/ask feed.
         *
         * Therefore both temporarily use the latest price.
         */
        quote.setBidPrice(
                lastPrice
        );

        quote.setAskPrice(
                lastPrice
        );

        quote.setLastPrice(
                lastPrice
        );

        quote.setUpdatedAt(
                now
        );

        marketQuoteRepository.save(
                quote
        );

        // =====================================================
        // LOG
        // =====================================================

        System.out.println(
                "Real market data updated: "
                        + instrument.getSymbol()
                        + " = "
                        + lastPrice
        );
    }
}