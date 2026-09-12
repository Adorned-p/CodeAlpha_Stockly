package com.codealpha.stockly.service;


import com.codealpha.stockly.dto.ExternalQuoteResponse;
import com.codealpha.stockly.dto.ExternalSymbolSearchResponse;
import com.codealpha.stockly.dto.InstrumentSearchResponse;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.exception.ResourceNotFoundException;
import com.codealpha.stockly.repository.InstrumentRepository;
import com.codealpha.stockly.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
public class InstrumentService {

    private final ExternalMarketDataClient externalMarketDataClient;
    private final MarketDataProviderService marketDataProviderService;
    private final AlphaVantageSymbolResolver alphaVantageSymbolResolver;
    private final InstrumentRepository instrumentRepository;
    private final StockRepository stockRepository;

    public InstrumentService(
            InstrumentRepository instrumentRepository,
            StockRepository stockRepository,
            ExternalMarketDataClient externalMarketDataClient,
            MarketDataProviderService marketDataProviderService,
            AlphaVantageSymbolResolver alphaVantageSymbolResolver
    ) {
        this.instrumentRepository =
                instrumentRepository;

        this.stockRepository =
                stockRepository;

        this.externalMarketDataClient =
                externalMarketDataClient;

        this.marketDataProviderService =
                marketDataProviderService;

        this.alphaVantageSymbolResolver =
                alphaVantageSymbolResolver;
    }

    // =========================================================
    // GET ACTIVE INSTRUMENTS
    // =========================================================

    public List<Instrument> getAllInstruments() {

        return instrumentRepository.findByActiveTrue();
    }

    // =========================================================
    // GET BY SYMBOL
    // =========================================================

