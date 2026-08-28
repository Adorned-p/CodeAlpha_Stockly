package com.codealpha.stockly.dto;

import com.codealpha.stockly.entity.StockStatus;

import java.math.BigDecimal;

public class StockResponse {

    private Long id;
    private String symbol;
    private String companyName;
    private BigDecimal currentPrice;

    private BigDecimal openingPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;

    private BigDecimal priceChange;
    private BigDecimal changePercentage;

    private String sector;
    private String exchange;
    private StockStatus status;

    public StockResponse() {
    }

    public StockResponse(
            Long id,
            String symbol,
            String companyName,
            BigDecimal currentPrice,
            BigDecimal openingPrice,
            BigDecimal previousClose,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            BigDecimal priceChange,
            BigDecimal changePercentage,
            String sector,
            String exchange,
            StockStatus status
    ) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.openingPrice = openingPrice;
        this.previousClose = previousClose;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.priceChange = priceChange;
        this.changePercentage = changePercentage;
        this.sector = sector;
        this.exchange = exchange;
        this.status = status;
    }

    public Long getId() {
        return id;
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

    public BigDecimal getOpeningPrice() {
        return openingPrice;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public BigDecimal getDayHigh() {
        return dayHigh;
    }

    public BigDecimal getDayLow() {
        return dayLow;
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

    public StockStatus getStatus() {
        return status;
    }
}