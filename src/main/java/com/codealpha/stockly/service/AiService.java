package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestClient restClient;
    private final String apiKey;

    private final InstrumentService instrumentService;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;

    public AiService(
            @Value("${groq.base-url}") String baseUrl,
            @Value("${GROQ_API_KEY}") String apiKey,
            InstrumentService instrumentService,
            StockPriceHistoryRepository stockPriceHistoryRepository
    ) {
        this.restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .build();

        this.apiKey = apiKey;
        this.instrumentService = instrumentService;
        this.stockPriceHistoryRepository =
                stockPriceHistoryRepository;
    }

    // =========================================================
    // TEST CONNECTION
    // =========================================================

    public String testConnection() {

        return callGroq(
                "Reply with exactly: Stockly AI is connected."
        );
    }

    // =========================================================
    // STOCK ANALYSIS
    // =========================================================

    public String analyzeStock(String symbol) {

        Instrument instrument =
                instrumentService.getBySymbol(
                        symbol.trim().toUpperCase()
                );

        Stock stock = instrument.getStock();

        if (stock == null) {
            throw new IllegalStateException(
                    "No stock is linked to instrument: "
                            + instrument.getSymbol()
            );
        }

        /*
         * Get recent history.
         *
         * Repository returns newest first.
         * We reverse it so calculations happen
         * chronologically from oldest -> newest.
         */
        List<StockPriceHistory> history =
                stockPriceHistoryRepository
                        .findByStockOrderByRecordedAtDesc(stock)
                        .stream()
                        .limit(100)
                        .toList();

        List<StockPriceHistory> chronologicalHistory =
                new ArrayList<>(history);

        Collections.reverse(
                chronologicalHistory
        );

        // =====================================================
        // CALCULATED METRICS
        // =====================================================

        double currentPrice =
                toDouble(stock.getCurrentPrice());

        double previousClose =
                toDouble(stock.getPreviousClose());

        double openingPrice =
                toDouble(stock.getOpeningPrice());

        double dayHigh =
                toDouble(stock.getDayHigh());

        double dayLow =
                toDouble(stock.getDayLow());

        double periodReturn =
                calculateReturn(
                        chronologicalHistory
                );

        double historyHigh =
                calculateHigh(
                        chronologicalHistory,
                        currentPrice
                );

        double historyLow =
                calculateLow(
                        chronologicalHistory,
                        currentPrice
                );

        double historyRangePercent =
                calculateRangePercent(
                        historyHigh,
                        historyLow
                );

        double sma20 =
                calculateSma(
                        chronologicalHistory,
                        20
                );

        double volatility =
                calculateVolatility(
                        chronologicalHistory
                );

        double rsi14 =
                calculateRsi(
                        chronologicalHistory,
                        14
                );

        String trend =
                determineTrend(
                        currentPrice,
                        sma20,
                        periodReturn
                );

        // =====================================================
        // PRICE HISTORY FOR AI
        // =====================================================

        StringBuilder historyText =
                new StringBuilder();

        for (StockPriceHistory point :
                chronologicalHistory) {

            historyText
                    .append(point.getRecordedAt())
                    .append(" : ")
                    .append(point.getPrice())
                    .append("\n");
        }

        // =====================================================
        // AI PROMPT
        // =====================================================

        String prompt = """
                You are Stockly AI, an educational assistant
                for a virtual stock trading platform.

                Analyze the stock using ONLY the Stockly data
                supplied below.

                IMPORTANT RULES:
                - Do not invent news.
                - Do not invent earnings information.
                - Do not invent analyst ratings.
                - Do not claim knowledge of events that are not supplied.
                - Do not call the stock guaranteed bullish or bearish.
                - Do not give guaranteed predictions.
                - Clearly distinguish calculated metrics from interpretation.
                - If a metric is unavailable, say so.
                - This is a virtual trading simulation.
                - This response is educational and not financial advice.

                =================================================
                STOCK DATA
                =================================================

                Symbol:
                %s

                Company:
                %s

                Exchange:
                %s

                Sector:
                %s

                Current Price:
                %s

                Previous Close:
                %s

                Opening Price:
                %s

                Day High:
                %s

                Day Low:
                %s

                =================================================
                STOCKLY CALCULATED METRICS
                =================================================

                Available-period Return:
                %s

                Historical High:
                %s

                Historical Low:
                %s

                Historical Range:
                %s

                SMA-20:
                %s

                Volatility:
                %s

                RSI-14:
                %s

                Trend:
                %s

                =================================================
                RECENT PRICE HISTORY
                =================================================

                %s

                =================================================
                RESPONSE FORMAT
                =================================================

                ## Market Snapshot

                Briefly explain the current price and
                available market information.

                ## Price Trend

                Explain the calculated trend, return,
                SMA-20 relationship and recent price movement.

                ## Technical Signals

                Explain RSI-14, volatility and price range.

                If RSI or SMA cannot be reliably calculated
                because there is insufficient history,
                explicitly mention that.

                ## Risk Factors

                Discuss risks visible from the supplied
                price data only.

                Do not invent external events.

                ## Educational Takeaway

                Give a concise educational interpretation
                suitable for someone using a virtual
                trading simulator.

                Do not tell the user to buy or sell.
                """.formatted(
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getExchange(),
                stock.getSector(),
                formatNumber(currentPrice),
                formatNumber(previousClose),
                formatNumber(openingPrice),
                formatNumber(dayHigh),
                formatNumber(dayLow),
                formatPercent(periodReturn),
                formatNumber(historyHigh),
                formatNumber(historyLow),
                formatPercent(historyRangePercent),
                formatNumber(sma20),
                formatPercent(volatility),
                formatNumber(rsi14),
                trend,
                historyText
        );

        return callGroq(prompt);
    }

    // =========================================================