    public Instrument getBySymbol(String symbol) {

        return instrumentRepository
                .findBySymbol(
                        symbol.toUpperCase()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Instrument not found: " + symbol
                        )
                );
    }

    // =========================================================
    // CREATE FROM EXISTING STOCK
    // =========================================================

    @Transactional
    public Instrument createFromStock(
            Long stockId,
            String assetType,
            String country,
            String currency
    ) {

        Stock stock =
                stockRepository.findById(stockId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Stock not found: " + stockId
                                )
                        );

        String symbol =
                stock.getSymbol().toUpperCase();

        String exchange =
                stock.getExchange().toUpperCase();

        if (instrumentRepository
                .existsBySymbolAndExchange(
                        symbol,
                        exchange
                )) {

            throw new IllegalArgumentException(
                    "Instrument already exists: " + symbol
            );
        }

        Instrument instrument =
                new Instrument();

        instrument.setSymbol(symbol);

        instrument.setName(
                stock.getCompanyName()
        );

        instrument.setAssetType(assetType);

        instrument.setExchange(exchange);

        instrument.setCountry(country);

        instrument.setCurrency(currency);

        instrument.setCurrentPrice(
                stock.getCurrentPrice()
        );

        instrument.setActive(true);

        instrument.setStock(stock);

        /*
         * Resolve the Alpha Vantage provider symbol
         * once and store it.
         */
        String alphaVantageSymbol =
                resolveAlphaVantageSymbol(
                        symbol,
                        exchange,
                        country
                );

        instrument.setAlphaVantageSymbol(
                alphaVantageSymbol
        );

        return instrumentRepository.save(
                instrument
        );
    }

    // =========================================================
    // UPDATE PRICE
    // =========================================================

    @Transactional
    public Instrument updatePrice(
            String symbol,
            BigDecimal price
    ) {

        if (price == null ||
                price.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Price must be greater than 0"
            );
        }

        Instrument instrument =
                getBySymbol(symbol);

        instrument.setCurrentPrice(price);


        /*
         * Keep the existing Stock price synchronized
         * with Instrument.
         */
        if (instrument.getStock() != null) {

            instrument.getStock()
                    .setCurrentPrice(price);

            stockRepository.save(
                    instrument.getStock()
            );
        }

        return instrumentRepository.save(
                instrument
        );
    }

    // =========================================================
    // SEARCH INSTRUMENTS
    // =========================================================

    /*
     * Search still uses Twelve Data's existing search DTO.
     *
     * We will make this multi-provider in Stage 2B,
     * because Alpha Vantage returns a different search
     * response structure.
     */
    public List<InstrumentSearchResponse> searchInstruments(
            String query
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Search query cannot be empty"
            );
        }

        ExternalSymbolSearchResponse response =
                externalMarketDataClient.searchSymbols(
                        query.trim()
                );

        if (response == null ||
                response.getData() == null) {

            return Collections.emptyList();
        }

        return response.getData()
                .stream()
                .map(result ->
                        new InstrumentSearchResponse(
                                result.getSymbol(),
                                result.getInstrument_name(),
                                result.getExchange(),
                                result.getCountry(),
                                result.getCurrency(),
                                result.getInstrument_type()
                        )
                )
                .toList();
    }

    // =========================================================
    // ALPHA VANTAGE SYMBOL RESOLUTION
    // =========================================================

    private String resolveAlphaVantageSymbol(
            String symbol,
            String exchange,
            String country
    ) {

        return alphaVantageSymbolResolver.resolve(
                symbol,
                exchange,
                country
        );
    }

    // =========================================================
    // CREATE FROM SEARCH RESULT
    // =========================================================

    @Transactional
    public Instrument createFromSearch(
            String symbol,
            String name,
            String assetType,
            String exchange,
            String country,
            String currency
    ) {

        if (symbol == null ||
                symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol is required"
            );
        }

        if (name == null ||
                name.isBlank()) {

            throw new IllegalArgumentException(
                    "Name is required"
            );
        }

        if (exchange == null ||
                exchange.isBlank()) {

            throw new IllegalArgumentException(
                    "Exchange is required"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedExchange =
                exchange.trim().toUpperCase();

        // =====================================================
        // DUPLICATE INSTRUMENT CHECK
        // =====================================================

        if (instrumentRepository
                .existsBySymbolAndExchange(
                        normalizedSymbol,
                        normalizedExchange
                )) {

            throw new IllegalArgumentException(
                    "Instrument already exists: "
                            + normalizedSymbol
                            + " ("
                            + normalizedExchange
                            + ")"
            );
        }

        // =====================================================
        // FIND EXISTING STOCK
        // =====================================================

        Stock stock =
                stockRepository
                        .findBySymbol(
                                normalizedSymbol
                        )
                        .orElse(null);

        if (stock != null) {

            /*
             * Our current Stock schema uses symbol as
             * globally unique.
             */
            if (!stock.getExchange()
                    .equalsIgnoreCase(
                            normalizedExchange
                    )) {

                throw new IllegalArgumentException(
                        "Stock "
                                + normalizedSymbol
                                + " already exists on exchange "
                                + stock.getExchange()
                                + ". "
                                + "The current Stock schema does not "
                                + "allow the same symbol on multiple exchanges."
                );
            }

        } else {

            // =================================================
            // GET INITIAL MARKET PRICE
            // =================================================

            /*
             * IMPORTANT:
             *
             * Do NOT call ExternalMarketDataClient directly.
             *
             * The provider coordinator decides:
             *
             * Twelve Data
             *       ↓
             * Alpha Vantage fallback
             */
            ExternalQuoteResponse quote =
                    marketDataProviderService.getQuote(
                            normalizedSymbol,
                            normalizedExchange
                    );

            if (quote == null ||
                    quote.getClose() == null ||
                    quote.getClose()
                            .compareTo(
                                    BigDecimal.ZERO
                            ) <= 0) {

                throw new IllegalArgumentException(
                        "Could not obtain market price for "
                                + normalizedSymbol
                                + " from the available market data providers."
                );
            }

            BigDecimal currentPrice =
                    quote.getClose();

            BigDecimal openingPrice =
                    quote.getOpen() != null
                            ? quote.getOpen()
                            : currentPrice;

            BigDecimal previousClose =
                    quote.getPreviousClose() != null
                            ? quote.getPreviousClose()
                            : currentPrice;

            BigDecimal dayHigh =
                    quote.getHigh() != null
                            ? quote.getHigh()
                            : currentPrice;

            BigDecimal dayLow =
                    quote.getLow() != null
                            ? quote.getLow()
                            : currentPrice;

            // =================================================
            // CREATE STOCK
            // =================================================

            stock =
                    new Stock();

            stock.setSymbol(
                    normalizedSymbol
            );

            stock.setCompanyName(
                    name.trim()
            );

            stock.setCurrentPrice(
                    currentPrice
            );

            stock.setOpeningPrice(
                    openingPrice
            );

            stock.setPreviousClose(
                    previousClose
            );

            stock.setDayHigh(
                    dayHigh
            );

            stock.setDayLow(
                    dayLow
            );

            /*
             * Current search DTO does not provide sector.
             *
             * Do not invent one.
             */
            stock.setSector(
                    "Unknown"
            );

            stock.setExchange(
                    normalizedExchange
            );

            stock.setStatus(
                    com.codealpha.stockly.entity.StockStatus.ACTIVE
            );

            stock =
                    stockRepository.save(
                            stock
                    );
        }

        // =====================================================
        // CREATE INSTRUMENT
        // =====================================================

        Instrument instrument =
                new Instrument();

        instrument.setSymbol(
                normalizedSymbol
        );

        instrument.setName(
                name.trim()
        );

        instrument.setAssetType(
                assetType == null ||
                        assetType.isBlank()
                        ? "Common Stock"
                        : assetType.trim()
        );

        instrument.setExchange(
                normalizedExchange
        );

        instrument.setCountry(
                country == null ||
                        country.isBlank()
                        ? "Unknown"
                        : country.trim()
        );

        instrument.setCurrency(
                currency == null ||
                        currency.isBlank()
                        ? "USD"
                        : currency.trim()
        );

        instrument.setCurrentPrice(
                stock.getCurrentPrice()
        );

        instrument.setActive(true);

        instrument.setStock(
                stock
        );

        // =====================================================
        // RESOLVE ALPHA VANTAGE SYMBOL
        // =====================================================

        String alphaVantageSymbol =
                alphaVantageSymbolResolver.resolve(
                        normalizedSymbol,
                        normalizedExchange,
                        country
                );

        instrument.setAlphaVantageSymbol(
                alphaVantageSymbol
        );

        return instrumentRepository.save(
                instrument
        );
    }
}