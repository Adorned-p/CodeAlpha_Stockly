package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockStatus;
import com.codealpha.stockly.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional
    public Stock updatePrice(
            String symbol,
            BigDecimal price
    ) {

        Stock stock = getStockBySymbol(symbol);

        stock.setCurrentPrice(price);

        return stockRepository.save(stock);
    }

    public List<Stock> getAllStocks() {

        return stockRepository.findAll();
    }


    public Stock getStockBySymbol(String symbol) {

        return stockRepository
                .findBySymbol(symbol.toUpperCase())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Stock not found: " + symbol
                        )
                );
    }


    @Transactional
    public Stock saveStock(Stock stock) {

        String symbol =
                stock.getSymbol().toUpperCase();

        if (stockRepository.existsBySymbol(symbol)) {

            throw new IllegalArgumentException(
                    "Stock already exists: " + symbol
            );
        }

        stock.setSymbol(symbol);

        /*
         * New stocks are ACTIVE by default.
         */
        if (stock.getStatus() == null) {
            stock.setStatus(
                    StockStatus.ACTIVE
            );
        }

        return stockRepository.save(stock);
    }


    @Transactional
    public Stock updateStock(
            String symbol,
            String companyName,
            java.math.BigDecimal currentPrice,
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


//    @Transactional
//    public void deleteStock(
//            String symbol
//    ) {
//
//        Stock stock =
//                getStockBySymbol(symbol);
//
//        stockRepository.delete(stock);
//    }
}