// STOCK CONTEXT FOR AI CHAT
// =========================================================

    public String getStockContext(String symbol) {

        Instrument instrument =
                instrumentService.getBySymbol(
                        symbol.trim().toUpperCase()
                );

        Stock stock = instrument.getStock();

        if (stock == null) {
            throw new IllegalStateException(
                    "No stock is linked to instrument: "
                            + instrument.getSymbol()
            );
        }

        List<StockPriceHistory> history =
                stockPriceHistoryRepository
                        .findByStockOrderByRecordedAtDesc(stock)
                        .stream()
                        .limit(100)
                        .toList();

        List<StockPriceHistory> chronologicalHistory =
                new ArrayList<>(history);

        Collections.reverse(
                chronologicalHistory
        );

        double currentPrice =
                toDouble(stock.getCurrentPrice());

        double previousClose =
                toDouble(stock.getPreviousClose());

        double openingPrice =
                toDouble(stock.getOpeningPrice());

        double dayHigh =
                toDouble(stock.getDayHigh());

        double dayLow =
                toDouble(stock.getDayLow());

        double periodReturn =
                calculateReturn(
                        chronologicalHistory
                );

        double historyHigh =
                calculateHigh(
                        chronologicalHistory,
                        currentPrice
                );

        double historyLow =
                calculateLow(
                        chronologicalHistory,
                        currentPrice
                );

        double historyRangePercent =
                calculateRangePercent(
                        historyHigh,
                        historyLow
                );

        double sma20 =
                calculateSma(
                        chronologicalHistory,
                        20
                );

        double volatility =
                calculateVolatility(
                        chronologicalHistory
                );

        double rsi14 =
                calculateRsi(
                        chronologicalHistory,
                        14
                );

        String trend =
                determineTrend(
                        currentPrice,
                        sma20,
                        periodReturn
                );

        StringBuilder context =
                new StringBuilder();

        context.append("\nSTOCK DATA:\n");

        context.append("Symbol: ")
                .append(stock.getSymbol())
                .append("\n");

        context.append("Company: ")
                .append(stock.getCompanyName())
                .append("\n");

        context.append("Exchange: ")
                .append(stock.getExchange())
                .append("\n");

        context.append("Sector: ")
                .append(stock.getSector())
                .append("\n");

        context.append("\nCURRENT PRICE DATA:\n");

        context.append("Current Price: ")
                .append(formatNumber(currentPrice))
                .append("\n");

        context.append("Previous Close: ")
                .append(formatNumber(previousClose))
                .append("\n");

        context.append("Opening Price: ")
                .append(formatNumber(openingPrice))
                .append("\n");

        context.append("Day High: ")
                .append(formatNumber(dayHigh))
                .append("\n");

        context.append("Day Low: ")
                .append(formatNumber(dayLow))
                .append("\n");

        context.append("\nCALCULATED TECHNICAL METRICS:\n");

        context.append("Available-period Return: ")
                .append(formatPercent(periodReturn))
                .append("\n");

        context.append("Historical High: ")
                .append(formatNumber(historyHigh))
                .append("\n");

        context.append("Historical Low: ")
                .append(formatNumber(historyLow))
                .append("\n");

        context.append("Historical Range: ")
                .append(formatPercent(historyRangePercent))
                .append("\n");

        context.append("SMA-20: ")
                .append(formatNumber(sma20))
                .append("\n");

        context.append("Volatility: ")
                .append(formatPercent(volatility))
                .append("\n");

        context.append("RSI-14: ")
                .append(formatNumber(rsi14))
                .append("\n");

        context.append("Trend: ")
                .append(trend)
                .append("\n");

        context.append("\nRECENT PRICE HISTORY:\n");

        for (StockPriceHistory point :
                chronologicalHistory) {

            context.append(point.getRecordedAt())
                    .append(" : ")
                    .append(point.getPrice())
                    .append("\n");
        }

        return context.toString();
    }

    // =========================================================
    // RETURN
    // =========================================================

    private double calculateReturn(
            List<StockPriceHistory> history
    ) {

        if (history.size() < 2) {
            return 0;
        }

        double first =
                toDouble(
                        history.get(0).getPrice()
                );

        double last =
                toDouble(
                        history.get(
                                history.size() - 1
                        ).getPrice()
                );

        if (first <= 0) {
            return 0;
        }

        return ((last - first) / first) * 100;
    }

    // =========================================================
    // HIGH
    // =========================================================

    private double calculateHigh(
            List<StockPriceHistory> history,
            double fallback
    ) {

        if (history.isEmpty()) {
            return fallback;
        }

        double high =
                Double.MIN_VALUE;

        for (StockPriceHistory point :
                history) {

            double price =
                    toDouble(
                            point.getPrice()
                    );

            if (price > high) {
                high = price;
            }
        }

        return high;
    }

    // =========================================================
    // LOW
    // =========================================================

    private double calculateLow(
            List<StockPriceHistory> history,
            double fallback
    ) {

        if (history.isEmpty()) {
            return fallback;
        }

        double low =
                Double.MAX_VALUE;

        for (StockPriceHistory point :
                history) {

            double price =
                    toDouble(
                            point.getPrice()
                    );

            if (price < low) {
                low = price;
            }
        }

        return low;
    }

    // =========================================================
    // RANGE %
    // =========================================================

    private double calculateRangePercent(
            double high,
            double low
    ) {

        if (low <= 0) {
            return 0;
        }

        return ((high - low) / low) * 100;
    }

    // =========================================================
    // SMA
    // =========================================================

    private double calculateSma(
            List<StockPriceHistory> history,
            int period
    ) {

        if (history.size() < period) {
            return 0;
        }

        double sum = 0;

        int start =
                history.size() - period;

        for (int i = start;
             i < history.size();
             i++) {

            sum +=
                    toDouble(
                            history.get(i)
                                    .getPrice()
                    );
        }

        return sum / period;
    }

    // =========================================================
    // VOLATILITY
    // =========================================================

    private double calculateVolatility(
            List<StockPriceHistory> history
    ) {

        if (history.size() < 2) {
            return 0;
        }

        List<Double> returns =
                new ArrayList<>();

        for (int i = 1;
             i < history.size();
             i++) {

            double previous =
                    toDouble(
                            history.get(i - 1)
                                    .getPrice()
                    );

            double current =
                    toDouble(
                            history.get(i)
                                    .getPrice()
                    );

            if (previous <= 0) {
                continue;
            }

            double returnValue =
                    ((current - previous)
                            / previous) * 100;

            returns.add(returnValue);
        }

        if (returns.size() < 2) {
            return 0;
        }

        double mean =
                returns.stream()
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .average()
                        .orElse(0);

        double squaredDifference = 0;

        for (double value : returns) {

            squaredDifference +=
                    Math.pow(
                            value - mean,
                            2
                    );
        }

        return Math.sqrt(
                squaredDifference /
                        returns.size()
        );
    }

    // =========================================================
    // RSI
    // =========================================================

    private double calculateRsi(
            List<StockPriceHistory> history,
            int period
    ) {

        if (history.size() <= period) {
            return 0;
        }

        double gains = 0;
        double losses = 0;

        int start =
                history.size() - period;

        for (int i = start;
             i < history.size();
             i++) {

            double previous =
                    toDouble(
                            history.get(i - 1)
                                    .getPrice()
                    );

            double current =
                    toDouble(
                            history.get(i)
                                    .getPrice()
                    );

            double change =
                    current - previous;

            if (change > 0) {
                gains += change;
            } else {
                losses += Math.abs(change);
            }
        }

        double averageGain =
                gains / period;

        double averageLoss =
                losses / period;

        if (averageLoss == 0) {
            return averageGain > 0
                    ? 100
                    : 50;
        }

        double relativeStrength =
                averageGain /
                        averageLoss;

        return 100 -
                (100 /
                        (1 + relativeStrength));
    }

    // =========================================================
    // TREND
    // =========================================================

    private String determineTrend(
            double currentPrice,
            double sma20,
            double periodReturn
    ) {

        if (sma20 <= 0) {

            if (periodReturn > 0) {
                return "Upward based on available price history";
            }

            if (periodReturn < 0) {
                return "Downward based on available price history";
            }

            return "Neutral";
        }

        if (currentPrice > sma20 &&
                periodReturn > 0) {

            return "Upward";
        }

        if (currentPrice < sma20 &&
                periodReturn < 0) {

            return "Downward";
        }

        return "Mixed / Neutral";
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
                                        "You are Stockly AI, an educational virtual trading assistant."
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
                        firstChoice.get(
                                "message"
                        );

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

    // =========================================================
    // HELPERS
    // =========================================================

    private double toDouble(
            java.math.BigDecimal value
    ) {

        if (value == null) {
            return 0;
        }

        return value.doubleValue();
    }

    private String formatNumber(
            double value
    ) {

        if (value <= 0) {
            return "Unavailable";
        }

        return String.format(
                "%.2f",
                value
        );
    }

    private String formatPercent(
            double value
    ) {

        if (Double.isNaN(value) ||
                Double.isInfinite(value)) {

            return "Unavailable";
        }

        return String.format(
                "%.2f%%",
                value
        );
    }
}