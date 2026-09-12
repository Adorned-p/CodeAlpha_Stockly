package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.MarketQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketQuoteRepository
        extends JpaRepository<MarketQuote, Long> {

    Optional<MarketQuote> findByInstrument(
            Instrument instrument
    );

    Optional<MarketQuote> findByInstrument_Symbol(
            String symbol
    );
}