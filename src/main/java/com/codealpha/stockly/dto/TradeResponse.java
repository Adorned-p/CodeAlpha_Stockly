package com.codealpha.stockly.dto;

import com.codealpha.stockly.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeResponse {

    private Long transactionId;
    private String symbol;
    private TransactionType type;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private BigDecimal remainingBalance;
    private LocalDateTime executedAt;

    public TradeResponse() {
    }

    public TradeResponse(
            Long transactionId,
            String symbol,
            TransactionType type,
            Integer quantity,
            BigDecimal price,
            BigDecimal totalAmount,
            BigDecimal remainingBalance,
            LocalDateTime executedAt
    ) {
        this.transactionId = transactionId;
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = totalAmount;
        this.remainingBalance = remainingBalance;
        this.executedAt = executedAt;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getSymbol() {
        return symbol;
    }

    public TransactionType getType() {
        return type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
}