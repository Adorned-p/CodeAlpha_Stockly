package com.codealpha.stockly.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class BuyRequest {

    @NotBlank(message = "Stock symbol is required")
    private String symbol;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public BuyRequest() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}