package com.codealpha.stockly.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockPriceHistoryResponse {

    private BigDecimal price;
    private LocalDateTime recordedAt;

    public StockPriceHistoryResponse() {
    }

    public StockPriceHistoryResponse(
            BigDecimal price,
            LocalDateTime recordedAt
    ) {
        this.price = price;
        this.recordedAt = recordedAt;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}