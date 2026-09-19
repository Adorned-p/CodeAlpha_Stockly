package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.dto.AlphaVantageDailyResponse;
import com.codealpha.stockly.dto.ExternalQuoteResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;

@Service
public class MarketDataProviderService {

    private final EodhdMarketDataProvider eodhdMarketDataProvider;
    private final TwelveDataMarketDataProvider twelveDataProvider;
    private final AlphaVantageMarketDataProvider alphaVantageProvider;
    private final IndianMarketDataProvider indianMarketDataProvider;

    /*
     * If Twelve Data returns 429, we stop calling it for
     * the rest of the current day.
     */
    private LocalDate twelveDataDisabledUntil;

    public MarketDataProviderService(
            TwelveDataMarketDataProvider twelveDataMarketDataProvider,
            AlphaVantageMarketDataProvider alphaVantageMarketDataProvider,
            IndianMarketDataProvider indianMarketDataProvider,
            EodhdMarketDataProvider eodhdMarketDataProvider
    ) {

        this.twelveDataProvider =
                twelveDataMarketDataProvider;

        this.alphaVantageProvider =
                alphaVantageMarketDataProvider;

        this.indianMarketDataProvider =
                indianMarketDataProvider;

        this.eodhdMarketDataProvider =
                eodhdMarketDataProvider;
    }

    // =========================================================
    // QUOTE USING SYMBOL + EXCHANGE
    // =========================================================

