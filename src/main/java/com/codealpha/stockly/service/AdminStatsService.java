package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.AdminStatsResponse;
import com.codealpha.stockly.repository.StockRepository;
import com.codealpha.stockly.repository.TransactionRepository;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;

    public AdminStatsService(
            UserRepository userRepository,
            StockRepository stockRepository,
            TransactionRepository transactionRepository
    ) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.transactionRepository = transactionRepository;
    }

    public AdminStatsResponse getStats() {

        long totalUsers =
                userRepository.count();

        long totalStocks =
                stockRepository.count();

        long totalTrades =
                transactionRepository.count();

        BigDecimal totalTradingVolume =
                transactionRepository.getTotalTradingVolume();

        if (totalTradingVolume == null) {
            totalTradingVolume = BigDecimal.ZERO;
        }

        return new AdminStatsResponse(
                totalUsers,
                totalStocks,
                totalTrades,
                totalTradingVolume
        );
    }
}