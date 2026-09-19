package com.codealpha.stockly.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketQuoteResponse {

    private String symbol;
    private String companyName;
    private String exchange;
    private String currency;

    /*
     * Native market price.
     *
     * Examples:
     * TCS  -> 2200.80 INR
     * AAPL -> 332.27 USD
     * SAP  -> 250.00 EUR
     */
    private BigDecimal currentPrice;

    /*
     * Same price converted to INR.
     *
     * This is the price STOCKLY uses as its
     * platform/base-currency value.
     */
    private BigDecimal currentPriceInr;

    /*
     * Native currency -> INR exchange rate.
     *
     * Examples:
     * INR -> 1
     * USD -> 95.xx
     * EUR -> 111.xx
     */
    private BigDecimal exchangeRateToInr;

    private BigDecimal openingPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;

    private BigDecimal change;
    private BigDecimal percentChange;

    private Long volume;

    /*
     * Main market-data constructor.
     *
     * Existing callers can continue using the
     * old constructor below. This constructor is
     * available for the new INR-aware flow.
     */
    public MarketQuoteResponse(
            String symbol,
            String companyName,
            String exchange,
            String currency,
            BigDecimal currentPrice,
            BigDecimal currentPriceInr,
            BigDecimal exchangeRateToInr,
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
        this.currentPriceInr = currentPriceInr;
        this.exchangeRateToInr = exchangeRateToInr;

        this.openingPrice = openingPrice;
        this.previousClose = previousClose;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;

        this.change = change;
        this.percentChange = percentChange;

        this.volume = volume;
    }

    /*
     * Backward-compatible constructor.
     *
     * This prevents existing code from breaking
     * while we update MarketQuoteService.
     */
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

        /*
         * INR value will be populated later by
         * MarketQuoteService.
         */
        this.currentPriceInr = null;
        this.exchangeRateToInr = null;

        this.openingPrice = openingPrice;
        this.previousClose = previousClose;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;

        this.change = change;
        this.percentChange = percentChange;

        this.volume = volume;
    }

    /*
     * Compatibility constructor for STOCKLY's
     * internal/manual market quote updates.
     */
    public MarketQuoteResponse(
            String symbol,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            BigDecimal lastPrice,
            LocalDateTime updatedAt
    ) {

        this.symbol = symbol;
        this.currentPrice = lastPrice;

        /*
         * Native currency is unknown for this
         * compatibility path.
         */
        this.currency = null;

        this.currentPriceInr = null;
        this.exchangeRateToInr = null;
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

    public BigDecimal getCurrentPriceInr() {
        return currentPriceInr;
    }

    public BigDecimal getExchangeRateToInr() {
        return exchangeRateToInr;
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

    /*
     * Setters are included for the INR-aware
     * MarketQuoteService.
     */

    public void setCurrentPriceInr(
            BigDecimal currentPriceInr
    ) {

        this.currentPriceInr =
                currentPriceInr;
    }

    public void setExchangeRateToInr(
            BigDecimal exchangeRateToInr
    ) {

        this.exchangeRateToInr =
                exchangeRateToInr;
    }
}