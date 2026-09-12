package com.codealpha.stockly.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.codealpha.stockly.entity.TimeRange;
import com.codealpha.stockly.dto.StockPriceHistoryResponse;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import java.time.LocalDateTime;

import com.codealpha.stockly.dto.StockRequest;
import com.codealpha.stockly.dto.StockResponse;
import com.codealpha.stockly.dto.StockUpdateRequest;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.service.StockService;
import com.codealpha.stockly.service.CurrencyConversionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockPriceHistoryRepository priceHistoryRepository;
    private final StockService stockService;
    private final CurrencyConversionService currencyConversionService;

    public StockController(
            StockService stockService,
            StockPriceHistoryRepository priceHistoryRepository,
            CurrencyConversionService currencyConversionService
    ) {
        this.stockService = stockService;
        this.priceHistoryRepository = priceHistoryRepository;
        this.currencyConversionService = currencyConversionService;
    }

    // =========================================================
    // SEARCH STOCKS
    // =========================================================

    @GetMapping("/search")
    public List<StockResponse> searchStocks(
            @RequestParam String query
    ) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query is required"
            );
        }

        String search =
                query.trim().toLowerCase();

        return stockService
                .getAllStocks()
                .stream()
                .filter(stock ->
                        stock.getSymbol()
                                .toLowerCase()
                                .contains(search)
                                ||
                                stock.getCompanyName()
                                        .toLowerCase()
                                        .contains(search)
                )
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // PRICE HISTORY
    // =========================================================

    @GetMapping("/{symbol}/history")
    public List<StockPriceHistoryResponse> getPriceHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "ONE_DAY") TimeRange range
    ) {

        Stock stock =
                stockService.getStockBySymbol(symbol);

        LocalDateTime end =
                LocalDateTime.now();

        LocalDateTime start;

        switch (range) {

            case ONE_WEEK:
                start = end.minusDays(7);
                break;

            case ONE_MONTH:
                start = end.minusDays(30);
                break;

            case THREE_MONTHS:
                start = end.minusDays(90);
                break;

            case ONE_YEAR:
                start = end.minusYears(1);
                break;

            case ONE_DAY:
            default:
                start = end.minusHours(24);
                break;
        }

        return priceHistoryRepository
                .findByStockAndRecordedAtBetweenOrderByRecordedAtAsc(
                        stock,
                        start,
                        end
                )
                .stream()
                .map(history ->
                        new StockPriceHistoryResponse(
                                history.getPrice(),
                                history.getRecordedAt()
                        )
                )
                .toList();
    }

    // =========================================================
    // GET ALL STOCKS
    // =========================================================

    @GetMapping
    public List<StockResponse> getAllStocks() {

        return stockService
                .getAllStocks()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET STOCK BY SYMBOL
    // =========================================================

    @GetMapping("/{symbol}")
    public StockResponse getStockBySymbol(
            @PathVariable String symbol
    ) {

        Stock stock =
                stockService.getStockBySymbol(symbol);

        return toResponse(stock);
    }

    // =========================================================
    // CREATE STOCK
    // =========================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockResponse createStock(
            @Valid @RequestBody StockRequest request
    ) {

        Stock stock = new Stock();

        stock.setSymbol(
                request.getSymbol().toUpperCase()
        );

        stock.setCompanyName(
                request.getCompanyName()
        );

        stock.setCurrentPrice(
                request.getCurrentPrice()
        );

        stock.setOpeningPrice(
                request.getCurrentPrice()
        );

        stock.setPreviousClose(
                request.getCurrentPrice()
        );

        stock.setDayHigh(
                request.getCurrentPrice()
        );

        stock.setDayLow(
                request.getCurrentPrice()
        );

        stock.setSector(
                request.getSector()
        );

        stock.setExchange(
                request.getExchange().toUpperCase()
        );

        stock.setStatus(
                com.codealpha.stockly.entity.StockStatus.ACTIVE
        );

        Stock savedStock =
                stockService.saveStock(stock);

        return toResponse(savedStock);
    }

    // =========================================================
    // UPDATE STOCK
    // =========================================================

    @PutMapping("/{symbol}")
    public StockResponse updateStock(
            @PathVariable String symbol,
            @Valid @RequestBody StockUpdateRequest request
    ) {

        Stock updatedStock =
                stockService.updateStock(
                        symbol,
                        request.getCompanyName(),
                        request.getCurrentPrice(),
                        request.getSector(),
                        request.getExchange(),
                        request.getStatus()
                );

        return toResponse(updatedStock);
    }

    // =========================================================
    // ENTITY → RESPONSE
    // =========================================================

    private StockResponse toResponse(
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

        /*
         * Stock currently does not have a currency field.
         *
         * Your existing global stocks use exchange information,
         * so determine the native currency from the exchange.
         *
         * NSE/BSE -> INR
         * Everything else -> USD for the current US-market stocks.
         */
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

    // =========================================================
    // STOCK CURRENCY
    // =========================================================

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