package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.PortfolioSnapshot;
import com.codealpha.stockly.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PortfolioSnapshotRepository
        extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot>
    findByUserAndRecordedAtBetweenOrderByRecordedAtAsc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );
}