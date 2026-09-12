package com.codealpha.stockly.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PortfolioHistoryResponse {

    private final BigDecimal value;
    private final LocalDateTime timestamp;

    public PortfolioHistoryResponse(
            BigDecimal value,
            LocalDateTime timestamp
    ) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}