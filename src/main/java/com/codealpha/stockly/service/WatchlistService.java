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

    public WatchlistService(
            UserRepository userRepository,
            StockRepository stockRepository,
            WatchlistRepository watchlistRepository
    ) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.watchlistRepository = watchlistRepository;
    }

    public List<WatchlistResponse> getWatchlist(
            String userEmail
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        return watchlistRepository.findByUser(user)
                .stream()
                .map(item -> toResponse(item.getStock()))
                .toList();
    }

    @Transactional
    public WatchlistResponse addToWatchlist(
            String userEmail,
            String symbol
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        Stock stock = stockRepository.findBySymbol(
                        symbol.toUpperCase()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Stock not found: " + symbol
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

        Watchlist watchlist = new Watchlist();

        watchlist.setUser(user);
        watchlist.setStock(stock);

        watchlistRepository.save(watchlist);

        return toResponse(stock);
    }

    @Transactional
    public void removeFromWatchlist(
            String userEmail,
            String symbol
    ) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        Stock stock = stockRepository.findBySymbol(
                        symbol.toUpperCase()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Stock not found: " + symbol
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

    private WatchlistResponse toResponse(
            Stock stock
    ) {

        BigDecimal priceChange =
                stock.getCurrentPrice()
                        .subtract(stock.getPreviousClose());

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

        return new WatchlistResponse(
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getCurrentPrice(),
                priceChange,
                changePercentage,
                stock.getSector(),
                stock.getExchange()
        );
    }
}