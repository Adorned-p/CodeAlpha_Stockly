package com.codealpha.stockly.service;

import com.codealpha.stockly.service.CurrencyConversionService;
import com.codealpha.stockly.dto.DashboardResponse;
import com.codealpha.stockly.dto.StockResponse;
import com.codealpha.stockly.dto.TransactionResponse;
import com.codealpha.stockly.dto.WatchlistResponse;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.StockRepository;
import com.codealpha.stockly.repository.TransactionRepository;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;

    private final CurrencyConversionService currencyConversionService;
    private final AccountSummaryService accountSummaryService;
    private final PortfolioService portfolioService;
    private final WatchlistService watchlistService;

    public DashboardService(
            UserRepository userRepository,
            StockRepository stockRepository,
            TransactionRepository transactionRepository,
            AccountSummaryService accountSummaryService,
            PortfolioService portfolioService,
            WatchlistService watchlistService,
            CurrencyConversionService currencyConversionService
    ) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
        this.accountSummaryService = accountSummaryService;
        this.portfolioService = portfolioService;
        this.watchlistService = watchlistService;
        this.currencyConversionService = currencyConversionService;
    }

    public DashboardResponse getDashboard(
            String userEmail
    ) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        var accountSummary =
                accountSummaryService.getSummary(userEmail);

        var portfolio =
                portfolioService.getPortfolio(userEmail);

        List<WatchlistResponse> watchlist =
                watchlistService.getWatchlist(userEmail);

        List<TransactionResponse> recentTransactions =
                transactionRepository
                        .findByUserOrderByExecutedAtDesc(user)
                        .stream()
                        .limit(5)
                        .map(transaction ->
                                new TransactionResponse(
                                        transaction.getId(),
                                        transaction.getStock().getSymbol(),
                                        transaction.getStock().getExchange(),
                                        transaction.getStock().getCompanyName(),
                                        transaction.getType(),
                                        transaction.getQuantity(),
                                        transaction.getPrice(),
                                        transaction.getTotalAmount(),
                                        transaction.getExecutedAt()
                                )
                        )
                        .toList();

        List<StockResponse> stocks =
                stockRepository.findAll()
                        .stream()
                        .map(this::toStockResponse)
                        .toList();

        return new DashboardResponse(
                accountSummary,
                portfolio,
                watchlist,
                recentTransactions,
                stocks
        );
    }

    private StockResponse toStockResponse(
            Stock stock
    ) {

        BigDecimal priceChange =
                stock.getCurrentPrice()
                        .subtract(
                                stock.getPreviousClose()
                        );

        BigDecimal changePercentage =
                BigDecimal.ZERO;

        if (stock.getPreviousClose()
                .compareTo(BigDecimal.ZERO) > 0) {

            changePercentage =
                    priceChange
                            .multiply(
                                    BigDecimal.valueOf(100)
                            )
                            .divide(
                                    stock.getPreviousClose(),
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }

        String currency = getStockCurrency(stock);

        BigDecimal currentPriceInr;
        BigDecimal exchangeRateToInr;

        if ("INR".equals(currency)) {

            currentPriceInr =
                    stock.getCurrentPrice();

            exchangeRateToInr =
                    BigDecimal.ONE;

        } else {

            currentPriceInr =
                    currencyConversionService.convertToInr(
                            stock.getCurrentPrice(),
                            currency
                    );

            exchangeRateToInr =
                    currencyConversionService.getExchangeRate(
                            currency
                    );
        }

        return new StockResponse(
                stock.getId(),
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getCurrentPrice(),
                currentPriceInr,
                exchangeRateToInr,
                stock.getOpeningPrice(),
                stock.getPreviousClose(),
                stock.getDayHigh(),
                stock.getDayLow(),
                priceChange,
                changePercentage,
                stock.getSector(),
                stock.getExchange(),
                stock.getStatus()
        );
    }

    private String getStockCurrency(
            Stock stock
    ) {

        String exchange = stock.getExchange();

        if (exchange == null) {
            return "USD";
        }

        String normalizedExchange =
                exchange.trim().toUpperCase();

        if ("NSE".equals(normalizedExchange)
                || "BSE".equals(normalizedExchange)) {

            return "INR";
        }

        return "USD";
    }
}