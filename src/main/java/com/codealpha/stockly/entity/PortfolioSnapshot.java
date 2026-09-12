package com.codealpha.stockly.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "portfolio_snapshots",
        indexes = {
                @Index(
                        name = "idx_portfolio_snapshot_user_time",
                        columnList = "user_id, recorded_at"
                )
        }
)
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal portfolioValue;

    @Column(
            name = "recorded_at",
            nullable = false
    )
    private LocalDateTime recordedAt;

    public PortfolioSnapshot() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public void setPortfolioValue(
            BigDecimal portfolioValue
    ) {
        this.portfolioValue = portfolioValue;
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