package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalQuoteResponse;

public interface MarketDataProvider {

    ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    );

    String getProviderName();
}