package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public class AdminStatsResponse {

    private long totalUsers;
    private long totalStocks;
    private long totalTrades;
    private BigDecimal totalTradingVolume;

    public AdminStatsResponse() {
    }

    public AdminStatsResponse(
            long totalUsers,
            long totalStocks,
            long totalTrades,
            BigDecimal totalTradingVolume
    ) {
        this.totalUsers = totalUsers;
        this.totalStocks = totalStocks;
        this.totalTrades = totalTrades;
        this.totalTradingVolume = totalTradingVolume;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalStocks() {
        return totalStocks;
    }

    public long getTotalTrades() {
        return totalTrades;
    }

    public BigDecimal getTotalTradingVolume() {
        return totalTradingVolume;
    }
}