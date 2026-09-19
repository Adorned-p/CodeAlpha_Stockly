package com.codealpha.stockly.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.repository.InstrumentRepository;
import com.codealpha.stockly.entity.TimeRange;
import com.codealpha.stockly.dto.StockPriceHistoryResponse;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;

import com.codealpha.stockly.dto.StockRequest;
import com.codealpha.stockly.dto.StockResponse;
import com.codealpha.stockly.dto.StockUpdateRequest;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.service.StockService;
import com.codealpha.stockly.service.CurrencyConversionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockPriceHistoryRepository priceHistoryRepository;
    private final StockService stockService;
    private final CurrencyConversionService currencyConversionService;
    private final InstrumentRepository instrumentRepository;

    public StockController(
            StockService stockService,
            StockPriceHistoryRepository priceHistoryRepository,
            CurrencyConversionService currencyConversionService,
            InstrumentRepository instrumentRepository
    ) {
        this.stockService = stockService;
        this.priceHistoryRepository = priceHistoryRepository;
        this.currencyConversionService = currencyConversionService;
        this.instrumentRepository = instrumentRepository;
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
            @RequestParam String exchange,
            @RequestParam(defaultValue = "ONE_DAY") TimeRange range
    ) {

        Stock stock =
                stockService.getStockBySymbolAndExchange(
                        symbol,
                        exchange
                );

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
    // GET STOCK BY SYMBOL + EXCHANGE
    // =========================================================

    @GetMapping("/{symbol}")
    public StockResponse getStockBySymbol(
            @PathVariable String symbol,
            @RequestParam String exchange
    ) {

        Stock stock =
                stockService.getStockBySymbolAndExchange(
                        symbol,
                        exchange
                );

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
                request.getSymbol()
                        .trim()
                        .toUpperCase()
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
                request.getExchange()
                        .trim()
                        .toUpperCase()
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
            @RequestParam String exchange,
            @Valid @RequestBody StockUpdateRequest request
    ) {

        Stock updatedStock =
                stockService.updateStock(
                        symbol,
                        exchange,
                        request.getCompanyName(),
                        request.getCurrentPrice(),
                        request.getSector(),
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
         * Determine native currency from the Instrument first.
         *
         * NSE/BSE -> INR
         * Everything else -> USD fallback.
         */
        String currency =
                getStockCurrency(stock);

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
                currency,
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

        Instrument instrument =
                instrumentRepository
                        .findBySymbolAndExchange(
                                stock.getSymbol(),
                                stock.getExchange()
                        )
                        .orElse(null);

        if (instrument != null &&
                instrument.getCurrency() != null &&
                !instrument.getCurrency().isBlank()) {

            return instrument.getCurrency()
                    .trim()
                    .toUpperCase();
        }

        String exchange =
                stock.getExchange();

        if (exchange == null ||
                exchange.isBlank()) {

            return "USD";
        }

        String normalizedExchange =
                exchange.trim().toUpperCase();

        if ("NSE".equals(normalizedExchange) ||
                "BSE".equals(normalizedExchange)) {

            return "INR";
        }

        return "USD";
    }
}