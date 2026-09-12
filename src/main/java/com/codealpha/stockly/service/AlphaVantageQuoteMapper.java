package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.AlphaVantageQuoteResponse;
import com.codealpha.stockly.dto.ExternalQuoteResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AlphaVantageQuoteMapper {

    public ExternalQuoteResponse map(
            AlphaVantageQuoteResponse response
    ) {

        if (response == null ||
                response.getGlobalQuote() == null) {

            throw new IllegalArgumentException(
                    "Alpha Vantage returned no quote data."
            );
        }

        AlphaVantageQuoteResponse.GlobalQuote quote =
                response.getGlobalQuote();

        ExternalQuoteResponse result =
                new ExternalQuoteResponse();

        result.setSymbol(
                quote.getSymbol()
        );

        result.setOpen(
                toBigDecimal(quote.getOpen())
        );

        result.setHigh(
                toBigDecimal(quote.getHigh())
        );

        result.setLow(
                toBigDecimal(quote.getLow())
        );

        result.setClose(
                toBigDecimal(quote.getPrice())
        );

        result.setPreviousClose(
                toBigDecimal(quote.getPreviousClose())
        );

        result.setChange(
                toBigDecimal(quote.getChange())
        );

        result.setPercentChange(
                parsePercentChange(
                        quote.getChangePercent()
                )
        );

        result.setVolume(
                toLong(quote.getVolume())
        );

        result.setDatetime(
                quote.getLatestTradingDay()
        );

        return result;
    }

    private BigDecimal toBigDecimal(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        try {

            return new BigDecimal(
                    value.trim()
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    private BigDecimal parsePercentChange(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        String cleaned =
                value
                        .trim()
                        .replace("%", "");

        return toBigDecimal(cleaned);
    }

    private Long toLong(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        try {

            return Long.parseLong(
                    value.trim()
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }
}