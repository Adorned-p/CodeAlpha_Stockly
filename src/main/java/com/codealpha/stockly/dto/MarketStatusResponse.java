package com.codealpha.stockly.dto;

public class MarketStatusResponse {

    private String exchange;
    private boolean open;
    private String timezone;
    private String marketTime;
    private String marketOpenTime;
    private String marketCloseTime;

    public MarketStatusResponse(
            String exchange,
            boolean open,
            String timezone,
            String marketTime,
            String marketOpenTime,
            String marketCloseTime
    ) {
        this.exchange = exchange;
        this.open = open;
        this.timezone = timezone;
        this.marketTime = marketTime;
        this.marketOpenTime = marketOpenTime;
        this.marketCloseTime = marketCloseTime;
    }

    public String getExchange() {
        return exchange;
    }

    public boolean isOpen() {
        return open;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getMarketTime() {
        return marketTime;
    }

    public String getMarketOpenTime() {
        return marketOpenTime;
    }

    public String getMarketCloseTime() {
        return marketCloseTime;
    }
}