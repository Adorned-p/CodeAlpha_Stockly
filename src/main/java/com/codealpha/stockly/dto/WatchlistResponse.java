package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public class WatchlistResponse {

    private String symbol;
    private String companyName;
    private BigDecimal currentPrice;
    private BigDecimal priceChange;
    private BigDecimal changePercentage;
    private String sector;
    private String exchange;

    public WatchlistResponse() {
    }

    public WatchlistResponse(
            String symbol,
            String companyName,
            BigDecimal currentPrice,
            BigDecimal priceChange,
            BigDecimal changePercentage,
            String sector,
            String exchange
    ) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.changePercentage = changePercentage;
        this.sector = sector;
        this.exchange = exchange;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getPriceChange() {
        return priceChange;
    }

    public BigDecimal getChangePercentage() {
        return changePercentage;
    }

    public String getSector() {
        return sector;
    }

    public String getExchange() {
        return exchange;
    }
}