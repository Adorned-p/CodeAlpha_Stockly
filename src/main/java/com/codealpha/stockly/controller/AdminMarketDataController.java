package com.codealpha.stockly.controller;

import com.codealpha.stockly.service.AlphaVantageDailyDataScheduler;
import com.codealpha.stockly.service.RealMarketDataScheduler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/market-data")
public class AdminMarketDataController {

    private final RealMarketDataScheduler realMarketDataScheduler;
    private final AlphaVantageDailyDataScheduler alphaVantageDailyDataScheduler;

    public AdminMarketDataController(
            RealMarketDataScheduler realMarketDataScheduler,
            AlphaVantageDailyDataScheduler alphaVantageDailyDataScheduler
    ) {
        this.realMarketDataScheduler = realMarketDataScheduler;
        this.alphaVantageDailyDataScheduler = alphaVantageDailyDataScheduler;
    }

    // =========================================================
    // LIVE MARKET DATA SYNC
    // =========================================================

    @PostMapping("/sync")
    public String syncMarketData() {

        realMarketDataScheduler.refreshMarketData();

        return "Market data synchronization triggered.";
    }

    // =========================================================
    // HISTORICAL DATA BACKFILL
    // =========================================================

    @PostMapping("/backfill-history")
    public String backfillHistory() {

        alphaVantageDailyDataScheduler.backfillDailyHistory();

        return "Historical market data backfill triggered.";
    }
}