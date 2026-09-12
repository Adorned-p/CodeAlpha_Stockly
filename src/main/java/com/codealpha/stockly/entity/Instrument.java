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
    private MarketDataStatus marketDataStatus = MarketDataStatus.UNAVAILABLE;

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
     * Provider-specific symbol used by Alpha Vantage.
     *
     * Examples:
     * TCS       -> TCS.BSE
     * RELIANCE  -> RELIANCE.BSE
     * AAPL      -> AAPL
     *
     * This is intentionally separate from Stockly's own symbol
     * because different market-data providers can use different
     * symbol formats.
     */
    @Column(name = "alpha_vantage_symbol", length = 50)
    private String alphaVantageSymbol;

    /*
     * Link to existing Stock entity.
     *
     * This allows our new trading engine to work with
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