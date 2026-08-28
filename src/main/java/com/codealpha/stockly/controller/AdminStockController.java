package com.codealpha.stockly.controller;

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

    private final StockService stockService;

    public AdminStockController(
            StockService stockService
    ) {
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

        return new StockResponse(
                stock.getId(),
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getCurrentPrice(),

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
}