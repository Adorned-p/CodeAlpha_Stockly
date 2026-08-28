package com.codealpha.stockly.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_price_history",
        indexes = {
                @Index(
                        name = "idx_price_history_stock_time",
                        columnList = "stock_id, recorded_at"
                )
        }
)
public class StockPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "stock_id",
            nullable = false
    )
    private Stock stock;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal price;

    @Column(
            name = "recorded_at",
            nullable = false
    )
    private LocalDateTime recordedAt;

    public StockPriceHistory() {
    }

    public Long getId() {
        return id;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(
            LocalDateTime recordedAt
    ) {
        this.recordedAt = recordedAt;
    }
}