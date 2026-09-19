package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalQuoteResponse;
import com.codealpha.stockly.dto.InstrumentSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class EodhdMarketDataClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public EodhdMarketDataClient(
            @Value("${eodhd.base-url}") String baseUrl,
            @Value("${eodhd.api-key}") String apiKey
    ) {

        this.restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .build();

        this.objectMapper =
                new ObjectMapper();

        this.apiKey = apiKey;
    }

    // =========================================================
    // GET QUOTE
    // =========================================================

    /*
     * IMPORTANT:
     *
     * This method is kept for compatibility with existing code.
     *
     * For provider-specific instruments, prefer:
     *
     *     getQuoteByProviderSymbol(...)
     *
     * because the provider symbol should come from EODHD search.
     */
    public ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    ) {

        if (symbol == null || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol is required"
            );
        }

        if (exchange == null || exchange.isBlank()) {

            throw new IllegalArgumentException(
                    "Exchange is required"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedExchange =
                exchange.trim().toUpperCase();

        String eodhdExchange =
                mapExchangeToEodhd(
                        normalizedExchange
                );

        String ticker =
                normalizedSymbol
                        + "."
                        + eodhdExchange;

        return getQuoteByProviderSymbol(
                ticker
        );
    }

    // =========================================================
    // GET QUOTE USING PROVIDER-SPECIFIC SYMBOL
    // =========================================================

    public ExternalQuoteResponse getQuoteByProviderSymbol(
            String providerSymbol
    ) {

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            throw new IllegalArgumentException(
                    "EODHD provider symbol is required"
            );
        }

        String ticker =
                providerSymbol.trim().toUpperCase();

        System.out.println(
                "[EODHD] Requesting quote: "
                        + ticker
        );

        // =====================================================
        // 1. TRY REAL-TIME QUOTE
        // =====================================================

        try {

            String responseBody =
                    restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/real-time/{ticker}")
                                    .queryParam(
                                            "api_token",
                                            apiKey
                                    )
                                    .queryParam(
                                            "fmt",
                                            "json"
                                    )
                                    .build(ticker)
                            )
                            .retrieve()
                            .body(String.class);

            System.out.println(
                    "[EODHD] Real-time response for "
                            + ticker
                            + ": "
                            + responseBody
            );

            if (responseBody != null &&
                    !responseBody.isBlank()) {

                JsonNode root =
                        objectMapper.readTree(
                                responseBody
                        );

                BigDecimal close =
                        getDecimal(
                                root,
                                "close"
                        );

                if (close != null &&
                        close.compareTo(
                                BigDecimal.ZERO
                        ) > 0) {

                    ExternalQuoteResponse response =
                            new ExternalQuoteResponse();

                    response.setSymbol(
                            getText(root, "code")
                    );

                    response.setExchange(
                            getText(root, "exchange")
                    );

                    response.setCurrency(
                            getText(root, "currency")
                    );

                    response.setDatetime(
                            getText(root, "timestamp")
                    );

                    response.setClose(close);

                    response.setOpen(
                            getDecimal(
                                    root,
                                    "open"
                            )
                    );

                    response.setHigh(
                            getDecimal(
                                    root,
                                    "high"
                            )
                    );

                    response.setLow(
                            getDecimal(
                                    root,
                                    "low"
                            )
                    );

                    response.setPreviousClose(
                            getDecimal(
                                    root,
                                    "previousClose"
                            )
                    );

                    return response;
                }

                System.out.println(
                        "[EODHD] Real-time price unavailable "
                                + "for "
                                + ticker
                                + ". Falling back to EOD."
                );
            }

        } catch (Exception exception) {

            System.err.println(
                    "[EODHD] Real-time request failed for "
                            + ticker
                            + ": "
                            + exception.getMessage()
            );

            System.out.println(
                    "[EODHD] Trying EOD fallback..."
            );
        }

        // =====================================================
        // 2. FALLBACK TO LATEST EOD PRICE
        // =====================================================

        return getLatestEodQuote(
                ticker
        );
    }

    // =========================================================
    // LATEST EOD QUOTE
    // =========================================================

    private ExternalQuoteResponse getLatestEodQuote(
            String ticker
    ) {

        try {

            System.out.println(
                    "[EODHD] Requesting latest EOD close: "
                            + ticker
            );

            String responseBody =
                    restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/eod/{ticker}")
                                    .queryParam(
                                            "api_token",
                                            apiKey
                                    )
                                    .queryParam(
                                            "filter",
                                            "last_close"
                                    )
                                    .queryParam(
                                            "fmt",
                                            "json"
                                    )
                                    .build(ticker)
                            )
                            .retrieve()
                            .body(String.class);

            System.out.println(
                    "[EODHD] EOD response for "
                            + ticker
                            + ": "
                            + responseBody
            );

            if (responseBody == null ||
                    responseBody.isBlank()) {

                throw new RuntimeException(
                        "EODHD returned an empty EOD response"
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            responseBody
                    );

            BigDecimal lastClose = null;

            /*
             * EODHD filter=last_close may return
             * a bare number/string.
             */

            if (root.isNumber()) {

                lastClose =
                        root.decimalValue();

            } else if (root.isTextual()) {

                lastClose =
                        new BigDecimal(
                                root.asText()
                        );
            }

            if (lastClose == null ||
                    lastClose.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                throw new RuntimeException(
                        "EODHD returned an invalid "
                                + "last close for "
                                + ticker
                );
            }

            System.out.println(
                    "[EODHD] Latest EOD close found: "
                            + lastClose
            );

            ExternalQuoteResponse response =
                    new ExternalQuoteResponse();

            response.setClose(lastClose);

            return response;

        } catch (Exception exception) {

            throw new RuntimeException(
                    "EODHD could not obtain "
                            + "real-time or EOD price for "
                            + ticker,
                    exception
            );
        }
    }

    // =========================================================
    // SEARCH SYMBOLS
    // =========================================================

    public List<InstrumentSearchResponse> searchSymbols(
            String query
    ) {

        if (query == null ||
                query.isBlank()) {

            return List.of();
        }

        try {

            String responseBody =
                    restClient.get()
                            .uri(uriBuilder ->
                                    uriBuilder
                                            .path("/search/{query}")
                                            .queryParam(
                                                    "api_token",
                                                    apiKey
                                            )
                                            .queryParam(
                                                    "limit",
                                                    20
                                            )
                                            .queryParam(
                                                    "type",
                                                    "stock"
                                            )
                                            .queryParam(
                                                    "fmt",
                                                    "json"
                                            )
                                            .build(
                                                    query.trim()
                                            )
                            )
                            .retrieve()
                            .body(String.class);

            if (responseBody == null ||
                    responseBody.isBlank()) {

                return List.of();
            }

            JsonNode root =
                    objectMapper.readTree(
                            responseBody
                    );

            if (!root.isArray()) {

                return List.of();
            }

            List<InstrumentSearchResponse> results =
                    new ArrayList<>();

            for (JsonNode item : root) {

                String symbol =
                        getText(
                                item,
                                "Code"
                        );

                String name =
                        getText(
                                item,
                                "Name"
                        );

                String exchange =
                        getText(
                                item,
                                "Exchange"
                        );

                String country =
                        getText(
                                item,
                                "Country"
                        );

                String currency =
                        getText(
                                item,
                                "Currency"
                        );

                String assetType =
                        getText(
                                item,
                                "Type"
                        );

                /*
                 * EODHD search returns the code and exchange
                 * separately.
                 *
                 * EODHD's documented ticker format is:
                 *
                 *     CODE.EXCHANGE
                 *
                 * We construct it ONLY at the provider boundary.
                 *
                 * The rest of Stockly receives the resulting
                 * providerSymbol and stores it unchanged.
                 */

                if (symbol == null ||
                        symbol.isBlank() ||
                        exchange == null ||
                        exchange.isBlank()) {

                    continue;
                }

                String providerExchange =
                        mapExchangeToEodhd(
                                exchange.trim().toUpperCase()
                        );

                String providerSymbol =
                        symbol.trim().toUpperCase()
                                + "."
                                + providerExchange;

                results.add(
                        new InstrumentSearchResponse(
                                symbol.trim(),
                                name,
                                exchange.trim(),
                                country,
                                currency,
                                assetType,
                                "EODHD",
                                providerSymbol
                        )
                );
            }

            return results;

        } catch (Exception exception) {

            System.err.println(
                    "EODHD symbol search failed for '"
                            + query
                            + "': "
                            + exception.getMessage()
            );

            return List.of();
        }
    }

    // =========================================================
    // JSON TEXT HELPER
    // =========================================================

    private String getText(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return null;
        }

        return value.asText();
    }

    // =========================================================
    // JSON DECIMAL HELPER
    // =========================================================

    private BigDecimal getDecimal(
            JsonNode node,
            String field
    ) {

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return null;
        }

        try {

            return value.decimalValue();

        } catch (Exception exception) {

            return null;
        }
    }

    // =========================================================
    // EODHD EXCHANGE MAPPING
    // =========================================================

    private String mapExchangeToEodhd(
            String exchange
    ) {

        return switch (exchange) {

            // Japan
            case "JPX", "XJPX" -> "T";

            // United States
            case "NASDAQ", "XNAS" -> "US";
            case "NYSE", "XNYS" -> "US";
            case "AMEX", "XASE" -> "US";
            case "OTC", "OTCM" -> "US";

            // United Kingdom
            case "LSE", "XLON" -> "LSE";

            // Germany
            case "XETRA" -> "XETRA";

            // India
            case "NSE", "XNSE" -> "NSE";
            case "BSE", "XBOM" -> "BSE";

            default -> exchange;
        };
    }
}