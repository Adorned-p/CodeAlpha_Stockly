package com.codealpha.stockly.dto;

public class CreateInstrumentRequest {

    private String symbol;
    private String name;
    private String assetType;
    private String exchange;
    private String country;
    private String currency;

    /*
     * Provider that supplied the selected search result.
     *
     * Examples:
     * EODHD
     * TWELVE_DATA
     */
    private String provider;

    /*
     * Exact symbol returned by that provider.
     *
     * This should NOT be reconstructed from
     * Stockly's symbol and exchange.
     */
    private String providerSymbol;

    public CreateInstrumentRequest() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderSymbol() {
        return providerSymbol;
    }

    public void setProviderSymbol(String providerSymbol) {
        this.providerSymbol = providerSymbol;
    }
}