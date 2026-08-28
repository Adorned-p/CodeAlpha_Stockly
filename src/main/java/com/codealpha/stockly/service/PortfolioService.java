package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.HoldingResponse;
import com.codealpha.stockly.dto.PortfolioResponse;
import com.codealpha.stockly.entity.Holding;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.HoldingRepository;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PortfolioService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;

    public PortfolioService(
            UserRepository userRepository,
            HoldingRepository holdingRepository
    ) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
    }

    public PortfolioResponse getPortfolio(
            String userEmail
    ) {

        // 1. Find logged-in user
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        // 2. Find all holdings
        List<Holding> holdings =
                holdingRepository.findByUser(user);

        BigDecimal totalInvested =
                BigDecimal.ZERO;

        BigDecimal currentPortfolioValue =
                BigDecimal.ZERO;

        // 3. Calculate individual holdings
        List<HoldingResponse> holdingResponses =
                holdings.stream()
                        .map(holding -> {

                            BigDecimal averageBuyPrice =
                                    holding.getAverageBuyPrice();

                            BigDecimal currentPrice =
                                    holding.getStock()
                                            .getCurrentPrice();

                            BigDecimal quantity =
                                    BigDecimal.valueOf(
                                            holding.getQuantity()
                                    );

                            // Total amount invested
                            BigDecimal investedAmount =
                                    averageBuyPrice
                                            .multiply(quantity);

                            // Current market value
                            BigDecimal currentValue =
                                    currentPrice
                                            .multiply(quantity);

                            // Profit / Loss
                            BigDecimal profitLoss =
                                    currentValue
                                            .subtract(
                                                    investedAmount
                                            );

                            // Profit / Loss percentage
                            BigDecimal profitLossPercentage =
                                    BigDecimal.ZERO;

                            if (investedAmount.compareTo(
                                    BigDecimal.ZERO
                            ) > 0) {

                                profitLossPercentage =
                                        profitLoss
                                                .multiply(
                                                        BigDecimal
                                                                .valueOf(100)
                                                )
                                                .divide(
                                                        investedAmount,
                                                        2,
                                                        RoundingMode.HALF_UP
                                                );
                            }

                            return new HoldingResponse(
                                    holding.getStock()
                                            .getSymbol(),

                                    holding.getStock()
                                            .getCompanyName(),

                                    holding.getQuantity(),

                                    averageBuyPrice,

                                    currentPrice,

                                    investedAmount,

                                    currentValue,

                                    profitLoss,

                                    profitLossPercentage
                            );
                        })
                        .toList();

        // 4. Calculate portfolio totals
        for (HoldingResponse holding :
                holdingResponses) {

            totalInvested =
                    totalInvested.add(
                            holding.getInvestedAmount()
                    );

            currentPortfolioValue =
                    currentPortfolioValue.add(
                            holding.getCurrentValue()
                    );
        }

        // 5. Total P/L
        BigDecimal totalProfitLoss =
                currentPortfolioValue
                        .subtract(totalInvested);

        // 6. Total P/L percentage
        BigDecimal totalProfitLossPercentage =
                BigDecimal.ZERO;

        if (totalInvested.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            totalProfitLossPercentage =
                    totalProfitLoss
                            .multiply(
                                    BigDecimal.valueOf(100)
                            )
                            .divide(
                                    totalInvested,
                                    2,
                                    RoundingMode.HALF_UP
                            );
        }

        // 7. Return portfolio
        return new PortfolioResponse(
                user.getVirtualBalance(),
                totalInvested,
                currentPortfolioValue,
                totalProfitLoss,
                totalProfitLossPercentage,
                holdingResponses
        );
    }
}