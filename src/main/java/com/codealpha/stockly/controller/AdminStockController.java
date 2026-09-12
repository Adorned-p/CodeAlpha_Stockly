package com.codealpha.stockly.controller;

import com.codealpha.stockly.service.CurrencyConversionService;
import com.codealpha.stockly.dto.PriceUpdateRequest;
import com.codealpha.stockly.dto.StockResponse;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/admin/stocks")
public class AdminStockController {

    private final CurrencyConversionService currencyConversionService;
    private final StockService stockService;

    public AdminStockController(
            StockService stockService,
            CurrencyConversionService currencyConversionService
    ) {
        this.currencyConversionService = currencyConversionService;
        this.stockService = stockService;
    }

    @PutMapping("/{symbol}/price")
    public StockResponse updatePrice(
            @PathVariable String symbol,
            @Valid @RequestBody PriceUpdateRequest request
    ) {

        Stock stock = stockService.updatePrice(
                symbol,
                request.getPrice()
        );

        return toResponse(stock);
    }

    private StockResponse toResponse(Stock stock) {

        BigDecimal priceChange =
                stock.getCurrentPrice()
                        .subtract(stock.getPreviousClose());

        BigDecimal changePercentage =
                BigDecimal.ZERO;

        if (stock.getPreviousClose()
                .compareTo(BigDecimal.ZERO) > 0) {

            changePercentage =
                    priceChange
                            .multiply(BigDecimal.valueOf(100))
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

            currentPriceInr = stock.getCurrentPrice();
            exchangeRateToInr = BigDecimal.ONE;

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