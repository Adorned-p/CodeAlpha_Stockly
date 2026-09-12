package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.AccountSummaryResponse;
import com.codealpha.stockly.dto.PortfolioResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountSummaryService {

    private final UserRepository userRepository;
    private final PortfolioService portfolioService;

    public AccountSummaryService(
            UserRepository userRepository,
            PortfolioService portfolioService
    ) {
        this.userRepository = userRepository;
        this.portfolioService = portfolioService;
    }

    public AccountSummaryResponse getSummary(
            String userEmail
    ) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        PortfolioResponse portfolio =
                portfolioService.getPortfolio(userEmail);

        BigDecimal cashBalance =
                user.getVirtualBalance();

        BigDecimal portfolioValue =
                portfolio.getCurrentPortfolioValue();

        BigDecimal totalInvested =
                portfolio.getTotalInvested();

        BigDecimal totalProfitLoss =
                portfolio.getTotalProfitLoss();

        BigDecimal totalProfitLossPercentage =
                portfolio.getTotalProfitLossPercentage();

        BigDecimal totalAccountValue =
                cashBalance.add(portfolioValue);

        return new AccountSummaryResponse(
                cashBalance,
                totalInvested,
                portfolioValue,
                totalAccountValue,
                totalProfitLoss,
                totalProfitLossPercentage
        );
    }
}