package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public class HoldingResponse {

    private String symbol;
    private String companyName;
    private Integer quantity;

    // Native currency values
    private BigDecimal averageBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal investedAmount;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;

    // INR display values
    private BigDecimal averageBuyPriceInr;
    private BigDecimal currentPriceInr;
    private BigDecimal investedAmountInr;
    private BigDecimal currentValueInr;
    private BigDecimal profitLossInr;

    private BigDecimal exchangeRateToInr;

    private BigDecimal profitLossPercentage;

    public HoldingResponse() {
    }

    public HoldingResponse(
            String symbol,
            String companyName,
            Integer quantity,

            BigDecimal averageBuyPrice,
            BigDecimal currentPrice,
            BigDecimal investedAmount,
            BigDecimal currentValue,
            BigDecimal profitLoss,

            BigDecimal averageBuyPriceInr,
            BigDecimal currentPriceInr,
            BigDecimal investedAmountInr,
            BigDecimal currentValueInr,
            BigDecimal profitLossInr,

            BigDecimal exchangeRateToInr,
            BigDecimal profitLossPercentage
    ) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.quantity = quantity;

        this.averageBuyPrice = averageBuyPrice;
        this.currentPrice = currentPrice;
        this.investedAmount = investedAmount;
        this.currentValue = currentValue;
        this.profitLoss = profitLoss;

        this.averageBuyPriceInr = averageBuyPriceInr;
        this.currentPriceInr = currentPriceInr;
        this.investedAmountInr = investedAmountInr;
        this.currentValueInr = currentValueInr;
        this.profitLossInr = profitLossInr;

        this.exchangeRateToInr = exchangeRateToInr;
        this.profitLossPercentage = profitLossPercentage;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getAverageBuyPrice() {
        return averageBuyPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getInvestedAmount() {
        return investedAmount;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    public BigDecimal getAverageBuyPriceInr() {
        return averageBuyPriceInr;
    }

    public BigDecimal getCurrentPriceInr() {
        return currentPriceInr;
    }

    public BigDecimal getInvestedAmountInr() {
        return investedAmountInr;
    }

    public BigDecimal getCurrentValueInr() {
        return currentValueInr;
    }

    public BigDecimal getProfitLossInr() {
        return profitLossInr;
    }

    public BigDecimal getExchangeRateToInr() {
        return exchangeRateToInr;
    }

    public BigDecimal getProfitLossPercentage() {
        return profitLossPercentage;
    }
}