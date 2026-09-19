package com.codealpha.stockly.dto;

import com.codealpha.stockly.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String symbol;
    private String exchange;
    private String companyName;
    private TransactionType type;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private LocalDateTime executedAt;

    public TransactionResponse() {
    }

    public TransactionResponse(
            Long id,
            String symbol,
            String exchange,
            String companyName,
            TransactionType type,
            Integer quantity,
            BigDecimal price,
            BigDecimal totalAmount,
            LocalDateTime executedAt
    ) {
        this.id = id;
        this.symbol = symbol;
        this.exchange = exchange;
        this.companyName = companyName;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = totalAmount;
        this.executedAt = executedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getExchange() {
        return exchange;
    }

    public String getCompanyName() {
        return companyName;
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

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
}