package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalQuoteResponse;
import org.springframework.stereotype.Component;

@Component
public class IndianMarketDataProvider
        implements MarketDataProvider {

    private final IndianMarketDataClient client;

    public IndianMarketDataProvider(
            IndianMarketDataClient client
    ) {
        this.client = client;
    }

    @Override
    public ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    ) {

        IndianMarketDataClient.IndianQuote quote =
                client.getQuote(
                        symbol,
                        exchange
                );

        ExternalQuoteResponse response =
                new ExternalQuoteResponse();

        response.setSymbol(
                quote.symbol()
        );

        response.setName(
                quote.name()
        );

        response.setExchange(
                quote.exchange()
        );

        response.setCurrency(
                quote.currency()
        );

        response.setDatetime(
                quote.datetime()
        );

        response.setOpen(
                quote.open()
        );

        response.setHigh(
                quote.high()
        );

        response.setLow(
                quote.low()
        );

        response.setClose(
                quote.close()
        );

        response.setPreviousClose(
                quote.previousClose()
        );

        response.setChange(
                quote.change()
        );

        response.setPercentChange(
                quote.percentChange()
        );

        response.setVolume(
                quote.volume()
        );

        return response;
    }

    @Override
    public String getProviderName() {

        return "Indian Market API";
    }
}