    public ExternalQuoteResponse getQuote(
            String symbol,
            String exchange
    ) {

        /*
         * =====================================================
         * PROVIDER 1 — INDIAN MARKET API
         * =====================================================
         */

        if (isIndianExchange(exchange)) {

            try {

                ExternalQuoteResponse response =
                        indianMarketDataProvider.getQuote(
                                symbol,
                                exchange
                        );

                if (isValidQuote(response)) {

                    setProvider(
                            response,
                            "INDIAN_MARKET"
                    );

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
                                + ". Trying EODHD."
                );
            }
        }

        /*
         * =====================================================
         * PROVIDER 2 — EODHD
         * =====================================================
         */

        try {

            ExternalQuoteResponse response =
                    eodhdMarketDataProvider.getQuote(
                            symbol,
                            exchange
                    );

            if (isValidQuote(response)) {

                setProvider(
                        response,
                        "EODHD"
                );

                System.out.println(
                        "Market data provider: "
                                + "EODHD"
                                + " -> "
                                + symbol
                                + " ("
                                + exchange
                                + ")"
                );

                return response;
            }

        } catch (Exception exception) {

            System.err.println(
                    "EODHD unavailable for "
                            + symbol
                            + " ("
                            + exchange
                            + "): "
                            + exception.getMessage()
                            + ". Trying Twelve Data."
            );
        }

        /*
         * =====================================================
         * PROVIDER 3 — TWELVE DATA
         * =====================================================
         */

        if (isTwelveDataAvailable()) {

            try {

                ExternalQuoteResponse response =
                        twelveDataProvider.getQuote(
                                symbol,
                                exchange
                        );

                if (isValidQuote(response)) {

                    setProvider(
                            response,
                            "TWELVE_DATA"
                    );

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
         * PROVIDER 4 — ALPHA VANTAGE
         * =====================================================
         */

        try {

            ExternalQuoteResponse response =
                    alphaVantageProvider.getQuote(
                            symbol,
                            exchange
                    );

            if (isValidQuote(response)) {

                setProvider(
                        response,
                        "ALPHA_VANTAGE"
                );

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
    // QUOTE USING INSTRUMENT PROVIDER MAPPINGS
    // =========================================================

    public ExternalQuoteResponse getQuote(
            Instrument instrument
    ) {

        if (instrument == null) {
            return null;
        }

        String symbol =
                instrument.getSymbol();

        String exchange =
                instrument.getExchange();

        String selectedProvider =
                instrument.getMarketDataProvider();

        /*
         * =====================================================
         * NO SELECTED PROVIDER
         * =====================================================
         *
         * Preserve the old fallback behaviour for instruments
         * created before provider selection was introduced.
         */

        if (selectedProvider == null ||
                selectedProvider.isBlank()) {

            return getQuoteUsingOldFallback(
                    instrument
            );
        }

        String normalizedProvider =
                selectedProvider.trim().toUpperCase();

        /*
         * =====================================================
         * SELECTED PROVIDER
         * =====================================================
         *
         * The selected provider gets the first attempt.
         */

        switch (normalizedProvider) {

            case "EODHD":

                ExternalQuoteResponse eodhdResponse =
                        getEodhdQuote(instrument);

                if (isValidQuote(eodhdResponse)) {

                    return eodhdResponse;
                }

                break;

            case "TWELVE_DATA":

                ExternalQuoteResponse twelveDataResponse =
                        getTwelveDataQuote(instrument);

                if (isValidQuote(twelveDataResponse)) {

                    return twelveDataResponse;
                }

                break;

            case "ALPHA_VANTAGE":

                ExternalQuoteResponse alphaVantageResponse =
                        getAlphaVantageQuote(instrument);

                if (isValidQuote(alphaVantageResponse)) {

                    return alphaVantageResponse;
                }

                break;

            case "INDIAN":

            case "INDIAN_MARKET":

            case "INDIAN_MARKET_DATA":

                ExternalQuoteResponse indianResponse =
                        getIndianQuote(instrument);

                if (isValidQuote(indianResponse)) {

                    return indianResponse;
                }

                break;

            default:

                System.err.println(
                        "Unsupported market data provider "
                                + selectedProvider
                                + " for "
                                + symbol
                );

                break;
        }

        /*
         * =====================================================
         * FALLBACK
         * =====================================================
         *
         * If the selected provider fails, try the remaining
         * providers so that market-data updates remain resilient.
         */

        System.err.println(
                "Selected provider "
                        + selectedProvider
                        + " failed for "
                        + symbol
                        + ". Trying fallback providers."
        );

        return getFallbackQuote(
                instrument,
                normalizedProvider
        );
    }

    // =========================================================
    // SELECTED PROVIDER — EODHD
    // =========================================================

    private ExternalQuoteResponse getEodhdQuote(
            Instrument instrument
    ) {

        String providerSymbol =
                instrument.getEodhdSymbol();

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            System.err.println(
                    "No EODHD provider symbol configured for "
                            + instrument.getSymbol()
            );

            return null;
        }

        try {

            ExternalQuoteResponse response =
                    eodhdMarketDataProvider
                            .getQuoteByProviderSymbol(
                                    providerSymbol
                            );

            if (isValidQuote(response)) {

                setProvider(
                        response,
                        "EODHD"
                );

                System.out.println(
                        "Market data provider: EODHD -> "
                                + providerSymbol
                );

                return response;
            }

        } catch (Exception exception) {

            System.err.println(
                    "EODHD unavailable for "
                            + providerSymbol
                            + ": "
                            + exception.getMessage()
            );
        }

        return null;
    }

    // =========================================================
    // SELECTED PROVIDER — TWELVE DATA
    // =========================================================

    private ExternalQuoteResponse getTwelveDataQuote(
            Instrument instrument
    ) {

        String providerSymbol =
                instrument.getTwelveDataSymbol();

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            System.err.println(
                    "No Twelve Data provider symbol configured for "
                            + instrument.getSymbol()
            );

            return null;
        }

        if (!isTwelveDataAvailable()) {

            System.out.println(
                    "Twelve Data is temporarily disabled. "
                            + "Skipping "
                            + providerSymbol
            );

            return null;
        }

        try {

            ExternalQuoteResponse response =
                    twelveDataProvider
                            .getQuoteByProviderSymbol(
                                    providerSymbol
                            );

            if (isValidQuote(response)) {

                setProvider(
                        response,
                        "TWELVE_DATA"
                );

                System.out.println(
                        "Market data provider: Twelve Data -> "
                                + providerSymbol
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
                            + "Disabling until "
                            + twelveDataDisabledUntil
            );

        } catch (Exception exception) {

            System.err.println(
                    "Twelve Data unavailable for "
                            + providerSymbol
                            + ": "
                            + exception.getMessage()
            );
        }

        return null;
    }

    // =========================================================
    // SELECTED PROVIDER — ALPHA VANTAGE
    // =========================================================

    private ExternalQuoteResponse getAlphaVantageQuote(
            Instrument instrument
    ) {

        String providerSymbol =
                instrument.getAlphaVantageSymbol();

        if (providerSymbol == null ||
                providerSymbol.isBlank()) {

            System.err.println(
                    "No Alpha Vantage provider symbol configured for "
                            + instrument.getSymbol()
            );

            return null;
        }

        try {

            ExternalQuoteResponse response =
                    alphaVantageProvider
                            .getQuoteByProviderSymbol(
                                    providerSymbol
                            );

            if (isValidQuote(response)) {

                setProvider(
                        response,
                        "ALPHA_VANTAGE"
                );

                System.out.println(
                        "Market data provider: Alpha Vantage -> "
                                + providerSymbol
                );

                return response;
            }

        } catch (Exception exception) {

            System.err.println(
                    "Alpha Vantage unavailable for "
                            + providerSymbol
                            + ": "
                            + exception.getMessage()
            );
        }

        return null;
    }

    // =========================================================
    // SELECTED PROVIDER — INDIAN MARKET
    // =========================================================

    private ExternalQuoteResponse getIndianQuote(
            Instrument instrument
    ) {

        String symbol =
                instrument.getSymbol();

        String exchange =
                instrument.getExchange();

        if (!isIndianExchange(exchange)) {

            System.err.println(
                    "Indian Market API cannot be used for "
                            + symbol
                            + " ("
                            + exchange
                            + ")"
            );

            return null;
        }

        try {

            ExternalQuoteResponse response =
                    indianMarketDataProvider.getQuote(
                            symbol,
                            exchange
                    );

            if (isValidQuote(response)) {

                setProvider(
                        response,
                        "INDIAN_MARKET"
                );

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
            );
        }

        return null;
    }

    // =========================================================
    // FALLBACK FOR SELECTED PROVIDER
    // =========================================================

    private ExternalQuoteResponse getFallbackQuote(
            Instrument instrument,
            String failedProvider
    ) {

        String symbol =
                instrument.getSymbol();

        /*
         * -----------------------------------------------------
         * INDIAN MARKET API
         * -----------------------------------------------------
         */

        if (!isProvider(
                failedProvider,
                "INDIAN",
                "INDIAN_MARKET",
                "INDIAN_MARKET_DATA"
        ) && isIndianExchange(
                instrument.getExchange()
        )) {

            ExternalQuoteResponse response =
                    getIndianQuote(instrument);

            if (isValidQuote(response)) {

                return response;
            }
        }

        /*
         * -----------------------------------------------------
         * EODHD
         * -----------------------------------------------------
         */

        if (!failedProvider.equals("EODHD")) {

            ExternalQuoteResponse response =
                    getEodhdQuote(instrument);

            if (isValidQuote(response)) {

                return response;
            }
        }

        /*
         * -----------------------------------------------------
         * TWELVE DATA
         * -----------------------------------------------------
         */

        if (!failedProvider.equals("TWELVE_DATA")) {

            ExternalQuoteResponse response =
                    getTwelveDataQuote(instrument);

            if (isValidQuote(response)) {

                return response;
            }
        }

        /*
         * -----------------------------------------------------
         * ALPHA VANTAGE
         * -----------------------------------------------------
         */

        if (!failedProvider.equals("ALPHA_VANTAGE")) {

            ExternalQuoteResponse response =
                    getAlphaVantageQuote(instrument);

            if (isValidQuote(response)) {

                return response;
            }
        }

        System.err.println(
                "No fallback market data received for "
                        + symbol
                        + " ("
                        + instrument.getExchange()
                        + ")"
        );

        return null;
    }

    // =========================================================
    // OLD FALLBACK BEHAVIOUR
    // =========================================================

    private ExternalQuoteResponse getQuoteUsingOldFallback(
            Instrument instrument
    ) {

        String symbol =
                instrument.getSymbol();

        String exchange =
                instrument.getExchange();

        /*
         * Indian Market API
         */

        if (isIndianExchange(exchange)) {

            ExternalQuoteResponse response =
                    getIndianQuote(instrument);

            if (isValidQuote(response)) {

                return response;
            }
        }

        /*
         * EODHD
         */

        ExternalQuoteResponse eodhdResponse =
                getEodhdQuote(instrument);

        if (isValidQuote(eodhdResponse)) {

            return eodhdResponse;
        }

        /*
         * Twelve Data
         */

        ExternalQuoteResponse twelveDataResponse =
                getTwelveDataQuote(instrument);

        if (isValidQuote(twelveDataResponse)) {

            return twelveDataResponse;
        }

        /*
         * Alpha Vantage
         */

        ExternalQuoteResponse alphaVantageResponse =
                getAlphaVantageQuote(instrument);

        if (isValidQuote(alphaVantageResponse)) {

            return alphaVantageResponse;
        }

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
    // PROVIDER SETTER
    // =========================================================

    private void setProvider(
            ExternalQuoteResponse response,
            String provider
    ) {

        if (response != null) {

            response.setProvider(provider);
        }
    }

    // =========================================================
    // PROVIDER CHECK
    // =========================================================

    private boolean isProvider(
            String provider,
            String... values
    ) {

        if (provider == null) {

            return false;
        }

        for (String value : values) {

            if (provider.equals(value)) {

                return true;
            }
        }

        return false;
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