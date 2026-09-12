package com.codealpha.stockly.dto;


import java.math.BigDecimal;

public class MarketQuoteResponse {

    private String symbol;
    private String companyName;
    private String exchange;
    private String currency;

    private BigDecimal currentPrice;
    private BigDecimal openingPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;

    private BigDecimal change;
    private BigDecimal percentChange;

    private Long volume;

    public MarketQuoteResponse(
            String symbol,
            String companyName,
            String exchange,
            String currency,
            BigDecimal currentPrice,
            BigDecimal openingPrice,
            BigDecimal previousClose,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            BigDecimal change,
            BigDecimal percentChange,
            Long volume
    ) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.exchange = exchange;
        this.currency = currency;
        this.currentPrice = currentPrice;
        this.openingPrice = openingPrice;
        this.previousClose = previousClose;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.change = change;
        this.percentChange = percentChange;
        this.volume = volume;
    }


    /*
     * Compatibility constructor for Stockly's
     * internal/manual market quote updates.
     */
    public MarketQuoteResponse(
            String symbol,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            BigDecimal lastPrice,
            java.time.LocalDateTime updatedAt
    ) {
        this.symbol = symbol;
        this.currentPrice = lastPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getExchange() {
        return exchange;
    }

    public String getCurrency() {
        return currency;
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

    public BigDecimal getChange() {
        return change;
    }

    public BigDecimal getPercentChange() {
        return percentChange;
    }

    public Long getVolume() {
        return volume;
    }
}