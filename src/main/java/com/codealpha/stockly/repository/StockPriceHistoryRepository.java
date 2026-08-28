package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface StockPriceHistoryRepository
        extends JpaRepository<StockPriceHistory, Long> {

    List<StockPriceHistory> findByStockAndRecordedAtBetweenOrderByRecordedAtAsc(
            Stock stock,
            LocalDateTime start,
            LocalDateTime end
    );

    List<StockPriceHistory>
    findTop100ByStockOrderByRecordedAtDesc(
            Stock stock
    );

    Optional<StockPriceHistory>
    findTop1ByStockOrderByRecordedAtDesc(
            Stock stock
    );
}