package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.InstrumentSearchResponse;
import com.codealpha.stockly.dto.ExternalQuoteResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EodhdMarketDataProvider {

    private final EodhdMarketDataClient client;

    public EodhdMarketDataProvider(
            EodhdMarketDataClient client
    ) {
        this.client = client;
    }

    // =========================================================
    // GET QUOTE
    // =========================================================

    public ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    ) {

        return client.getQuote(
                symbol,
                exchange
        );
    }

    // =========================================================
    // GET QUOTE USING PROVIDER-SPECIFIC SYMBOL
    // =========================================================

    public ExternalQuoteResponse getQuoteByProviderSymbol(
            String providerSymbol
    ) {

        return client.getQuoteByProviderSymbol(
                providerSymbol
        );
    }

    // =========================================================
    // SEARCH SYMBOLS
    // =========================================================

    public List<InstrumentSearchResponse> searchSymbols(
            String query
    ) {

        return client.searchSymbols(
                query
        );
    }
}