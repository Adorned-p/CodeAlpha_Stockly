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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockPriceHistoryRepository
            priceHistoryRepository;

    private final StockService stockService;

    public StockController(
            StockService stockService,
            StockPriceHistoryRepository priceHistoryRepository
    ) {
        this.stockService = stockService;
        this.priceHistoryRepository = priceHistoryRepository;
    }

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


    /*
     * ================================================
     * GET ALL STOCKS
     * ================================================
     */

    @GetMapping
    public List<StockResponse> getAllStocks() {

        return stockService
                .getAllStocks()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    /*
     * ================================================
     * GET STOCK BY SYMBOL
     * ================================================
     */

    @GetMapping("/{symbol}")
    public StockResponse getStockBySymbol(
            @PathVariable String symbol
    ) {

        Stock stock =
                stockService.getStockBySymbol(symbol);

        return toResponse(stock);
    }


    /*
     * ================================================
     * CREATE STOCK
     * ADMIN ONLY
     * ================================================
     */

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

        stock.setOpeningPrice(request.getCurrentPrice());
        stock.setPreviousClose(request.getCurrentPrice());
        stock.setDayHigh(request.getCurrentPrice());
        stock.setDayLow(request.getCurrentPrice());

        stock.setSector(
                request.getSector()
        );

        stock.setExchange(
                request.getExchange().toUpperCase()
        );

        /*
         * New stocks are ACTIVE.
         */
        stock.setStatus(
                com.codealpha.stockly.entity.StockStatus.ACTIVE
        );

        Stock savedStock =
                stockService.saveStock(stock);

        return toResponse(savedStock);
    }


    /*
     * ================================================
     * UPDATE STOCK
     * ADMIN ONLY
     * ================================================
     */

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


    /*
     * ================================================
     * DELETE STOCK
     * ADMIN ONLY
     * ================================================
     */

//    @DeleteMapping("/{symbol}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void deleteStock(
//            @PathVariable String symbol
//    ) {
//
//        stockService.deleteStock(symbol);
//    }


    /*
     * ================================================
     * ENTITY → RESPONSE
     * ================================================
     */

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