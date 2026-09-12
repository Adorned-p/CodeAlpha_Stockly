package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalQuoteResponse;
import org.springframework.stereotype.Component;

@Component
public class TwelveDataMarketDataProvider
        implements MarketDataProvider {

    private final ExternalMarketDataClient externalMarketDataClient;

    public TwelveDataMarketDataProvider(
            ExternalMarketDataClient externalMarketDataClient
    ) {
        this.externalMarketDataClient =
                externalMarketDataClient;
    }

    @Override
    public ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    ) {

        return externalMarketDataClient.getQuote(
                symbol,
                exchange
        );
    }

    @Override
    public String getProviderName() {

        return "Twelve Data";
    }
}