package com.codealpha.stockly.controller;

import com.codealpha.stockly.service.AlphaVantageDailyDataScheduler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/market-data")
public class AdminMarketDataController {

    private final AlphaVantageDailyDataScheduler scheduler;

    public AdminMarketDataController(
            AlphaVantageDailyDataScheduler scheduler
    ) {
        this.scheduler = scheduler;
    }

    @PostMapping("/sync")
    public String syncMarketData() {

        scheduler.refreshDailyMarketData();

        return "Market data synchronization triggered.";
    }

    @PostMapping("/backfill-history")
    public String backfillHistory() {

        scheduler.backfillDailyHistory();

        return "Historical market data backfill triggered.";
    }
}