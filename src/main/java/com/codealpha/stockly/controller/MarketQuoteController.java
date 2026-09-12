package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.ExternalSymbolSearchResponse;
import com.codealpha.stockly.dto.MarketCandleResponse;
import com.codealpha.stockly.dto.MarketQuoteResponse;
import com.codealpha.stockly.entity.MarketQuote;
import com.codealpha.stockly.service.ExternalMarketDataClient;
import com.codealpha.stockly.service.MarketQuoteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/market-data")
public class MarketQuoteController {

    private final MarketQuoteService marketQuoteService;
    private final ExternalMarketDataClient externalMarketDataClient;

    public MarketQuoteController(
            MarketQuoteService marketQuoteService,
            ExternalMarketDataClient externalMarketDataClient
    ) {
        this.marketQuoteService = marketQuoteService;
        this.externalMarketDataClient =
                externalMarketDataClient;
    }

    // =========================================================
    // GET CURRENT QUOTE
    // =========================================================

    @GetMapping("/{symbol}/quote")
    public MarketQuoteResponse getQuote(
            @PathVariable String symbol
    ) {

        return marketQuoteService.getQuote(symbol);
    }

    // =========================================================
    // TEMPORARY MANUAL QUOTE UPDATE
    // =========================================================

    @PostMapping("/{symbol}/quote")
    @ResponseStatus(HttpStatus.OK)
    public MarketQuoteResponse updateQuote(
            @PathVariable String symbol,
            @RequestParam BigDecimal bid,
            @RequestParam BigDecimal ask,
            @RequestParam BigDecimal last
    ) {

        MarketQuote quote =
                marketQuoteService.updateQuote(
                        symbol,
                        bid,
                        ask,
                        last
                );

        return new MarketQuoteResponse(
                quote.getInstrument().getSymbol(),
                quote.getBidPrice(),
                quote.getAskPrice(),
                quote.getLastPrice(),
                quote.getUpdatedAt()
        );
    }

    // =========================================================
    // HISTORICAL DATA
    // =========================================================

    @GetMapping("/{symbol}/history")
    public List<MarketCandleResponse> getHistoricalData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1day") String interval,
            @RequestParam(defaultValue = "30") int outputSize
    ) {

        return marketQuoteService.getHistoricalData(
                symbol,
                interval,
                outputSize
        );
    }

    // =========================================================
    // SYMBOL SEARCH
    // =========================================================

    @GetMapping("/search")
    public ExternalSymbolSearchResponse searchSymbols(
            @RequestParam String query
    ) {

        if (query == null || query.isBlank()) {

            throw new IllegalArgumentException(
                    "Search query is required"
            );
        }

        return externalMarketDataClient.searchSymbols(
                query.trim()
        );
    }
}