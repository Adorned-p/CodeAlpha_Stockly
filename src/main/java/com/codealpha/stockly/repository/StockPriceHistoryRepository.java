package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockPriceHistoryRepository
        extends JpaRepository<StockPriceHistory, Long> {

    List<StockPriceHistory>
    findByStockAndRecordedAtBetweenOrderByRecordedAtAsc(
            Stock stock,
            LocalDateTime start,
            LocalDateTime end
    );

    List<StockPriceHistory>
    findByStockOrderByRecordedAtDesc(
            Stock stock
    );

    List<StockPriceHistory>
    findByStockOrderByRecordedAtDesc(
            Stock stock,
            Pageable pageable
    );

    Optional<StockPriceHistory>
    findTop1ByStockOrderByRecordedAtDesc(
            Stock stock
    );

    boolean existsByStockAndRecordedAt(
            Stock stock,
            LocalDateTime recordedAt
    );
}