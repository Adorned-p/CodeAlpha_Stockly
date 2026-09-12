package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository
        extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    List<Stock> findBySectorIgnoreCase(String sector);

    List<Stock> findByExchangeIgnoreCase(String exchange);

    List<Stock> findByStatus(StockStatus status);

    List<Stock> findByCompanyNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(
            String companyName,
            String symbol
    );
}