package com.codealpha.stockly.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioResponse {

    private BigDecimal virtualBalance;
    private BigDecimal totalInvested;
    private BigDecimal currentPortfolioValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercentage;
    private List<HoldingResponse> holdings;

    public PortfolioResponse() {
    }

    public PortfolioResponse(
            BigDecimal virtualBalance,
            BigDecimal totalInvested,
            BigDecimal currentPortfolioValue,
            BigDecimal totalProfitLoss,
            BigDecimal totalProfitLossPercentage,
            List<HoldingResponse> holdings
    ) {
        this.virtualBalance = virtualBalance;
        this.totalInvested = totalInvested;
        this.currentPortfolioValue = currentPortfolioValue;
        this.totalProfitLoss = totalProfitLoss;
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

    public BigDecimal getTotalProfitLossPercentage() {
        return totalProfitLossPercentage;
    }

    public List<HoldingResponse> getHoldings() {
        return holdings;
    }
}