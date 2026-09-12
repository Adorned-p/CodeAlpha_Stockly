package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.MarketStatusResponse;
import com.codealpha.stockly.service.MarketTradingCalendar;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/market-status")
public class MarketStatusController {

    private final MarketTradingCalendar marketTradingCalendar;

    public MarketStatusController(
            MarketTradingCalendar marketTradingCalendar
    ) {
        this.marketTradingCalendar =
                marketTradingCalendar;
    }

    @GetMapping("/{exchange}")
    public MarketStatusResponse getMarketStatus(
            @PathVariable String exchange
    ) {

        String normalizedExchange =
                exchange.trim().toUpperCase();

        ZoneId zone;
        LocalTime openTime;
        LocalTime closeTime;

        switch (normalizedExchange) {

            case "NASDAQ":
            case "NYSE":

                zone =
                        ZoneId.of(
                                "America/New_York"
                        );

                openTime =
                        LocalTime.of(9, 30);

                closeTime =
                        LocalTime.of(16, 0);

                break;

            case "NSE":
            case "BSE":

                zone =
                        ZoneId.of(
                                "Asia/Kolkata"
                        );

                openTime =
                        LocalTime.of(9, 15);

                closeTime =
                        LocalTime.of(15, 30);

                break;

            default:

                throw new IllegalArgumentException(
                        "Unsupported exchange: "
                                + normalizedExchange
                );
        }

        ZonedDateTime now =
                ZonedDateTime.now(zone);

        boolean open =
                marketTradingCalendar.isMarketOpen(
                        normalizedExchange
                );

        return new MarketStatusResponse(
                normalizedExchange,
                open,
                zone.getId(),
                now.toLocalTime().toString(),
                openTime.toString(),
                closeTime.toString()
        );
    }
}