package com.codealpha.stockly.dto;

public class InstrumentSearchResponse {

    private String symbol;
    private String name;
    private String exchange;
    private String country;
    private String currency;
    private String assetType;

    public InstrumentSearchResponse(
            String symbol,
            String name,
            String exchange,
            String country,
            String currency,
            String assetType
    ) {
        this.symbol = symbol;
        this.name = name;
        this.exchange = exchange;
        this.country = country;
        this.currency = currency;
        this.assetType = assetType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
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

    public String getAssetType() {
        return assetType;
    }
}