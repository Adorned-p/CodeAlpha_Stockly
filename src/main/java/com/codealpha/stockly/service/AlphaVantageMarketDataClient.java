package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.AlphaVantageDailyResponse;
import com.codealpha.stockly.dto.AlphaVantageQuoteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AlphaVantageMarketDataClient {

    private final RestClient restClient;
    private final String apiKey;

    public AlphaVantageMarketDataClient(
            @Value("${alphavantage.base-url}") String baseUrl,
            @Value("${alphavantage.api-key}") String apiKey
    ) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }

    // =========================================================
    // GLOBAL QUOTE
    // =========================================================

    public AlphaVantageQuoteResponse getQuote(
            String symbol
    ) {

        if (symbol == null ||
                symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol is required"
            );
        }

        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/query")
                                .queryParam(
                                        "function",
                                        "GLOBAL_QUOTE"
                                )
                                .queryParam(
                                        "symbol",
                                        symbol.toUpperCase()
                                )
                                .queryParam(
                                        "apikey",
                                        apiKey
                                )
                                .build()
                )
                .retrieve()
                .body(
                        AlphaVantageQuoteResponse.class
                );
    }

    // =========================================================
    // DAILY HISTORICAL DATA
    // =========================================================

    public AlphaVantageDailyResponse getDailyHistory(
            String symbol
    ) {

        if (symbol == null ||
                symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol is required"
            );
        }

        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/query")
                                .queryParam(
                                        "function",
                                        "TIME_SERIES_DAILY"
                                )
                                .queryParam(
                                        "symbol",
                                        symbol.toUpperCase()
                                )
                                .queryParam(
                                        "outputsize",
                                        "compact"
                                )
                                .queryParam(
                                        "apikey",
                                        apiKey
                                )
                                .build()
                )
                .retrieve()
                .body(
                        AlphaVantageDailyResponse.class
                );
    }

    // =========================================================
    // SYMBOL SEARCH
    // =========================================================

    public String searchSymbols(
            String query
    ) {

        if (query == null ||
                query.isBlank()) {

            throw new IllegalArgumentException(
                    "Search query is required"
            );
        }

        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/query")
                                .queryParam(
                                        "function",
                                        "SYMBOL_SEARCH"
                                )
                                .queryParam(
                                        "keywords",
                                        query
                                )
                                .queryParam(
                                        "apikey",
                                        apiKey
                                )
                                .build()
                )
                .retrieve()
                .body(String.class);
    }
}