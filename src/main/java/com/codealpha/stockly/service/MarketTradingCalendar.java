package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Instrument;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class MarketTradingCalendar {

    public boolean isMarketOpen(Instrument instrument) {

        if (instrument == null ||
                instrument.getExchange() == null) {

            return false;
        }

        return isMarketOpen(
                instrument.getExchange()
        );
    }

    public boolean isMarketOpen(String exchange) {

        if (exchange == null ||
                exchange.isBlank()) {

            return false;
        }

        String normalizedExchange =
                exchange.trim().toUpperCase();

        return switch (normalizedExchange) {

            case "NASDAQ", "NYSE" ->
                    isUsMarketOpen();

            case "NSE", "BSE" ->
                    isIndiaMarketOpen();

            default ->
                    false;
        };
    }

    private boolean isUsMarketOpen() {

        ZoneId zone =
                ZoneId.of("America/New_York");

        ZonedDateTime now =
                ZonedDateTime.now(zone);

        DayOfWeek day =
                now.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY ||
                day == DayOfWeek.SUNDAY) {

            return false;
        }

        LocalTime time =
                now.toLocalTime();

        return !time.isBefore(
                LocalTime.of(9, 30)
        )
                &&
                time.isBefore(
                        LocalTime.of(16, 0)
                );
    }

    private boolean isIndiaMarketOpen() {

        ZoneId zone =
                ZoneId.of("Asia/Kolkata");

        ZonedDateTime now =
                ZonedDateTime.now(zone);

        DayOfWeek day =
                now.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY ||
                day == DayOfWeek.SUNDAY) {

            return false;
        }

        LocalTime time =
                now.toLocalTime();

        return !time.isBefore(
                LocalTime.of(9, 15)
        )
                &&
                time.isBefore(
                        LocalTime.of(15, 30)
                );
    }
}