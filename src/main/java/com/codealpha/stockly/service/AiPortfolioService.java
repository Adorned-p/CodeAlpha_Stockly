package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Holding;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.HoldingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class AiPortfolioService {

    private final RestClient restClient;
    private final String apiKey;

    private final HoldingRepository holdingRepository;

    public AiPortfolioService(
            @Value("${groq.base-url}") String baseUrl,
            @Value("${GROQ_API_KEY}") String apiKey,
            HoldingRepository holdingRepository
    ) {
        this.restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .build();

        this.apiKey = apiKey;
        this.holdingRepository =
                holdingRepository;
    }

    // =========================================================
    // ANALYZE USER PORTFOLIO
    // =========================================================

    public String analyzePortfolio(
            User user
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Authenticated user not found."
            );
        }

        List<Holding> holdings =
                holdingRepository.findByUser(user);

        BigDecimal totalInvested =
                BigDecimal.ZERO;

        BigDecimal currentValue =
                BigDecimal.ZERO;

        StringBuilder holdingsText =
                new StringBuilder();

        // =====================================================
        // CALCULATE PORTFOLIO METRICS
        // =====================================================

        for (Holding holding : holdings) {

            if (holding == null ||
                    holding.getStock() == null) {
                continue;
            }

            int quantity =
                    holding.getQuantity();

            if (quantity <= 0) {
                continue;
            }

            BigDecimal averageBuyPrice =
                    holding.getAverageBuyPrice();

            BigDecimal stockPrice =
                    holding.getStock()
                            .getCurrentPrice();

            if (averageBuyPrice == null) {
                averageBuyPrice =
                        BigDecimal.ZERO;
            }

            if (stockPrice == null) {
                stockPrice =
                        BigDecimal.ZERO;
            }

            BigDecimal invested =
                    averageBuyPrice.multiply(
                            BigDecimal.valueOf(
                                    quantity
                            )
                    );

            BigDecimal value =
                    stockPrice.multiply(
                            BigDecimal.valueOf(
                                    quantity
                            )
                    );

            BigDecimal profitLoss =
                    value.subtract(
                            invested
                    );

            totalInvested =
                    totalInvested.add(
                            invested
                    );

            currentValue =
                    currentValue.add(
                            value
                    );

            holdingsText
                    .append(
                            holding.getStock()
                                    .getSymbol()
                    )
                    .append(" | Company: ")
                    .append(
                            holding.getStock()
                                    .getCompanyName()
                    )
                    .append(" | Quantity: ")
                    .append(quantity)
                    .append(" | Average Buy Price: ")
                    .append(averageBuyPrice)
                    .append(" | Current Price: ")
                    .append(stockPrice)
                    .append(" | Invested: ")
                    .append(invested)
                    .append(" | Current Value: ")
                    .append(value)
                    .append(" | P/L: ")
                    .append(profitLoss)
                    .append("\n");
        }

        // =====================================================
        // TOTAL PROFIT / LOSS
        // =====================================================

        BigDecimal totalProfitLoss =
                currentValue.subtract(
                        totalInvested
                );

        // =====================================================
        // PORTFOLIO RETURN
        // =====================================================

        BigDecimal returnPercentage =
                BigDecimal.ZERO;

        if (totalInvested.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            returnPercentage =
                    totalProfitLoss
                            .divide(
                                    totalInvested,
                                    6,
                                    RoundingMode.HALF_UP
                            )
                            .multiply(
                                    BigDecimal.valueOf(100)
                            );
        }

        // =====================================================
        // CASH BALANCE
        // =====================================================

        BigDecimal cashBalance =
                user.getVirtualBalance();

        if (cashBalance == null) {
            cashBalance =
                    BigDecimal.ZERO;
        }

        BigDecimal totalPortfolioValue =
                cashBalance.add(
                        currentValue
                );

        // =====================================================
        // PORTFOLIO ALLOCATION
        // =====================================================

        StringBuilder allocationText =
                new StringBuilder();

        if (currentValue.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            for (Holding holding : holdings) {

                if (holding == null ||
                        holding.getStock() == null) {
                    continue;
                }

                int quantity =
                        holding.getQuantity();

                if (quantity <= 0) {
                    continue;
                }

                BigDecimal price =
                        holding.getStock()
                                .getCurrentPrice();

                if (price == null) {
                    continue;
                }

                BigDecimal value =
                        price.multiply(
                                BigDecimal.valueOf(
                                        quantity
                                )
                        );

                BigDecimal percentage =
                        value
                                .divide(
                                        currentValue,
                                        6,
                                        RoundingMode.HALF_UP
                                )
                                .multiply(
                                        BigDecimal.valueOf(100)
                                );

                allocationText
                        .append(
                                holding.getStock()
                                        .getSymbol()
                        )
                        .append(": ")
                        .append(
                                percentage.setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                )
                        )
                        .append("%\n");
            }
        }

        // =====================================================
        // AI PROMPT
        // =====================================================

        String prompt = """
                You are Stockly AI, an educational portfolio
                assistant for a virtual stock trading platform.

                Analyze the user's portfolio using ONLY the
                supplied Stockly data.

                IMPORTANT RULES:

                - Do not invent market news.
                - Do not invent company information.
                - Do not invent analyst ratings.
                - Do not make guaranteed predictions.
                - Do not tell the user to buy or sell.
                - Do not claim that diversification is good or
                  bad without explaining why based on the supplied data.
                - Clearly distinguish calculated metrics from
                  AI interpretation.
                - This is a virtual trading simulation.
                - This is educational information, not financial advice.

                =================================================
                PORTFOLIO SUMMARY
                =================================================

                Cash Balance:
                %s

                Total Invested:
                %s

                Current Holdings Value:
                %s

                Total Portfolio Value:
                %s

                Total Profit / Loss:
                %s

                Portfolio Return:
                %s

                =================================================
                HOLDINGS
                =================================================

                %s

                =================================================
                PORTFOLIO ALLOCATION
                =================================================

                %s

                =================================================
                RESPONSE FORMAT
                =================================================

                ## Portfolio Snapshot

                Summarize the portfolio's current value,
                invested amount, cash and overall P/L.

                ## Performance

                Explain the portfolio return and identify
                which holdings contribute most to the current
                profit or loss.

                ## Diversification

                Explain the concentration of the portfolio
                using the supplied allocation percentages.

                Mention if the portfolio appears highly
                concentrated in one or a few positions.

                ## Risk Observations

                Discuss observable portfolio risks based only
                on the supplied holdings, concentration,
                current values and P/L.

                ## Educational Takeaway

                Give practical educational observations about
                portfolio management and diversification.

                Do not provide a direct buy, sell or hold
                recommendation.
                """.formatted(
                cashBalance,
                totalInvested,
                currentValue,
                totalPortfolioValue,
                totalProfitLoss,
                returnPercentage.setScale(
                        2,
                        RoundingMode.HALF_UP
                ),
                holdingsText,
                allocationText
        );

        return callGroq(prompt);
    }

    // =========================================================
    // GROQ REQUEST
    // =========================================================

    private String callGroq(
            String prompt
    ) {

        Map<String, Object> requestBody =
                Map.of(
                        "model",
                        "openai/gpt-oss-20b",

                        "messages",
                        List.of(
                                Map.of(
                                        "role",
                                        "system",
                                        "content",
                                        "You are Stockly AI, an educational virtual portfolio assistant."
                                ),
                                Map.of(
                                        "role",
                                        "user",
                                        "content",
                                        prompt
                                )
                        ),

                        "temperature",
                        0.3,

                        "max_completion_tokens",
                        1400
                );

        Map<String, Object> response =
                restClient
                        .post()
                        .uri("/chat/completions")
                        .header(
                                "Authorization",
                                "Bearer " + apiKey
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "No response received from Groq."
            );
        }

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>)
                        response.get("choices");

        if (choices == null ||
                choices.isEmpty()) {

            throw new IllegalStateException(
                    "Groq returned no choices."
            );
        }

        Map<String, Object> firstChoice =
                choices.get(0);

        Map<String, Object> message =
                (Map<String, Object>)
                        firstChoice.get("message");

        if (message == null) {
            throw new IllegalStateException(
                    "Groq response did not contain a message."
            );
        }

        Object content =
                message.get("content");

        if (content == null) {
            throw new IllegalStateException(
                    "Groq response did not contain message content."
            );
        }

        return content.toString();
    }
}