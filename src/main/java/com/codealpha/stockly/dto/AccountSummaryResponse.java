package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public class AccountSummaryResponse {

    private BigDecimal cashBalance;
    private BigDecimal totalInvested;
    private BigDecimal portfolioValue;
    private BigDecimal totalAccountValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercentage;

    public AccountSummaryResponse() {
    }

    public AccountSummaryResponse(
            BigDecimal cashBalance,
            BigDecimal totalInvested,
            BigDecimal portfolioValue,
            BigDecimal totalAccountValue,
            BigDecimal totalProfitLoss,
            BigDecimal totalProfitLossPercentage
    ) {
        this.cashBalance = cashBalance;
        this.totalInvested = totalInvested;
        this.portfolioValue = portfolioValue;
        this.totalAccountValue = totalAccountValue;
        this.totalProfitLoss = totalProfitLoss;
        this.totalProfitLossPercentage =
                totalProfitLossPercentage;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public BigDecimal getTotalInvested() {
        return totalInvested;
    }

    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public BigDecimal getTotalAccountValue() {
        return totalAccountValue;
    }

    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public BigDecimal getTotalProfitLossPercentage() {
        return totalProfitLossPercentage;
    }
}