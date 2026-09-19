package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.MarketCandleResponse;
import com.codealpha.stockly.service.HistoricalMarketDataService;
import com.codealpha.stockly.service.MarketQuoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market-data")
public class HistoricalMarketDataController {

    private final MarketQuoteService marketQuoteService;
    private final HistoricalMarketDataService historicalMarketDataService;

    public HistoricalMarketDataController(
            MarketQuoteService marketQuoteService,
            HistoricalMarketDataService historicalMarketDataService
    ) {
        this.marketQuoteService =
                marketQuoteService;

        this.historicalMarketDataService =
                historicalMarketDataService;
    }

    // =========================================================
    // GET HISTORICAL MARKET DATA
    // =========================================================

    @GetMapping("/{symbol}/history")
    public List<MarketCandleResponse> getHistoricalData(
            @PathVariable String symbol,
            @RequestParam String exchange,
            @RequestParam(defaultValue = "1day") String interval,
            @RequestParam(defaultValue = "100") int outputSize
    ) {

        return marketQuoteService.getHistoricalData(
                symbol,
                exchange,
                interval,
                outputSize
        );
    }

    // =========================================================
    // IMPORT HISTORICAL DATA
    // =========================================================

    @PostMapping("/import/{symbol}")
    public int importHistoricalData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1day") String interval,
            @RequestParam(defaultValue = "365") int outputSize
    ) {

        return historicalMarketDataService.importHistoricalData(
                symbol,
                interval,
                outputSize
        );
    }
}