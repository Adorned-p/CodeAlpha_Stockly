package com.codealpha.stockly.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionResponse {

    private Long id;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public WalletTransactionResponse() {
    }

    public WalletTransactionResponse(
            Long id,
            String type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}