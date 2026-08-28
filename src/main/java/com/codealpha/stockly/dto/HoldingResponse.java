package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public class HoldingResponse {

    private String symbol;
    private String companyName;
    private Integer quantity;
    private BigDecimal averageBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal investedAmount;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
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
            BigDecimal profitLossPercentage
    ) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
        this.currentValue = currentValue;
        this.currentPrice = currentPrice;
        this.investedAmount = investedAmount;
        this.profitLoss = profitLoss;
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

    public BigDecimal getProfitLossPercentage() {
        return profitLossPercentage;
    }
}