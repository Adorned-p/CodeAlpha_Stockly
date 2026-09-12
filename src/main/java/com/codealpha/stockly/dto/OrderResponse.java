package com.codealpha.stockly.dto;

import com.codealpha.stockly.entity.OrderSide;
import com.codealpha.stockly.entity.OrderStatus;
import com.codealpha.stockly.entity.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderResponse {

    private Long id;
    private String symbol;
    private String instrumentName;
    private OrderSide side;
    private OrderType type;
    private OrderStatus status;
    private Integer quantity;
    private Integer filledQuantity;
    private BigDecimal limitPrice;
    private BigDecimal stopPrice;
    private BigDecimal averageFillPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderResponse() {
    }

    public OrderResponse(
            Long id,
            String symbol,
            String instrumentName,
            OrderSide side,
            OrderType type,
            OrderStatus status,
            Integer quantity,
            Integer filledQuantity,
            BigDecimal limitPrice,
            BigDecimal stopPrice,
            BigDecimal averageFillPrice,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.symbol = symbol;
        this.instrumentName = instrumentName;
        this.side = side;
        this.type = type;
        this.status = status;
        this.quantity = quantity;
        this.filledQuantity = filledQuantity;
        this.limitPrice = limitPrice;
        this.stopPrice = stopPrice;
        this.averageFillPrice = averageFillPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderType getType() {
        return type;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getFilledQuantity() {
        return filledQuantity;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public BigDecimal getAverageFillPrice() {
        return averageFillPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}