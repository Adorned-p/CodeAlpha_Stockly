package com.codealpha.stockly.dto;

import com.codealpha.stockly.entity.StockStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class StockUpdateRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotNull(message = "Current price is required")
    @DecimalMin(
            value = "0.01",
            message = "Current price must be greater than 0"
    )
    private BigDecimal currentPrice;

    @NotBlank(message = "Sector is required")
    private String sector;

    @NotBlank(message = "Exchange is required")
    private String exchange;

    @NotNull(message = "Status is required")
    private StockStatus status;

    public StockUpdateRequest() {
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public StockStatus getStatus() {
        return status;
    }

    public void setStatus(StockStatus status) {
        this.status = status;
    }
}