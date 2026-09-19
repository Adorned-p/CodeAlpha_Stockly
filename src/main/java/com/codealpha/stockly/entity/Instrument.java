package com.codealpha.stockly.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "instruments",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"symbol", "exchange"}
                )
        }
)
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "market_data_updated_at")
    private LocalDateTime marketDataUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_data_status", nullable = false, length = 20)
    private MarketDataStatus marketDataStatus =
            MarketDataStatus.UNAVAILABLE;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 30)
    private String assetType;

    @Column(nullable = false, length = 30)
    private String exchange;

    @Column(nullable = false, length = 50)
    private String country;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private boolean active;

    /*
     * Primary market-data provider selected for this instrument.
     *
     * Example:
     *   marketDataProvider = EODHD
     *
     * This tells Stockly which provider should be preferred
     * when fetching market data for this instrument.
     */
    @Column(name = "market_data_provider", length = 30)
    private String marketDataProvider;

    /*
     * Provider-specific symbols.
     *
     * Stockly's symbol is provider-independent.
     * Each market-data provider may represent the same
     * instrument using a different symbol format.
     *
     * Examples:
     *
     * Stockly:
     *   symbol   = 7203
     *   exchange = JPX
     *
     * Provider mappings:
     *   EODHD          -> 7203.T
     *   Twelve Data    -> 7203
     *   Alpha Vantage  -> 7203.T
     */
    @Column(name = "eodhd_symbol", length = 100)
    private String eodhdSymbol;

    @Column(name = "twelve_data_symbol", length = 100)
    private String twelveDataSymbol;

    @Column(name = "alpha_vantage_symbol", length = 100)
    private String alphaVantageSymbol;

    /*
     * Link to existing Stock entity.
     *
     * This allows the trading engine to work with
     * the existing Stock/Holding/Portfolio system.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", unique = true)
    private Stock stock;

    public Instrument() {
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getMarketDataProvider() {
        return marketDataProvider;
    }

    public void setMarketDataProvider(String marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    public String getEodhdSymbol() {
        return eodhdSymbol;
    }

    public void setEodhdSymbol(String eodhdSymbol) {
        this.eodhdSymbol = eodhdSymbol;
    }

    public String getTwelveDataSymbol() {
        return twelveDataSymbol;
    }

    public void setTwelveDataSymbol(String twelveDataSymbol) {
        this.twelveDataSymbol = twelveDataSymbol;
    }

    public String getAlphaVantageSymbol() {
        return alphaVantageSymbol;
    }

    public void setAlphaVantageSymbol(String alphaVantageSymbol) {
        this.alphaVantageSymbol = alphaVantageSymbol;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public MarketDataStatus getMarketDataStatus() {
        return marketDataStatus;
    }

    public void setMarketDataStatus(MarketDataStatus marketDataStatus) {
        this.marketDataStatus = marketDataStatus;
    }

    public LocalDateTime getMarketDataUpdatedAt() {
        return marketDataUpdatedAt;
    }

    public void setMarketDataUpdatedAt(
            LocalDateTime marketDataUpdatedAt
    ) {
        this.marketDataUpdatedAt = marketDataUpdatedAt;
    }
}