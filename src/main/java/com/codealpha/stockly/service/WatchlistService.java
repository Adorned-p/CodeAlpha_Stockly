package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.WatchlistResponse;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.entity.Watchlist;
import com.codealpha.stockly.repository.StockRepository;
import com.codealpha.stockly.repository.UserRepository;
import com.codealpha.stockly.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class WatchlistService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final WatchlistRepository watchlistRepository;
    private final CurrencyConversionService currencyConversionService;

    public WatchlistService(
            UserRepository userRepository,
            StockRepository stockRepository,
            WatchlistRepository watchlistRepository,
            CurrencyConversionService currencyConversionService
    ) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.watchlistRepository = watchlistRepository;
        this.currencyConversionService = currencyConversionService;
    }

    // =========================================================
    // GET WATCHLIST
    // =========================================================

    public List<WatchlistResponse> getWatchlist(
            String userEmail
    ) {

        User user =
                userRepository.findByEmail(userEmail)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );

        return watchlistRepository
                .findByUser(user)
                .stream()
                .map(item ->
                        toResponse(item.getStock())
                )
                .toList();
    }

    // =========================================================
    // ADD TO WATCHLIST
    // =========================================================

    @Transactional
    public WatchlistResponse addToWatchlist(
            String userEmail,
            String symbol,
            String exchange
    ) {

        User user =
                userRepository.findByEmail(userEmail)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );

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

        Stock stock =
                stockRepository
                        .findBySymbolAndExchange(
                                normalizedSymbol,
                                normalizedExchange
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Stock not found: "
                                                + normalizedSymbol
                                                + " on "
                                                + normalizedExchange
                                )
                        );

        if (watchlistRepository.existsByUserAndStock(
                user,
                stock
        )) {

            throw new IllegalArgumentException(
                    "Stock is already in your watchlist"
            );
        }

        Watchlist watchlist =
                new Watchlist();

        watchlist.setUser(user);
        watchlist.setStock(stock);

        watchlistRepository.save(watchlist);

        return toResponse(stock);
    }

    // =========================================================
    // REMOVE FROM WATCHLIST
    // =========================================================

    @Transactional
    public void removeFromWatchlist(
            String userEmail,
            String symbol,
            String exchange
    ) {

        User user =
                userRepository.findByEmail(userEmail)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );

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

        Stock stock =
                stockRepository
                        .findBySymbolAndExchange(
                                normalizedSymbol,
                                normalizedExchange
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Stock not found: "
                                                + normalizedSymbol
                                                + " on "
                                                + normalizedExchange
                                )
                        );

        if (!watchlistRepository.existsByUserAndStock(
                user,
                stock
        )) {

            throw new IllegalArgumentException(
                    "Stock is not in your watchlist"
            );
        }

        watchlistRepository.deleteByUserAndStock(
                user,
                stock
        );
    }

    // =========================================================
    // ENTITY → RESPONSE
    // =========================================================

    private WatchlistResponse toResponse(
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

        return new WatchlistResponse(
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getCurrentPrice(),
                currentPriceInr,
                exchangeRateToInr,
                priceChange,
                changePercentage,
                stock.getSector(),
                stock.getExchange()
        );
    }

    // =========================================================
    // STOCK CURRENCY
    // =========================================================

    private String getStockCurrency(
            Stock stock
    ) {

        String exchange =
                stock.getExchange();

        if (exchange == null ||
                exchange.isBlank()) {

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