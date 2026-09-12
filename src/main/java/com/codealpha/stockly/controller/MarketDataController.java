package com.codealpha.stockly.controller;

import com.codealpha.stockly.service.MarketDataSyncService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataSyncService marketDataSyncService;

    public MarketDataController(
            MarketDataSyncService marketDataSyncService
    ) {
        this.marketDataSyncService =
                marketDataSyncService;
    }

    @PostMapping("/sync")
    public String syncMarketData() {

        marketDataSyncService.syncAllInstruments();

        return "Market data synchronization completed.";
    }
}