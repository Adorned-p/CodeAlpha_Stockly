package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalExchangeRateResponse;
import com.codealpha.stockly.dto.ExternalQuoteResponse;
import com.codealpha.stockly.dto.ExternalSymbolSearchResponse;
import com.codealpha.stockly.dto.ExternalTimeSeriesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ExternalMarketDataClient {

    private final RestClient restClient;
    private final String apiKey;

    public ExternalMarketDataClient(
            @Value("${twelvedata.base-url}") String baseUrl,
            @Value("${twelvedata.api-key}") String apiKey
    ) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
    }

    // =========================================================
    // QUOTE
    // =========================================================

    public ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    ) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedExchange =
                exchange == null || exchange.isBlank()
                        ? null
                        : exchange.trim().toUpperCase();

        System.out.println(
                "[TWELVE DATA] Requesting quote: symbol="
                        + normalizedSymbol
                        + ", exchange="
                        + normalizedExchange
        );

        return restClient.get()
                .uri(uriBuilder -> {

                    var builder = uriBuilder
                            .path("/quote")
                            .queryParam(
                                    "symbol",
                                    normalizedSymbol
                            )
                            .queryParam(
                                    "apikey",
                                    apiKey
                            );

                    if (normalizedExchange != null) {
                        builder.queryParam(
                                "exchange",
                                normalizedExchange
                        );
                    }

                    return builder.build();
                })
                .retrieve()
                .onStatus(
                        status -> status.value() == 429,
                        (request, response) -> {

                            System.err.println(
                                    "[TWELVE DATA] HTTP status: "
                                            + response.getStatusCode()
                            );

                            throw org.springframework.web.client
                                    .HttpClientErrorException.create(
                                            response.getStatusCode(),
                                            "Twelve Data daily quota exhausted",
                                            response.getHeaders(),
                                            new byte[0],
                                            java.nio.charset.StandardCharsets.UTF_8
                                    );
                        }
                )
                .body(ExternalQuoteResponse.class);
    }

    /*
     * Kept for existing code that only supplies a symbol.
     *
     * Example:
     * AAPL -> symbol=AAPL
     */

    public ExternalQuoteResponse getQuote(
            String symbol
    ) {

        return getQuote(
                symbol,
                null
        );
    }

    // =========================================================
// QUOTE USING PROVIDER-SPECIFIC SYMBOL
// =========================================================

    public ExternalQuoteResponse getQuoteByProviderSymbol(
            String providerSymbol
    ) {

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Twelve Data provider symbol is required"
            );
        }

        String normalizedProviderSymbol =
                providerSymbol.trim().toUpperCase();

        System.out.println(
                "[TWELVE DATA] Requesting provider symbol: "
                        + normalizedProviderSymbol
        );

        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/quote")
                                .queryParam(
                                        "symbol",
                                        normalizedProviderSymbol
                                )
                                .queryParam(
                                        "apikey",
                                        apiKey
                                )
                                .build()
                )
                .retrieve()
                .onStatus(
                        status -> status.value() == 429,
                        (request, response) -> {

                            System.err.println(
                                    "[TWELVE DATA] HTTP status: "
                                            + response.getStatusCode()
                            );

                            throw org.springframework.web.client
                                    .HttpClientErrorException.create(
                                            response.getStatusCode(),
                                            "Twelve Data daily quota exhausted",
                                            response.getHeaders(),
                                            new byte[0],
                                            java.nio.charset.StandardCharsets.UTF_8
                                    );
                        }
                )
                .body(ExternalQuoteResponse.class);
    }

    // =========================================================
    // HISTORICAL DATA
    // =========================================================

    public ExternalTimeSeriesResponse getTimeSeries(
            String symbol,
            String exchange,
            String interval,
            int outputSize
    ) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        return restClient.get()
                .uri(uriBuilder -> {

                    var builder = uriBuilder
                            .path("/time_series")
                            .queryParam(
                                    "symbol",
                                    normalizedSymbol
                            )
                            .queryParam(
                                    "interval",
                                    interval
                            )
                            .queryParam(
                                    "outputsize",
                                    outputSize
                            )
                            .queryParam(
                                    "apikey",
                                    apiKey
                            );

                    /*
                     * Keep exchange separate from symbol
                     * for historical data as well.
                     */

                    if (exchange != null &&
                            !exchange.isBlank()) {

                        builder.queryParam(
                                "exchange",
                                exchange.trim().toUpperCase()
                        );
                    }

                    return builder.build();
                })
                .retrieve()
                .body(
                        ExternalTimeSeriesResponse.class
                );
    }

    // =========================================================
    // SYMBOL SEARCH
    // =========================================================

    public ExternalSymbolSearchResponse searchSymbols(
            String query
    ) {

        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/symbol_search")
                                .queryParam(
                                        "symbol",
                                        query
                                )
                                .queryParam(
                                        "apikey",
                                        apiKey
                                )
                                .build()
                )
                .retrieve()
                .body(
                        ExternalSymbolSearchResponse.class
                );
    }

    // =========================================================
    // EXCHANGE RATE
    // =========================================================

    public ExternalExchangeRateResponse getExchangeRate(
            String baseCurrency,
            String quoteCurrency
    ) {

        if (baseCurrency == null ||
                baseCurrency.isBlank()) {

            throw new IllegalArgumentException(
                    "Base currency is required"
            );
        }

        if (quoteCurrency == null ||
                quoteCurrency.isBlank()) {

            throw new IllegalArgumentException(
                    "Quote currency is required"
            );
        }

        String symbol =
                baseCurrency.trim().toUpperCase()
                        + "/"
                        + quoteCurrency.trim().toUpperCase();

        return restClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/exchange_rate")
                                .queryParam(
                                        "symbol",
                                        symbol
                                )
                                .queryParam(
                                        "apikey",
                                        apiKey
                                )
                                .build()
                )
                .retrieve()
                .body(
                        ExternalExchangeRateResponse.class
                );
    }
}