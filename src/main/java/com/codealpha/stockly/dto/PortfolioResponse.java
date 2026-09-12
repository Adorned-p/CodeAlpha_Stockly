package com.codealpha.stockly.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioResponse {

    private BigDecimal virtualBalance;

    // Native portfolio totals
    private BigDecimal totalInvested;
    private BigDecimal currentPortfolioValue;
    private BigDecimal totalProfitLoss;

    // INR portfolio totals
    private BigDecimal virtualBalanceInr;
    private BigDecimal totalInvestedInr;
    private BigDecimal currentPortfolioValueInr;
    private BigDecimal totalProfitLossInr;

    private BigDecimal totalProfitLossPercentage;

    private List<HoldingResponse> holdings;

    public PortfolioResponse() {
    }

    public PortfolioResponse(
            BigDecimal virtualBalance,

            BigDecimal totalInvested,
            BigDecimal currentPortfolioValue,
            BigDecimal totalProfitLoss,

            BigDecimal virtualBalanceInr,
            BigDecimal totalInvestedInr,
            BigDecimal currentPortfolioValueInr,
            BigDecimal totalProfitLossInr,

            BigDecimal totalProfitLossPercentage,

            List<HoldingResponse> holdings
    ) {
        this.virtualBalance = virtualBalance;

        this.totalInvested = totalInvested;
        this.currentPortfolioValue = currentPortfolioValue;
        this.totalProfitLoss = totalProfitLoss;

        this.virtualBalanceInr = virtualBalanceInr;
        this.totalInvestedInr = totalInvestedInr;
        this.currentPortfolioValueInr = currentPortfolioValueInr;
        this.totalProfitLossInr = totalProfitLossInr;

        this.totalProfitLossPercentage = totalProfitLossPercentage;
        this.holdings = holdings;
    }

    public BigDecimal getVirtualBalance() {
        return virtualBalance;
    }

    public BigDecimal getTotalInvested() {
        return totalInvested;
    }

    public BigDecimal getCurrentPortfolioValue() {
        return currentPortfolioValue;
    }

    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public BigDecimal getVirtualBalanceInr() {
        return virtualBalanceInr;
    }

    public BigDecimal getTotalInvestedInr() {
        return totalInvestedInr;
    }

    public BigDecimal getCurrentPortfolioValueInr() {
        return currentPortfolioValueInr;
    }

    public BigDecimal getTotalProfitLossInr() {
        return totalProfitLossInr;
    }

    public BigDecimal getTotalProfitLossPercentage() {
        return totalProfitLossPercentage;
    }

    public List<HoldingResponse> getHoldings() {
        return holdings;
    }
}