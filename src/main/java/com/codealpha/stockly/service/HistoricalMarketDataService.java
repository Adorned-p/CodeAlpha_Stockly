package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalTimeSeriesResponse;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class HistoricalMarketDataService {

    private final InstrumentService instrumentService;
    private final ExternalMarketDataClient externalMarketDataClient;
    private final StockPriceHistoryRepository stockPriceHistoryRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> SUPPORTED_INTERVALS =
            Set.of(
                    "1min",
                    "5min",
                    "15min",
                    "30min",
                    "45min",
                    "1h",
                    "2h",
                    "4h",
                    "8h",
                    "1day",
                    "1week",
                    "1month"
            );

    private static final int MIN_OUTPUT_SIZE = 1;
    private static final int MAX_OUTPUT_SIZE = 5000;

    public HistoricalMarketDataService(
            InstrumentService instrumentService,
            ExternalMarketDataClient externalMarketDataClient,
            StockPriceHistoryRepository stockPriceHistoryRepository
    ) {
        this.instrumentService =
                instrumentService;

        this.externalMarketDataClient =
                externalMarketDataClient;

        this.stockPriceHistoryRepository =
                stockPriceHistoryRepository;
    }

    // =========================================================
    // IMPORT HISTORICAL DATA
    // =========================================================

    @Transactional
    public int importHistoricalData(
            String symbol,
            String interval,
            int outputSize
    ) {

        // -----------------------------------------------------
        // VALIDATE SYMBOL
        // -----------------------------------------------------

        if (symbol == null || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol is required."
            );
        }

        // -----------------------------------------------------
        // VALIDATE INTERVAL
        // -----------------------------------------------------

        if (interval == null || interval.isBlank()) {

            throw new IllegalArgumentException(
                    "Interval is required."
            );
        }

        interval =
                interval.trim().toLowerCase();

        if (!SUPPORTED_INTERVALS.contains(interval)) {

            throw new IllegalArgumentException(
                    "Unsupported interval: "
                            + interval
                            + ". Supported intervals: "
                            + SUPPORTED_INTERVALS
            );
        }

        // -----------------------------------------------------
        // VALIDATE OUTPUT SIZE
        // -----------------------------------------------------

        if (outputSize < MIN_OUTPUT_SIZE ||
                outputSize > MAX_OUTPUT_SIZE) {

            throw new IllegalArgumentException(
                    "Output size must be between "
                            + MIN_OUTPUT_SIZE
                            + " and "
                            + MAX_OUTPUT_SIZE
            );
        }

        // -----------------------------------------------------
        // FIND INSTRUMENT
        // -----------------------------------------------------

        Instrument instrument =
                instrumentService.getBySymbol(
                        symbol.trim().toUpperCase()
                );

        // -----------------------------------------------------
        // FIND LINKED STOCK
        // -----------------------------------------------------

        Stock stock =
                instrument.getStock();

        if (stock == null) {

            throw new IllegalStateException(
                    "No Stock is linked to instrument: "
                            + instrument.getSymbol()
            );
        }

        // -----------------------------------------------------
        // REQUEST HISTORICAL DATA
        // -----------------------------------------------------

        ExternalTimeSeriesResponse response;

        try {

            response =
                    externalMarketDataClient.getTimeSeries(
                            instrument.getSymbol(),
                            instrument.getExchange(),
                            interval,
                            outputSize
                    );

        } catch (
                HttpClientErrorException.TooManyRequests exception
        ) {

            throw new IllegalStateException(
                    "Twelve Data rate limit or daily quota "
                            + "has been exceeded. "
                            + "Please wait for the quota to reset "
                            + "before importing historical data.",
                    exception
            );
        }

        // -----------------------------------------------------
        // VALIDATE RESPONSE
        // -----------------------------------------------------

        if (response == null) {

            throw new IllegalStateException(
                    "No response received from Twelve Data."
            );
        }

        if (!"ok".equalsIgnoreCase(
                response.getStatus()
        )) {

            String message =
                    response.getMessage();

            if (message == null ||
                    message.isBlank()) {

                message =
                        "Unknown error returned by Twelve Data.";
            }

            throw new IllegalStateException(
                    "Twelve Data error: "
                            + message
            );
        }

        List<ExternalTimeSeriesResponse.TimeSeriesValue>
                values =
                response.getValues();

        if (values == null ||
                values.isEmpty()) {

            return 0;
        }

        // -----------------------------------------------------
        // SAVE HISTORY
        // -----------------------------------------------------

        int imported = 0;

        for (
                ExternalTimeSeriesResponse.TimeSeriesValue value
                : values
        ) {

            if (value == null ||
                    value.getDatetime() == null ||
                    value.getClose() == null) {

                continue;
            }

            // -------------------------------------------------
            // PARSE PRICE
            // -------------------------------------------------

            BigDecimal closePrice;

            try {

                closePrice =
                        new BigDecimal(
                                value.getClose()
                        );

            } catch (NumberFormatException exception) {

                continue;
            }

            if (closePrice.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                continue;
            }

            // -------------------------------------------------
            // PARSE TIMESTAMP
            // -------------------------------------------------

            LocalDateTime recordedAt;

            try {

                recordedAt =
                        LocalDateTime.parse(
                                value.getDatetime(),
                                DATE_TIME_FORMATTER
                        );

            } catch (Exception exception) {

                System.err.println(
                        "Unable to parse historical timestamp: "
                                + value.getDatetime()
                );

                continue;
            }

            // -------------------------------------------------
            // PREVENT DUPLICATES
            // -------------------------------------------------

            boolean alreadyExists =
                    stockPriceHistoryRepository
                            .existsByStockAndRecordedAt(
                                    stock,
                                    recordedAt
                            );

            if (alreadyExists) {
                continue;
            }

            // -------------------------------------------------
            // CREATE HISTORY RECORD
            // -------------------------------------------------

            StockPriceHistory history =
                    new StockPriceHistory();

            history.setStock(stock);

            history.setPrice(
                    closePrice
            );

            history.setRecordedAt(
                    recordedAt
            );

            stockPriceHistoryRepository.save(
                    history
            );

            imported++;
        }

        return imported;
    }
}