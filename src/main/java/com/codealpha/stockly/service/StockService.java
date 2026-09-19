package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.entity.StockStatus;
import com.codealpha.stockly.exception.ResourceNotFoundException;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import com.codealpha.stockly.repository.StockRepository;
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

        validatePrice(price);

        updateStockPriceFields(stock, price);

        Stock savedStock =
                stockRepository.save(stock);

        savePriceHistory(savedStock, price);

        return savedStock;
    }

    // =========================================================
    // UPDATE PRICE BY SYMBOL + EXCHANGE
    // =========================================================

    @Transactional
    public Stock updatePrice(
            String symbol,
            String exchange,
            BigDecimal price
    ) {

        Stock stock =
                getStockBySymbolAndExchange(
                        symbol,
                        exchange
                );

        validatePrice(price);

        updateStockPriceFields(stock, price);

        Stock savedStock =
                stockRepository.save(stock);

        savePriceHistory(savedStock, price);

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
    /*
     * Kept for backward compatibility with existing services.
     *
     * New exchange-aware code should use:
     * getStockBySymbolAndExchange(symbol, exchange)
     */

    public Stock getStockBySymbol(
            String symbol
    ) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Stock symbol is required"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        return stockRepository
                .findBySymbol(normalizedSymbol)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found: " + normalizedSymbol
                        )
                );
    }

    // =========================================================
    // GET STOCK BY SYMBOL + EXCHANGE
    // =========================================================

    public Stock getStockBySymbolAndExchange(
            String symbol,
            String exchange
    ) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Stock symbol is required"
            );
        }

        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException(
                    "Stock exchange is required"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedExchange =
                exchange.trim().toUpperCase();

        return stockRepository
                .findBySymbolAndExchange(
                        normalizedSymbol,
                        normalizedExchange
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Stock not found: "
                                        + normalizedSymbol
                                        + " on "
                                        + normalizedExchange
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

        if (stock == null) {
            throw new IllegalArgumentException(
                    "Stock is required"
            );
        }

        if (stock.getSymbol() == null ||
                stock.getSymbol().isBlank()) {

            throw new IllegalArgumentException(
                    "Stock symbol is required"
            );
        }

        if (stock.getExchange() == null ||
                stock.getExchange().isBlank()) {

            throw new IllegalArgumentException(
                    "Stock exchange is required"
            );
        }

        String symbol =
                stock.getSymbol()
                        .trim()
                        .toUpperCase();

        String exchange =
                stock.getExchange()
                        .trim()
                        .toUpperCase();

        if (stockRepository
                .existsBySymbolAndExchange(
                        symbol,
                        exchange
                )) {

            throw new IllegalArgumentException(
                    "Stock already exists: "
                            + symbol
                            + " on "
                            + exchange
            );
        }

        stock.setSymbol(symbol);
        stock.setExchange(exchange);

        if (stock.getStatus() == null) {

            stock.setStatus(
                    StockStatus.ACTIVE
            );
        }

        Stock savedStock =
                stockRepository.save(stock);

        // Create initial price-history entry
        if (savedStock.getCurrentPrice() != null) {

            savePriceHistory(
                    savedStock,
                    savedStock.getCurrentPrice()
            );
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

        if (exchange != null &&
                !exchange.isBlank()) {

            stock.setExchange(
                    exchange.trim().toUpperCase()
            );
        }

        stock.setStatus(status);

        return stockRepository.save(stock);
    }

    // =========================================================
    // UPDATE STOCK BY SYMBOL + EXCHANGE
    // =========================================================

    @Transactional
    public Stock updateStock(
            String symbol,
            String exchange,
            String companyName,
            BigDecimal currentPrice,
            String sector,
            StockStatus status
    ) {

        Stock stock =
                getStockBySymbolAndExchange(
                        symbol,
                        exchange
                );

        stock.setCompanyName(companyName);
        stock.setCurrentPrice(currentPrice);
        stock.setSector(sector);
        stock.setStatus(status);

        return stockRepository.save(stock);
    }

    // =========================================================
    // VALIDATE PRICE
    // =========================================================

    private void validatePrice(
            BigDecimal price
    ) {

        if (price == null ||
                price.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Stock price must be greater than zero"
            );
        }
    }

    // =========================================================
    // UPDATE PRICE FIELDS
    // =========================================================

    private void updateStockPriceFields(
            Stock stock,
            BigDecimal price
    ) {

        stock.setCurrentPrice(price);

        if (stock.getDayHigh() == null ||
                price.compareTo(
                        stock.getDayHigh()
                ) > 0) {

            stock.setDayHigh(price);
        }

        if (stock.getDayLow() == null ||
                price.compareTo(
                        stock.getDayLow()
                ) < 0) {

            stock.setDayLow(price);
        }
    }

    // =========================================================
    // SAVE PRICE HISTORY
    // =========================================================

    private void savePriceHistory(
            Stock stock,
            BigDecimal price
    ) {

        StockPriceHistory history =
                new StockPriceHistory();

        history.setStock(stock);
        history.setPrice(price);
        history.setRecordedAt(
                LocalDateTime.now()
        );

        priceHistoryRepository.save(history);
    }
}