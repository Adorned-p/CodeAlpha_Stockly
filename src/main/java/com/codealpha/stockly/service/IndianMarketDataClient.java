package com.codealpha.stockly.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Service
public class IndianMarketDataClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IndianMarketDataClient(
            @Value("${indianapi.base-url}") String baseUrl,
            @Value("${indianapi.api-key}") String apiKey
    ) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("Accept", "application/json")
                .build();

        this.objectMapper = new ObjectMapper();
    }

    // =========================================================
    // GET QUOTE
    // =========================================================

    public IndianQuote getQuote(
            String symbol,
            String exchange
    ) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Symbol is required"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedExchange =
                exchange == null || exchange.isBlank()
                        ? "NSE"
                        : exchange.trim().toUpperCase();

        if (!normalizedExchange.equals("NSE")
                && !normalizedExchange.equals("BSE")) {

            throw new IllegalArgumentException(
                    "Indian Market API supports NSE and BSE"
            );
        }

        System.out.println(
                "[INDIAN API] Requesting quote: "
                        + normalizedSymbol
                        + " ("
                        + normalizedExchange
                        + ")"
        );

        try {

            String responseBody =
                    restClient.get()
                            .uri(uriBuilder ->
                                    uriBuilder
                                            .path("/stock")
                                            .queryParam(
                                                    "name",
                                                    normalizedSymbol
                                            )
                                            .build()
                            )
                            .retrieve()
                            .body(String.class);

            if (responseBody == null ||
                    responseBody.isBlank()) {

                throw new RuntimeException(
                        "IndianAPI returned an empty response"
                );
            }

            JsonNode root =
                    objectMapper.readTree(responseBody);

            return parseQuote(
                    root,
                    normalizedSymbol,
                    normalizedExchange
            );

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to get IndianAPI quote for "
                            + normalizedSymbol
                            + " ("
                            + normalizedExchange
                            + ")",
                    exception
            );
        }
    }

    // =========================================================
    // PARSE QUOTE
    // =========================================================

    private IndianQuote parseQuote(
            JsonNode root,
            String requestedSymbol,
            String exchange
    ) {

        // -----------------------------------------------------
        // COMPANY NAME
        // -----------------------------------------------------

        String companyName =
                getText(
                        root,
                        "companyName"
                );

        if (companyName == null ||
                companyName.isBlank()) {

            companyName = requestedSymbol;
        }

        // -----------------------------------------------------
        // CURRENT PRICE
        //
        // IndianAPI response:
        //
        // "currentPrice": {
        //     "BSE": "2202.00",
        //     "NSE": "2200.80"
        // }
        // -----------------------------------------------------

        JsonNode currentPriceNode =
                root.path("currentPrice");

        BigDecimal currentPrice =
                getDecimal(
                        currentPriceNode,
                        exchange
                );

        if (currentPrice == null ||
                currentPrice.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            throw new RuntimeException(
                    "No valid current price returned by "
                            + "IndianAPI for "
                            + requestedSymbol
                            + " ("
                            + exchange
                            + ")"
            );
        }

        // -----------------------------------------------------
        // STOCK DETAILS
        // -----------------------------------------------------

        JsonNode details =
                root.path(
                        "stockDetailsReusableData"
                );

        // The API's "close" represents the previous
        // closing price used for calculating change.

        BigDecimal previousClose =
                getDecimal(
                        details,
                        "close"
                );

        BigDecimal high =
                getDecimal(
                        details,
                        "high"
                );

        BigDecimal low =
                getDecimal(
                        details,
                        "low"
                );

        // -----------------------------------------------------
        // PERCENT CHANGE
        // -----------------------------------------------------

        BigDecimal percentChange =
                getDecimal(
                        root,
                        "percentChange"
                );

        if (percentChange == null) {

            percentChange =
                    getDecimal(
                            details,
                            "percentChange"
                    );
        }

        // -----------------------------------------------------
        // CHANGE
        // -----------------------------------------------------

        BigDecimal change = null;

        if (previousClose != null) {

            change =
                    currentPrice.subtract(
                            previousClose
                    );
        }

        // -----------------------------------------------------
        // OPEN
        // -----------------------------------------------------
        //
        // We do NOT guess the opening price.
        // If IndianAPI provides an explicit "open"
        // field later, we can add it safely.
        //

        BigDecimal open =
                getDecimal(
                        details,
                        "open"
                );

        // -----------------------------------------------------
        // VOLUME
        // -----------------------------------------------------

        Long volume =
                getLong(
                        details,
                        "volume"
                );

        if (volume == null) {

            volume =
                    getLong(
                            root,
                            "volume"
                    );
        }

        // -----------------------------------------------------
        // DATE + TIME
        // -----------------------------------------------------

        String date =
                getText(
                        details,
                        "date"
                );

        String time =
                getText(
                        details,
                        "time"
                );

        String datetime = null;

        if (date != null && time != null) {

            datetime =
                    date + " " + time;

        } else if (date != null) {

            datetime = date;

        } else if (time != null) {

            datetime = time;
        }

        // -----------------------------------------------------
        // SYMBOL
        // -----------------------------------------------------

        String responseSymbol =
                getText(
                        root,
                        "tickerId"
                );

        if (responseSymbol == null ||
                responseSymbol.isBlank()) {

            responseSymbol =
                    getText(
                            root,
                            "symbol"
                    );
        }

        if (responseSymbol == null ||
                responseSymbol.isBlank()) {

            responseSymbol = requestedSymbol;
        }

        // -----------------------------------------------------
        // LOG
        // -----------------------------------------------------

        System.out.println(
                "[INDIAN API] Quote received: "
                        + responseSymbol
                        + " | "
                        + exchange
                        + " | ₹"
                        + currentPrice
                        + " | Change: "
                        + percentChange
                        + "%"
        );

        // -----------------------------------------------------
        // RETURN
        // -----------------------------------------------------

        return new IndianQuote(
                responseSymbol,
                companyName,
                exchange,
                "INR",
                datetime,
                open,
                high,
                low,
                currentPrice,
                previousClose,
                change,
                percentChange,
                volume
        );
    }

    // =========================================================
    // DECIMAL HELPER
    // =========================================================

    private BigDecimal getDecimal(
            JsonNode node,
            String field
    ) {

        if (node == null ||
                node.isMissingNode() ||
                node.isNull()) {

            return null;
        }

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return null;
        }

        String text =
                value.asText();

        if (text == null ||
                text.isBlank() ||
                text.equalsIgnoreCase("null")) {

            return null;
        }

        try {

            return new BigDecimal(
                    text.trim()
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    // =========================================================
    // LONG HELPER
    // =========================================================

    private Long getLong(
            JsonNode node,
            String field
    ) {

        if (node == null ||
                node.isMissingNode() ||
                node.isNull()) {

            return null;
        }

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return null;
        }

        try {

            return Long.parseLong(
                    value.asText().trim()
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    // =========================================================
    // TEXT HELPER
    // =========================================================

    private String getText(
            JsonNode node,
            String field
    ) {

        if (node == null ||
                node.isMissingNode() ||
                node.isNull()) {

            return null;
        }

        JsonNode value =
                node.get(field);

        if (value == null ||
                value.isNull()) {

            return null;
        }

        String text =
                value.asText();

        if (text == null ||
                text.isBlank()) {

            return null;
        }

        return text.trim();
    }

    // =========================================================
    // INTERNAL QUOTE MODEL
    // =========================================================

    public record IndianQuote(

            String symbol,

            String name,

            String exchange,

            String currency,

            String datetime,

            BigDecimal open,

            BigDecimal high,

            BigDecimal low,

            BigDecimal close,

            BigDecimal previousClose,

            BigDecimal change,

            BigDecimal percentChange,

            Long volume

    ) {
    }
}