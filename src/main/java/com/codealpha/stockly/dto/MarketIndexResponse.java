package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public record MarketIndexResponse(
        String symbol,
        String name,
        BigDecimal value,
        BigDecimal change,
        BigDecimal changePercentage
) {
}