package com.codealpha.stockly.dto;

public class InstrumentSearchResponse {

    private String symbol;
    private String name;
    private String exchange;
    private String country;
    private String currency;
    private String assetType;

    /*
     * Market-data provider that returned this result.
     *
     * Examples:
     * EODHD
     * TWELVE_DATA
     */
    private String provider;

    /*
     * Exact symbol used by that provider.
     *
     * This is important because provider symbol formats
     * can differ from Stockly's own symbol.
     */
    private String providerSymbol;

    public InstrumentSearchResponse(
            String symbol,
            String name,
            String exchange,
            String country,
            String currency,
            String assetType
    ) {

        this(
                symbol,
                name,
                exchange,
                country,
                currency,
                assetType,
                null,
                symbol
        );
    }

    public InstrumentSearchResponse(
            String symbol,
            String name,
            String exchange,
            String country,
            String currency,
            String assetType,
            String provider,
            String providerSymbol
    ) {

        this.symbol = symbol;
        this.name = name;
        this.exchange = exchange;
        this.country = country;
        this.currency = currency;
        this.assetType = assetType;
        this.provider = provider;
        this.providerSymbol = providerSymbol;
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

    public String getProvider() {
        return provider;
    }

    public String getProviderSymbol() {
        return providerSymbol;
    }
}