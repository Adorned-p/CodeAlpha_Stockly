package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.AlphaVantageDailyResponse;
import com.codealpha.stockly.dto.ExternalQuoteResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;

@Service
public class MarketDataProviderService {

    private final TwelveDataMarketDataProvider twelveDataProvider;
    private final AlphaVantageMarketDataProvider alphaVantageProvider;
    private final IndianMarketDataProvider indianMarketDataProvider;

    /*
     * If Twelve Data returns 429, we stop calling it for
     * the rest of the current day.
     */
    private LocalDate twelveDataDisabledUntil;

    public MarketDataProviderService(
            TwelveDataMarketDataProvider twelveDataProvider,
            AlphaVantageMarketDataProvider alphaVantageProvider,
            IndianMarketDataProvider indianMarketDataProvider
    ) {

        this.twelveDataProvider =
                twelveDataProvider;

        this.alphaVantageProvider =
                alphaVantageProvider;

        this.indianMarketDataProvider =
                indianMarketDataProvider;
    }

    // =========================================================
    // QUOTE
    // =========================================================

    public ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    ) {

        /*
         * =====================================================
         * PROVIDER 1 — INDIAN MARKET API
         * =====================================================
         *
         * Use NSE provider first for Indian exchanges.
         *
         * This is our preferred provider for Indian stocks
         * because it gets the quote directly from NSE.
         */

        if (isIndianExchange(exchange)) {

            try {

                ExternalQuoteResponse response =
                        indianMarketDataProvider.getQuote(
                                symbol,
                                exchange
                        );

                if (isValidQuote(response)) {

                    System.out.println(
                            "Market data provider: "
                                    + indianMarketDataProvider
                                    .getProviderName()
                                    + " -> "
                                    + symbol
                    );

                    return response;
                }

            } catch (Exception exception) {

                System.err.println(
                        "Indian market provider unavailable for "
                                + symbol
                                + " ("
                                + exchange
                                + "): "
                                + exception.getMessage()
                                + ". Trying Twelve Data."
                );
            }
        }

        /*
         * =====================================================
         * PROVIDER 2 — TWELVE DATA
         * =====================================================
         *
         * Used as fallback if the Indian provider fails.
         *
         * For US/global stocks, this is the primary provider.
         */

        if (isTwelveDataAvailable()) {

            try {

                ExternalQuoteResponse response =
                        twelveDataProvider.getQuote(
                                symbol,
                                exchange
                        );

                if (isValidQuote(response)) {

                    System.out.println(
                            "Market data provider: "
                                    + twelveDataProvider
                                    .getProviderName()
                                    + " -> "
                                    + symbol
                    );

                    return response;
                }

            } catch (
                    HttpClientErrorException.TooManyRequests exception
            ) {

                twelveDataDisabledUntil =
                        LocalDate.now().plusDays(1);

                System.err.println(
                        "Twelve Data daily quota exhausted. "
                                + "Disabling Twelve Data until "
                                + twelveDataDisabledUntil
                );

            } catch (Exception exception) {

                System.err.println(
                        "Twelve Data unavailable for "
                                + symbol
                                + " ("
                                + exchange
                                + "): "
                                + exception.getMessage()
                                + ". Trying Alpha Vantage."
                );
            }

        } else {

            System.out.println(
                    "Twelve Data is temporarily disabled "
                            + "because its daily quota was exhausted. "
                            + "Using Alpha Vantage."
            );
        }

        /*
         * =====================================================
         * PROVIDER 3 — ALPHA VANTAGE
         * =====================================================
         *
         * Final fallback for quote data.
         *
         * Historical daily data is also handled by
         * Alpha Vantage below.
         */

        try {

            ExternalQuoteResponse response =
                    alphaVantageProvider.getQuote(
                            symbol,
                            exchange
                    );

            if (isValidQuote(response)) {

                System.out.println(
                        "Market data provider: "
                                + alphaVantageProvider
                                .getProviderName()
                                + " -> "
                                + symbol
                );

                return response;
            }

        } catch (Exception exception) {

            System.err.println(
                    "Alpha Vantage unavailable for "
                            + symbol
                            + " ("
                            + exchange
                            + "): "
                            + exception.getMessage()
            );
        }

        /*
         * =====================================================
         * NO PROVIDER AVAILABLE
         * =====================================================
         */

        System.err.println(
                "No market data received for "
                        + symbol
                        + " ("
                        + exchange
                        + ")"
        );

        return null;
    }

    // =========================================================
    // DAILY HISTORY
    // =========================================================

    /*
     * Historical daily data remains explicitly handled
     * by Alpha Vantage.
     */
    public AlphaVantageDailyResponse getDailyHistory(
            String symbol,
            String exchange
    ) {

        return alphaVantageProvider
                .getDailyHistory(
                        symbol,
                        exchange
                );
    }

    // =========================================================
    // TWELVE DATA AVAILABILITY
    // =========================================================

    private boolean isTwelveDataAvailable() {

        if (twelveDataDisabledUntil == null) {

            return true;
        }

        if (LocalDate.now().isBefore(
                twelveDataDisabledUntil
        )) {

            return false;
        }

        twelveDataDisabledUntil = null;

        System.out.println(
                "Twelve Data quota reset window reached. "
                        + "Re-enabling Twelve Data."
        );

        return true;
    }

    // =========================================================
    // INDIAN EXCHANGE CHECK
    // =========================================================

    private boolean isIndianExchange(
            String exchange
    ) {

        if (exchange == null ||
                exchange.isBlank()) {

            return false;
        }

        String normalizedExchange =
                exchange.trim().toUpperCase();

        return normalizedExchange.equals("NSE")
                || normalizedExchange.equals("BSE");
    }

    // =========================================================
    // QUOTE VALIDATION
    // =========================================================

    private boolean isValidQuote(
            ExternalQuoteResponse response
    ) {

        if (response == null) {

            return false;
        }

        if (response.getClose() == null) {

            return false;
        }

        return response.getClose()
                .compareTo(
                        java.math.BigDecimal.ZERO
                ) > 0;
    }
}