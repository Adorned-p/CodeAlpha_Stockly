package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public class InstrumentResponse {

    private Long id;

    private String symbol;

    private String name;

    private String assetType;

    private String exchange;

    private String country;

    private String currency;

    /*
     * Native market price.
     *
     * Example:
     * MSFT -> 497.755 USD
     */
    private BigDecimal currentPrice;

    /*
     * Converted display price.
     *
     * Example:
     * MSFT -> approximately ₹41,xxx
     */
    private BigDecimal currentPriceInr;

    /*
     * Current FX rate.
     *
     * Example:
     * 1 USD -> 83.xx INR
     */
    private BigDecimal exchangeRateToInr;

    private boolean active;

    public InstrumentResponse() {
    }

    public InstrumentResponse(
            Long id,
            String symbol,
            String name,
            String assetType,
            String exchange,
            String country,
            String currency,
            BigDecimal currentPrice,
            BigDecimal currentPriceInr,
            BigDecimal exchangeRateToInr,
            boolean active
    ) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.exchange = exchange;
        this.country = country;
        this.currency = currency;
        this.currentPrice = currentPrice;
        this.currentPriceInr = currentPriceInr;
        this.exchangeRateToInr = exchangeRateToInr;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getExchange() {
        return exchange;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getCurrentPriceInr() {
        return currentPriceInr;
    }

    public BigDecimal getExchangeRateToInr() {
        return exchangeRateToInr;
    }

    public boolean isActive() {
        return active;
    }
}