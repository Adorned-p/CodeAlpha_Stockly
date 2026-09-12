package com.codealpha.stockly.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AlphaVantageSymbolResolver {

    private final AlphaVantageMarketDataClient alphaVantageMarketDataClient;
    private final ObjectMapper objectMapper;

    public AlphaVantageSymbolResolver(
            AlphaVantageMarketDataClient alphaVantageMarketDataClient
    ) {
        this.alphaVantageMarketDataClient =
                alphaVantageMarketDataClient;

        this.objectMapper =
                new ObjectMapper();
    }

    public String resolve(
            String symbol,
            String exchange,
            String country
    ) {

        if (symbol == null || symbol.isBlank()) {
            return null;
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedExchange =
                normalize(exchange);

        String normalizedCountry =
                normalize(country);

        /*
         * If the symbol already contains a provider-specific
         * suffix, keep it.
         *
         * Example:
         * TCS.BSE
         */
        if (normalizedSymbol.contains(".")) {
            return normalizedSymbol;
        }

        /*
         * Alpha Vantage normally uses the normal ticker
         * for major US exchanges.
         */
        if (normalizedExchange.equals("NASDAQ") ||
                normalizedExchange.equals("NYSE")) {

            return normalizedSymbol;
        }

        /*
         * Alpha Vantage uses .BSE for Bombay Stock Exchange.
         */
        if (normalizedExchange.equals("BSE")) {

            return normalizedSymbol + ".BSE";
        }

        /*
         * Do NOT assume NSE -> .NSE.
         *
         * We already confirmed that this is not a safe
         * assumption for Alpha Vantage.
         */
        if (normalizedExchange.equals("NSE")) {

            System.out.println(
                    "Alpha Vantage mapping not assumed for NSE: "
                            + normalizedSymbol
            );

            return null;
        }

        /*
         * For other exchanges, search Alpha Vantage.
         */
        try {

            String rawResponse =
                    alphaVantageMarketDataClient
                            .searchSymbols(normalizedSymbol);

            if (rawResponse == null ||
                    rawResponse.isBlank()) {

                return null;
            }

            JsonNode root =
                    objectMapper.readTree(rawResponse);

            JsonNode matches =
                    root.get("bestMatches");

            if (matches == null ||
                    !matches.isArray() ||
                    matches.isEmpty()) {

                return null;
            }

            /*
             * Look for an exact ticker and matching country.
             */
            for (JsonNode match : matches) {

                String providerSymbol =
                        text(
                                match,
                                "1. symbol"
                        );

                String region =
                        text(
                                match,
                                "4. region"
                        );

                if (isSameTicker(
                        providerSymbol,
                        normalizedSymbol
                ) &&
                        matchesCountry(
                                region,
                                normalizedCountry
                        )) {

                    System.out.println(
                            "Alpha Vantage symbol resolved: "
                                    + normalizedSymbol
                                    + " -> "
                                    + providerSymbol
                    );

                    return providerSymbol;
                }
            }

        } catch (Exception exception) {

            System.err.println(
                    "Alpha Vantage symbol resolution failed for "
                            + normalizedSymbol
                            + ": "
                            + exception.getMessage()
            );
        }

        return null;
    }

    private boolean isSameTicker(
            String providerSymbol,
            String stocklySymbol
    ) {

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            return false;
        }

        String baseSymbol =
                providerSymbol;

        int dotIndex =
                baseSymbol.indexOf('.');

        if (dotIndex > 0) {

            baseSymbol =
                    baseSymbol.substring(
                            0,
                            dotIndex
                    );
        }

        return baseSymbol.equalsIgnoreCase(
                stocklySymbol
        );
    }

    private boolean matchesCountry(
            String region,
            String country
    ) {

        if (region == null ||
                region.isBlank() ||
                country == null ||
                country.isBlank()) {

            return false;
        }

        return region.equalsIgnoreCase(country);
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toUpperCase();
    }

    private String text(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return "";
        }

        return value.asText().trim();
    }
}