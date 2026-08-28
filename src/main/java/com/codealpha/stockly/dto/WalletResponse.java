package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public class WalletResponse {

    private BigDecimal virtualBalance;

    public WalletResponse() {
    }

    public WalletResponse(BigDecimal virtualBalance) {
        this.virtualBalance = virtualBalance;
    }

    public BigDecimal getVirtualBalance() {
        return virtualBalance;
    }
}