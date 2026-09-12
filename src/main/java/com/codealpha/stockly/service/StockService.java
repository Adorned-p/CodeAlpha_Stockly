package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.entity.StockStatus;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import com.codealpha.stockly.repository.StockRepository;
import com.codealpha.stockly.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceHistoryRepository priceHistoryRepository;

    public StockService(
            StockRepository stockRepository,
            StockPriceHistoryRepository priceHistoryRepository
    ) {
        this.stockRepository = stockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    // =========================================================
    // UPDATE PRICE
    // =========================================================

    @Transactional
    public Stock updatePrice(
            String symbol,
            BigDecimal price
    ) {

        Stock stock =
                getStockBySymbol(symbol);

        if (price == null ||
                price.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Stock price must be greater than zero"
            );
        }

        // Update current price
        stock.setCurrentPrice(price);

        // Update day high
        if (stock.getDayHigh() == null ||
                price.compareTo(stock.getDayHigh()) > 0) {

            stock.setDayHigh(price);
        }

        // Update day low
        if (stock.getDayLow() == null ||
                price.compareTo(stock.getDayLow()) < 0) {

            stock.setDayLow(price);
        }

        // Save updated stock
        Stock savedStock =
                stockRepository.save(stock);

        // Record price history
        StockPriceHistory history =
                new StockPriceHistory();

        history.setStock(savedStock);
        history.setPrice(price);
        history.setRecordedAt(
                LocalDateTime.now()
        );

        priceHistoryRepository.save(history);

        return savedStock;
    }

    // =========================================================
    // GET ALL STOCKS
    // =========================================================

    public List<Stock> getAllStocks() {

        return stockRepository.findAll();
    }

    // =========================================================
    // GET STOCK BY SYMBOL
    // =========================================================

    public Stock getStockBySymbol(
            String symbol
    ) {

        return stockRepository
                .findBySymbol(
                        symbol.toUpperCase()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found: " + symbol
                        )
                );
    }

    // =========================================================
    // CREATE STOCK
    // =========================================================

    @Transactional
    public Stock saveStock(
            Stock stock
    ) {

        String symbol =
                stock.getSymbol()
                        .toUpperCase();

        if (stockRepository
                .existsBySymbol(symbol)) {

            throw new IllegalArgumentException(
                    "Stock already exists: " + symbol
            );
        }

        stock.setSymbol(symbol);

        if (stock.getStatus() == null) {

            stock.setStatus(
                    StockStatus.ACTIVE
            );
        }

        Stock savedStock =
                stockRepository.save(stock);

        // Create initial price-history entry
        if (savedStock.getCurrentPrice() != null) {

            StockPriceHistory history =
                    new StockPriceHistory();

            history.setStock(savedStock);
            history.setPrice(
                    savedStock.getCurrentPrice()
            );
            history.setRecordedAt(
                    LocalDateTime.now()
            );

            priceHistoryRepository.save(history);
        }

        return savedStock;
    }

    // =========================================================
    // UPDATE STOCK
    // =========================================================

    @Transactional
    public Stock updateStock(
            String symbol,
            String companyName,
            BigDecimal currentPrice,
            String sector,
            String exchange,
            StockStatus status
    ) {

        Stock stock =
                getStockBySymbol(symbol);

        stock.setCompanyName(companyName);
        stock.setCurrentPrice(currentPrice);
        stock.setSector(sector);
        stock.setExchange(
                exchange.toUpperCase()
        );
        stock.setStatus(status);

        return stockRepository.save(stock);
    }
}