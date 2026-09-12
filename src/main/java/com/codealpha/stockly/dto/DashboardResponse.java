package com.codealpha.stockly.dto;

import java.util.List;

public class DashboardResponse {

    private AccountSummaryResponse accountSummary;
    private PortfolioResponse portfolio;
    private List<WatchlistResponse> watchlist;
    private List<TransactionResponse> recentTransactions;
    private List<StockResponse> stocks;

    public DashboardResponse() {
    }

    public DashboardResponse(
            AccountSummaryResponse accountSummary,
            PortfolioResponse portfolio,
            List<WatchlistResponse> watchlist,
            List<TransactionResponse> recentTransactions,
            List<StockResponse> stocks
    ) {
        this.accountSummary = accountSummary;
        this.portfolio = portfolio;
        this.watchlist = watchlist;
        this.recentTransactions = recentTransactions;
        this.stocks = stocks;
    }

    public AccountSummaryResponse getAccountSummary() {
        return accountSummary;
    }

    public PortfolioResponse getPortfolio() {
        return portfolio;
    }

    public List<WatchlistResponse> getWatchlist() {
        return watchlist;
    }

    public List<TransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public List<StockResponse> getStocks() {
        return stocks;
    }
}