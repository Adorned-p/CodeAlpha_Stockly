package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalQuoteResponse;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarketDataSyncService {

    private int currentInstrumentIndex = 0;

    private final InstrumentService instrumentService;
    private final ExternalMarketDataClient externalMarketDataClient;
    private final StockPriceHistoryRepository priceHistoryRepository;

    public MarketDataSyncService(
            InstrumentService instrumentService,
            ExternalMarketDataClient externalMarketDataClient,
            StockPriceHistoryRepository priceHistoryRepository
    ) {
        this.instrumentService = instrumentService;
        this.externalMarketDataClient =
                externalMarketDataClient;
        this.priceHistoryRepository =
                priceHistoryRepository;
    }

    // =========================================================
    // MANUAL SYNC ALL ACTIVE INSTRUMENTS
    // =========================================================
    //
    // Used by MarketDataController:
    //
    // POST /api/market-data/sync
    //
    // This method is NOT scheduled.
    // It only runs when explicitly requested.
    // =========================================================

    @Transactional
    public void syncAllInstruments() {

        List<Instrument> instruments =
                instrumentService.getAllInstruments();

        for (Instrument instrument : instruments) {

            try {

                syncInstrument(instrument);

            } catch (Exception e) {

                System.err.println(
                        "Failed to sync "
                                + instrument.getSymbol()
                                + ": "
                                + e.getMessage()
                );
            }
        }
    }

    // =========================================================
    // SYNC ONE INSTRUMENT
    // =========================================================

    @Transactional
    public void syncInstrument(
            Instrument instrument
    ) {

        if (instrument == null) {
            throw new IllegalArgumentException(
                    "Instrument cannot be null"
            );
        }

        ExternalQuoteResponse quote =
                externalMarketDataClient.getQuote(
                        instrument.getSymbol(),
                        instrument.getExchange()
                );

        if (quote == null ||
                quote.getClose() == null) {

            throw new IllegalStateException(
                    "No valid price received for "
                            + instrument.getSymbol()
            );
        }

        BigDecimal currentPrice =
                quote.getClose();

        if (currentPrice.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalStateException(
                    "Invalid price received for "
                            + instrument.getSymbol()
            );
        }

        /*
         * Update Instrument.
         */
        instrumentService.updatePrice(
                instrument.getSymbol(),
                currentPrice
        );

        /*
         * Save price history.
         */
        Stock stock =
                instrument.getStock();

        if (stock != null) {

            StockPriceHistory history =
                    new StockPriceHistory();

            history.setStock(stock);

            history.setPrice(
                    currentPrice
            );

            history.setRecordedAt(
                    LocalDateTime.now()
            );

            priceHistoryRepository.save(
                    history
            );
        }

        System.out.println(
                "Updated "
                        + instrument.getSymbol()
                        + " -> ₹"
                        + currentPrice
        );
    }

    // =========================================================
    // LEGACY SCHEDULED SYNC
    // =========================================================
    //
    // IMPORTANT:
    //
    // RealMarketDataScheduler is now responsible for automatic
    // real market-data synchronization.
    //
    // Therefore this method intentionally DOES NOT call
    // syncAllInstruments().
    //
    // It is kept so existing callers do not break.
    // =========================================================

    public void scheduledMarketDataSync() {

        System.out.println(
                "Legacy scheduled market data sync skipped. "
                        + "RealMarketDataScheduler handles "
                        + "automatic synchronization."
        );
    }

    // =========================================================
    // SYNC NEXT INSTRUMENT
    // =========================================================
    //
    // Kept for existing functionality.
    //
    // This updates only ONE instrument per call instead of
    // consuming API credits for every instrument.
    // =========================================================

    public void syncNextInstrument() {

        List<Instrument> instruments =
                instrumentService.getAllInstruments();

        if (instruments.isEmpty()) {
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

            syncInstrument(instrument);

            System.out.println(
                    "Market data updated: "
                            + instrument.getSymbol()
            );

        } catch (Exception e) {

            System.err.println(
                    "Failed to sync "
                            + instrument.getSymbol()
                            + ": "
                            + e.getMessage()
            );
        }
    }
}