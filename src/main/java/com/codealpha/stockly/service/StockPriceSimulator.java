package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import com.codealpha.stockly.entity.StockStatus;
import com.codealpha.stockly.repository.StockPriceHistoryRepository;
import com.codealpha.stockly.repository.StockRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class StockPriceSimulator {

    private final StockRepository stockRepository;
    private final StockPriceHistoryRepository
            priceHistoryRepository;

    private final Random random = new Random();

    public StockPriceSimulator(
            StockRepository stockRepository,
            StockPriceHistoryRepository priceHistoryRepository
    ) {
        this.stockRepository = stockRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional
    @Scheduled(fixedRate = 30000)
    public void updatePrices() {

        List<Stock> stocks =
                stockRepository.findAll();

        for (Stock stock : stocks) {

            initializeMarketData(stock);

            // Suspended stocks don't move
            if (stock.getStatus() != StockStatus.ACTIVE) {
                continue;
            }

            saveInitialPriceIfNeeded(stock);
            BigDecimal currentPrice =
                    stock.getCurrentPrice();

            // Random movement between -2% and +2%
            double percentageChange =
                    (random.nextDouble() * 4.0) - 2.0;

            BigDecimal multiplier =
                    BigDecimal.valueOf(
                            1 + percentageChange / 100
                    );

            BigDecimal newPrice =
                    currentPrice
                            .multiply(multiplier)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            if (newPrice.compareTo(stock.getDayHigh()) > 0) {
                stock.setDayHigh(newPrice);
            }

            if (newPrice.compareTo(stock.getDayLow()) < 0) {
                stock.setDayLow(newPrice);
            }

            if (newPrice.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {
                continue;
            }

            stock.setCurrentPrice(newPrice);

            if (newPrice.compareTo(stock.getDayHigh()) > 0) {
                stock.setDayHigh(newPrice);
            }

            if (newPrice.compareTo(stock.getDayLow()) < 0) {
                stock.setDayLow(newPrice);
            }

            stockRepository.save(stock);

            // Save price history
            StockPriceHistory history =
                    new StockPriceHistory();

            history.setStock(stock);
            history.setPrice(newPrice);
            history.setRecordedAt(
                    LocalDateTime.now()
            );

            priceHistoryRepository.save(history);

            System.out.println(
                    stock.getSymbol()
                            + " price updated: ₹"
                            + newPrice
            );
        }
    }

    private void saveInitialPriceIfNeeded(
            Stock stock
    ) {

        if (!priceHistoryRepository
                .findTop1ByStockOrderByRecordedAtDesc(stock)
                .isPresent()) {

            StockPriceHistory history =
                    new StockPriceHistory();

            history.setStock(stock);
            history.setPrice(
                    stock.getCurrentPrice()
            );
            history.setRecordedAt(
                    LocalDateTime.now()
            );

            priceHistoryRepository.save(history);
        }
    }

    private void initializeMarketData(Stock stock) {

        BigDecimal currentPrice =
                stock.getCurrentPrice();

        boolean changed = false;

        if (stock.getOpeningPrice() == null) {
            stock.setOpeningPrice(currentPrice);
            changed = true;
        }

        if (stock.getPreviousClose() == null) {
            stock.setPreviousClose(currentPrice);
            changed = true;
        }

        if (stock.getDayHigh() == null) {
            stock.setDayHigh(currentPrice);
            changed = true;
        }

        if (stock.getDayLow() == null) {
            stock.setDayLow(currentPrice);
            changed = true;
        }

        if (changed) {
            stockRepository.save(stock);
        }
    }
}