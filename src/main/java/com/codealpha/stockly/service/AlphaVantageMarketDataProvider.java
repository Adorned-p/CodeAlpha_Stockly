package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.AlphaVantageDailyResponse;
import com.codealpha.stockly.dto.AlphaVantageQuoteResponse;
import com.codealpha.stockly.dto.ExternalQuoteResponse;
import org.springframework.stereotype.Component;

@Component
public class AlphaVantageMarketDataProvider
        implements MarketDataProvider {

    private final AlphaVantageMarketDataClient alphaVantageMarketDataClient;
    private final AlphaVantageQuoteMapper alphaVantageQuoteMapper;

    public AlphaVantageMarketDataProvider(
            AlphaVantageMarketDataClient alphaVantageMarketDataClient,
            AlphaVantageQuoteMapper alphaVantageQuoteMapper
    ) {
        this.alphaVantageMarketDataClient =
                alphaVantageMarketDataClient;

        this.alphaVantageQuoteMapper =
                alphaVantageQuoteMapper;
    }

    @Override
    public ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    ) {

        String providerSymbol =
                buildProviderSymbol(
                        symbol,
                        exchange
                );

        AlphaVantageQuoteResponse response =
                alphaVantageMarketDataClient.getQuote(
                        providerSymbol
                );

        return alphaVantageQuoteMapper.map(response);
    }

    /*
     * This version allows InstrumentService to explicitly
     * provide a provider-specific Alpha Vantage symbol.
     */
    public ExternalQuoteResponse getQuoteByProviderSymbol(
            String providerSymbol
    ) {

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Alpha Vantage symbol is required"
            );
        }

        AlphaVantageQuoteResponse response =
                alphaVantageMarketDataClient.getQuote(
                        providerSymbol.trim().toUpperCase()
                );

        return alphaVantageQuoteMapper.map(response);
    }

    @Override
    public String getProviderName() {

        return "Alpha Vantage";
    }

    public AlphaVantageDailyResponse getDailyHistory(
            String symbol,
            String exchange
    ) {

        String providerSymbol =
                buildProviderSymbol(
                        symbol,
                        exchange
                );

        return alphaVantageMarketDataClient.getDailyHistory(
                providerSymbol
        );
    }

    public AlphaVantageDailyResponse getDailyHistoryByProviderSymbol(
            String providerSymbol
    ) {

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Alpha Vantage symbol is required"
            );
        }

        return alphaVantageMarketDataClient.getDailyHistory(
                providerSymbol.trim().toUpperCase()
        );
    }

    private String buildProviderSymbol(
            String symbol,
            String exchange
    ) {

        if (symbol == null ||
                symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol is required"
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase();

        /*
         * Already provider-specific.
         *
         * Example:
         * TCS.BSE
         */
        if (normalizedSymbol.contains(".")) {

            return normalizedSymbol;
        }

        if (exchange == null ||
                exchange.isBlank()) {

            return normalizedSymbol;
        }

        String normalizedExchange =
                exchange.trim().toUpperCase();

        /*
         * US stocks normally use their ticker directly.
         */
        if (normalizedExchange.equals("NASDAQ") ||
                normalizedExchange.equals("NYSE")) {

            return normalizedSymbol;
        }

        /*
         * Alpha Vantage BSE mapping.
         */
        if (normalizedExchange.equals("BSE")) {

            return normalizedSymbol + ".BSE";
        }

        /*
         * Do NOT invent NSE mappings.
         *
         * Alpha Vantage has not returned usable quote
         * data for our current TCS/NSE attempt.
         */
        return normalizedSymbol;
    }
}