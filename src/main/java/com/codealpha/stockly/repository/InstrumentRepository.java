package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository
        extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findBySymbolAndExchange(
            String symbol,
            String exchange
    );

    Optional<Instrument> findBySymbol(
            String symbol
    );

    List<Instrument> findByActiveTrue();

    boolean existsBySymbolAndExchange(
            String symbol,
            String exchange
    );

    List<Instrument> findByActiveTrueAndStockIsNotNull();
}