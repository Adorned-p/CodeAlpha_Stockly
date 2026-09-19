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
import java.util.ArrayList;
import java.util.List;

@Service
public class InstrumentService {

    private final EodhdMarketDataProvider eodhdMarketDataProvider;
    private final ExternalMarketDataClient externalMarketDataClient;
    private final MarketDataProviderService marketDataProviderService;
    private final InstrumentRepository instrumentRepository;
    private final StockRepository stockRepository;

    public InstrumentService(
            InstrumentRepository instrumentRepository,
            StockRepository stockRepository,
            ExternalMarketDataClient externalMarketDataClient,
            MarketDataProviderService marketDataProviderService,
            EodhdMarketDataProvider eodhdMarketDataProvider
    ) {
        this.instrumentRepository = instrumentRepository;
        this.stockRepository = stockRepository;
        this.externalMarketDataClient = externalMarketDataClient;
        this.marketDataProviderService = marketDataProviderService;
        this.eodhdMarketDataProvider = eodhdMarketDataProvider;
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
                .findBySymbol(symbol.toUpperCase())
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

        if (instrumentRepository.existsBySymbolAndExchange(
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

        return instrumentRepository.save(instrument);
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

        return instrumentRepository.save(instrument);
    }

    // =========================================================
    // SEARCH INSTRUMENTS
    // =========================================================

    public List<InstrumentSearchResponse> searchInstruments(
            String query
    ) {

        if (query == null ||
                query.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Search query cannot be empty"
            );
        }

        String normalizedQuery =
                query.trim();

        List<InstrumentSearchResponse> results =
                new ArrayList<>();

        // =====================================================
        // EODHD SEARCH
        // =====================================================

        try {

            List<InstrumentSearchResponse> eodhdResults =
                    eodhdMarketDataProvider.searchSymbols(
                            normalizedQuery
                    );

            if (eodhdResults != null) {
                results.addAll(eodhdResults);
            }

        } catch (Exception exception) {

            System.err.println(
                    "EODHD search unavailable for '"
                            + normalizedQuery
                            + "': "
                            + exception.getMessage()
            );
        }

        // =====================================================
        // TWELVE DATA SEARCH
        // =====================================================

        try {

            ExternalSymbolSearchResponse response =
                    externalMarketDataClient.searchSymbols(
                            normalizedQuery
                    );

            if (response != null &&
                    response.getData() != null) {

                results.addAll(
                        response.getData()
                                .stream()
                                .map(result ->
                                        new InstrumentSearchResponse(
                                                result.getSymbol(),
                                                result.getInstrument_name(),
                                                result.getExchange(),
                                                result.getCountry(),
                                                result.getCurrency(),
                                                result.getInstrument_type(),
                                                "TWELVE_DATA",
                                                result.getSymbol()
                                        )
                                )
                                .toList()
                );
            }

        } catch (Exception exception) {

            System.err.println(
                    "Twelve Data search unavailable for '"
                            + normalizedQuery
                            + "': "
                            + exception.getMessage()
            );
        }

        // =====================================================
        // FILTER + DEDUPLICATE
        // =====================================================

        return results
                .stream()

                .filter(this::isSupportedEquity)

                .filter(result ->
                        result.getSymbol() != null
                                && !result.getSymbol().isBlank()
                                && result.getExchange() != null
                                && !result.getExchange().isBlank()
                )

                .sorted(
                        java.util.Comparator
                                .comparingInt(
                                        (InstrumentSearchResponse result) ->
                                                searchPriority(
                                                        result,
                                                        normalizedQuery
                                                )
                                )
                                .thenComparingInt(result ->
                                        result.getName() == null
                                                ? Integer.MAX_VALUE
                                                : result.getName().length()
                                )
                )

                .collect(
                        java.util.stream.Collectors.toMap(

                                result ->
                                        result.getSymbol()
                                                .trim()
                                                .toUpperCase()
                                                + "|"
                                                + result.getExchange()
                                                .trim()
                                                .toUpperCase(),

                                result -> result,

                                /*
                                 * If EODHD and Twelve Data
                                 * return the same instrument,
                                 * keep the first result.
                                 */
                                (existing, duplicate) ->
                                        existing,

                                java.util.LinkedHashMap::new
                        )
                )

                .values()
                .stream()
                .limit(20)
                .toList();
    }

    // =========================================================
    // SEARCH PRIORITY
    // =========================================================

    private int searchPriority(
            InstrumentSearchResponse result,
            String query
    ) {

        String normalizedQuery =
                query.trim().toLowerCase();

        String symbol =
                result.getSymbol() == null
                        ? ""
                        : result.getSymbol()
                        .trim()
                        .toLowerCase();

        String name =
                result.getName() == null
                        ? ""
                        : result.getName()
                        .trim()
                        .toLowerCase();

        String type =
                result.getAssetType() == null
                        ? ""
                        : result.getAssetType()
                        .trim()
                        .toLowerCase();

        boolean commonStock =
                type.equals("common stock");

        boolean depositaryReceipt =
                type.contains("depositary")
                        || name.contains("adr");

        // 1. Exact symbol match
        if (symbol.equals(normalizedQuery)) {
            return 0;
        }

        // 2. Exact company-name match
        if (name.equals(normalizedQuery)) {
            return 1;
        }

        // 3. Name starts with search query
        if (name.startsWith(normalizedQuery + " ")) {

            if (commonStock && !depositaryReceipt) {
                return 2;
            }

            if (commonStock) {
                return 3;
            }

            if (depositaryReceipt) {
                return 4;
            }

            return 5;
        }

        // 4. Query appears somewhere inside name
        if (name.contains(normalizedQuery)) {

            if (commonStock && !depositaryReceipt) {
                return 6;
            }

            if (commonStock) {
                return 7;
            }

            if (depositaryReceipt) {
                return 8;
            }
        }

        // 5. Everything else
        return 10;
    }

    // =========================================================
    // SUPPORTED EQUITY CHECK
    // =========================================================

    private boolean isSupportedEquity(
            InstrumentSearchResponse result
    ) {

        if (result == null) {
            return false;
        }

        String symbol =
                result.getSymbol();

        String name =
                result.getName();

        String assetType =
                result.getAssetType();

        if (symbol == null ||
                symbol.isBlank()) {
            return false;
        }

        if (name == null ||
                name.isBlank()) {
            return false;
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedName =
                name.trim().toLowerCase();

        String normalizedType =
                assetType == null
                        ? ""
                        : assetType.trim().toLowerCase();

        // =====================================================
        // REMOVE ETFs
        // =====================================================

        if (normalizedType.contains("etf")) {
            return false;
        }

        // =====================================================
        // REMOVE BONDS / NOTES / DEBT
        // =====================================================

        if (normalizedName.contains("senior note")
                || normalizedName.contains("senior notes")
                || normalizedName.contains("unsecured note")
                || normalizedName.contains("unsecured notes")
                || normalizedName.contains("bond")
                || normalizedName.contains("debenture")
                || normalizedName.contains("medium-term note")
                || normalizedName.contains("medium term note")) {

            return false;
        }

        // =====================================================
        // REMOVE OBVIOUS DEBT IDENTIFIERS
        // =====================================================

        if (normalizedSymbol.length() >= 10
                && normalizedSymbol.matches(
                "[A-Z]{2}[A-Z0-9]{9,}"
        )) {

            return false;
        }

        // =====================================================
        // ALLOW EQUITY TYPES
        // =====================================================

        return normalizedType.equals("common stock")
                || normalizedType.equals(
                "american depositary receipt"
        )
                || normalizedType.equals(
                "depositary receipt"
        );
    }

    // =========================================================
// CREATE OR UPDATE FROM SEARCH RESULT
// =========================================================

    @Transactional
    public Instrument createFromSearch(
            String symbol,
            String name,
            String assetType,
            String exchange,
            String country,
            String currency,
            String provider,
            String providerSymbol
    ) {

        // =====================================================
        // VALIDATION
        // =====================================================

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

        if (provider == null ||
                provider.isBlank()) {

            throw new IllegalArgumentException(
                    "Market data provider is required"
            );
        }

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Provider symbol is required"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        String normalizedExchange =
                exchange.trim().toUpperCase();

        String normalizedProvider =
                provider.trim().toUpperCase();

        String normalizedProviderSymbol =
                providerSymbol.trim().toUpperCase();

        // =====================================================
        // VALIDATE PROVIDER
        // =====================================================

        if (!normalizedProvider.equals("EODHD")
                && !normalizedProvider.equals("TWELVE_DATA")
                && !normalizedProvider.equals("ALPHA_VANTAGE")) {

            throw new IllegalArgumentException(
                    "Unsupported market-data provider: "
                            + provider
            );
        }

        // =====================================================
        // CHECK IF INSTRUMENT ALREADY EXISTS
        // =====================================================

        Instrument existingInstrument =
                instrumentRepository
                        .findBySymbolAndExchange(
                                normalizedSymbol,
                                normalizedExchange
                        )
                        .orElse(null);

        // =====================================================
        // EXISTING INSTRUMENT
        // =====================================================

        if (existingInstrument != null) {

            System.out.println(
                    "Instrument already exists: "
                            + normalizedSymbol
                            + " ("
                            + normalizedExchange
                            + ")"
            );

            System.out.println(
                    "Updating provider configuration..."
            );

            // -------------------------------------------------
            // Update basic information if supplied
            // -------------------------------------------------

            existingInstrument.setName(
                    name.trim()
            );

            existingInstrument.setAssetType(
                    assetType == null ||
                            assetType.isBlank()
                            ? existingInstrument.getAssetType()
                            : assetType.trim()
            );

            if (country != null &&
                    !country.isBlank()) {

                existingInstrument.setCountry(
                        country.trim()
                );
            }

            if (currency != null &&
                    !currency.isBlank()) {

                existingInstrument.setCurrency(
                        currency.trim().toUpperCase()
                );
            }

            // -------------------------------------------------
            // Set selected provider
            // -------------------------------------------------

            existingInstrument.setMarketDataProvider(
                    normalizedProvider
            );

            // -------------------------------------------------
            // Store provider-specific symbol
            // -------------------------------------------------

            switch (normalizedProvider) {

                case "EODHD":

                    existingInstrument.setEodhdSymbol(
                            normalizedProviderSymbol
                    );

                    break;

                case "TWELVE_DATA":

                    existingInstrument.setTwelveDataSymbol(
                            normalizedProviderSymbol
                    );

                    break;

                case "ALPHA_VANTAGE":

                    existingInstrument.setAlphaVantageSymbol(
                            normalizedProviderSymbol
                    );

                    break;

                default:

                    throw new IllegalArgumentException(
                            "Unsupported market-data provider: "
                                    + provider
                    );
            }

            Instrument savedInstrument =
                    instrumentRepository.save(
                            existingInstrument
                    );

            System.out.println(
                    "Provider configuration updated: "
                            + normalizedSymbol
                            + " -> "
                            + normalizedProvider
                            + " / "
                            + normalizedProviderSymbol
            );

            return savedInstrument;
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

        // =====================================================
        // EXISTING STOCK
        // =====================================================

        if (stock != null) {

            /*
             * Current Stock schema uses symbol as
             * globally unique.
             */
            if (stock.getExchange() == null ||
                    !stock.getExchange()
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
            // TEMPORARY INSTRUMENT FOR PROVIDER LOOKUP
            // =================================================

            Instrument providerInstrument =
                    new Instrument();

            providerInstrument.setSymbol(
                    normalizedSymbol
            );

            providerInstrument.setExchange(
                    normalizedExchange
            );

            providerInstrument.setMarketDataProvider(
                    normalizedProvider
            );

            switch (normalizedProvider) {

                case "EODHD":

                    providerInstrument.setEodhdSymbol(
                            normalizedProviderSymbol
                    );

                    break;

                case "TWELVE_DATA":

                    providerInstrument.setTwelveDataSymbol(
                            normalizedProviderSymbol
                    );

                    break;

                case "ALPHA_VANTAGE":

                    providerInstrument.setAlphaVantageSymbol(
                            normalizedProviderSymbol
                    );

                    break;

                default:

                    throw new IllegalArgumentException(
                            "Unsupported market-data provider: "
                                    + provider
                    );
            }

            // =================================================
            // GET INITIAL MARKET PRICE
            // =================================================

            ExternalQuoteResponse quote =
                    marketDataProviderService.getQuote(
                            providerInstrument
                    );

            if (quote == null ||
                    quote.getClose() == null ||
                    quote.getClose()
                            .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Could not obtain market price for "
                                + normalizedSymbol
                                + " from the selected market data provider."
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
        // CREATE NEW INSTRUMENT
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

        if (currency == null ||
                currency.isBlank()) {

            throw new IllegalArgumentException(
                    "Currency is required for instrument: "
                            + normalizedSymbol
            );
        }

        instrument.setCurrency(
                currency.trim().toUpperCase()
        );

        instrument.setCurrentPrice(
                stock.getCurrentPrice()
        );

        instrument.setActive(true);

        instrument.setStock(
                stock
        );

        // =====================================================
        // SAVE SELECTED PROVIDER
        // =====================================================

        instrument.setMarketDataProvider(
                normalizedProvider
        );

        // =====================================================
        // SAVE PROVIDER-SPECIFIC SYMBOL
        // =====================================================

        switch (normalizedProvider) {

            case "EODHD":

                instrument.setEodhdSymbol(
                        normalizedProviderSymbol
                );

                break;

            case "TWELVE_DATA":

                instrument.setTwelveDataSymbol(
                        normalizedProviderSymbol
                );

                break;

            case "ALPHA_VANTAGE":

                instrument.setAlphaVantageSymbol(
                        normalizedProviderSymbol
                );

                break;

            default:

                throw new IllegalArgumentException(
                        "Unsupported market-data provider: "
                                + provider
                );
        }

        Instrument savedInstrument =
                instrumentRepository.save(
                        instrument
                );

        System.out.println(
                "New instrument created: "
                        + normalizedSymbol
                        + " ("
                        + normalizedExchange
                        + ") using "
                        + normalizedProvider
                        + " / "
                        + normalizedProviderSymbol
        );

        return savedInstrument;
    }
}