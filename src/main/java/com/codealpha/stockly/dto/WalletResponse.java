package com.codealpha.stockly.dto;

import java.math.BigDecimal;

public class WalletResponse {

    private BigDecimal virtualBalance;
    private BigDecimal reservedBalance;
    private BigDecimal availableBalance;

    public WalletResponse() {
    }

    public WalletResponse(
            BigDecimal virtualBalance,
            BigDecimal reservedBalance,
            BigDecimal availableBalance
    ) {
        this.virtualBalance = virtualBalance;
        this.reservedBalance = reservedBalance;
        this.availableBalance = availableBalance;
    }

    /*
     * Kept for backward compatibility with any existing code
     * that creates WalletResponse using only the balance.
     */
    public WalletResponse(BigDecimal virtualBalance) {
        this.virtualBalance = virtualBalance;
        this.reservedBalance = BigDecimal.ZERO;
        this.availableBalance = virtualBalance;
    }

    public BigDecimal getVirtualBalance() {
        return virtualBalance;
    }

    public BigDecimal getReservedBalance() {
        return reservedBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
}