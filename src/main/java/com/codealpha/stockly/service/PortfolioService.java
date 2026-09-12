package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.PortfolioHistoryResponse;
import com.codealpha.stockly.entity.PortfolioSnapshot;
import com.codealpha.stockly.repository.PortfolioSnapshotRepository;

import java.time.LocalDateTime;

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
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final CurrencyConversionService currencyConversionService;

    public PortfolioService(
            UserRepository userRepository,
            HoldingRepository holdingRepository,
            PortfolioSnapshotRepository portfolioSnapshotRepository,
            CurrencyConversionService currencyConversionService
    ) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.portfolioSnapshotRepository =
                portfolioSnapshotRepository;
        this.currencyConversionService =
                currencyConversionService;
    }

    public PortfolioResponse getPortfolio(
            String userEmail
    ) {

        // =========================================================
        // 1. FIND LOGGED-IN USER
        // =========================================================

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        // =========================================================
        // 2. FIND ALL HOLDINGS
        // =========================================================

        List<Holding> holdings =
                holdingRepository.findByUser(user);

        BigDecimal totalInvested =
                BigDecimal.ZERO;

        BigDecimal currentPortfolioValue =
                BigDecimal.ZERO;

        BigDecimal totalInvestedInr =
                BigDecimal.ZERO;

        BigDecimal currentPortfolioValueInr =
                BigDecimal.ZERO;

        // =========================================================
        // 3. CALCULATE INDIVIDUAL HOLDINGS
        // =========================================================

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

                            // -------------------------------------------------
                            // Native currency calculations
                            // -------------------------------------------------

                            BigDecimal investedAmount =
                                    averageBuyPrice
                                            .multiply(quantity);

                            BigDecimal currentValue =
                                    currentPrice
                                            .multiply(quantity);

                            BigDecimal profitLoss =
                                    currentValue
                                            .subtract(
                                                    investedAmount
                                            );

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

                            // -------------------------------------------------
                            // Determine stock currency
                            // -------------------------------------------------

                            String currency =
                                    getStockCurrency(
                                            holding.getStock()
                                    );

                            // -------------------------------------------------
                            // INR conversion
                            // -------------------------------------------------

                            BigDecimal averageBuyPriceInr;
                            BigDecimal currentPriceInr;
                            BigDecimal investedAmountInr;
                            BigDecimal currentValueInr;
                            BigDecimal profitLossInr;
                            BigDecimal exchangeRateToInr;

                            if ("INR".equals(currency)) {

                                averageBuyPriceInr =
                                        averageBuyPrice;

                                currentPriceInr =
                                        currentPrice;

                                investedAmountInr =
                                        investedAmount;

                                currentValueInr =
                                        currentValue;

                                profitLossInr =
                                        profitLoss;

                                exchangeRateToInr =
                                        BigDecimal.ONE;

                            } else {

                                averageBuyPriceInr =
                                        currencyConversionService
                                                .convertToInr(
                                                        averageBuyPrice,
                                                        currency
                                                );

                                currentPriceInr =
                                        currencyConversionService
                                                .convertToInr(
                                                        currentPrice,
                                                        currency
                                                );

                                investedAmountInr =
                                        currencyConversionService
                                                .convertToInr(
                                                        investedAmount,
                                                        currency
                                                );

                                currentValueInr =
                                        currencyConversionService
                                                .convertToInr(
                                                        currentValue,
                                                        currency
                                                );

                                profitLossInr =
                                        currencyConversionService
                                                .convertToInr(
                                                        profitLoss,
                                                        currency
                                                );

                                exchangeRateToInr =
                                        currencyConversionService
                                                .getExchangeRate(
                                                        currency
                                                );
                            }

                            return new HoldingResponse(

                                    holding.getStock()
                                            .getSymbol(),

                                    holding.getStock()
                                            .getCompanyName(),

                                    holding.getQuantity(),

                                    // Native
                                    averageBuyPrice,
                                    currentPrice,
                                    investedAmount,
                                    currentValue,
                                    profitLoss,

                                    // INR
                                    averageBuyPriceInr,
                                    currentPriceInr,
                                    investedAmountInr,
                                    currentValueInr,
                                    profitLossInr,

                                    exchangeRateToInr,
                                    profitLossPercentage
                            );
                        })
                        .toList();

        // =========================================================
        // 4. CALCULATE PORTFOLIO TOTALS
        // =========================================================

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

            totalInvestedInr =
                    totalInvestedInr.add(
                            holding.getInvestedAmountInr()
                    );

            currentPortfolioValueInr =
                    currentPortfolioValueInr.add(
                            holding.getCurrentValueInr()
                    );
        }

        // =========================================================
        // 5. TOTAL P/L
        // =========================================================

        BigDecimal totalProfitLoss =
                currentPortfolioValue
                        .subtract(totalInvested);

        BigDecimal totalProfitLossInr =
                currentPortfolioValueInr
                        .subtract(totalInvestedInr);

        // =========================================================
        // 6. TOTAL P/L PERCENTAGE
        // =========================================================

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

        // =========================================================
        // 7. VIRTUAL BALANCE
        // =========================================================

        /*
         * Virtual balance is already maintained by the trading
         * system in its existing account currency.
         *
         * We are not changing the actual balance used by BUY/SELL.
         * For the current platform this is INR.
         */
        BigDecimal virtualBalance =
                user.getVirtualBalance();

        BigDecimal virtualBalanceInr =
                virtualBalance;

        // =========================================================
        // 8. RETURN PORTFOLIO
        // =========================================================

        return new PortfolioResponse(

                // Native
                virtualBalance,
                totalInvested,
                currentPortfolioValue,
                totalProfitLoss,

                // INR
                virtualBalanceInr,
                totalInvestedInr,
                currentPortfolioValueInr,
                totalProfitLossInr,

                totalProfitLossPercentage,

                holdingResponses
        );
    }

    // =========================================================
    // PORTFOLIO HISTORY
    // =========================================================

    public List<PortfolioHistoryResponse> getPortfolioHistory(
            String userEmail,
            LocalDateTime start,
            LocalDateTime end
    ) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        return portfolioSnapshotRepository
                .findByUserAndRecordedAtBetweenOrderByRecordedAtAsc(
                        user,
                        start,
                        end
                )
                .stream()
                .map(snapshot ->
                        new PortfolioHistoryResponse(
                                snapshot.getPortfolioValue(),
                                snapshot.getRecordedAt()
                        )
                )
                .toList();
    }

    // =========================================================
    // STOCK CURRENCY
    // =========================================================

    private String getStockCurrency(
            com.codealpha.stockly.entity.Stock stock
    ) {

        String exchange =
                stock.getExchange();

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