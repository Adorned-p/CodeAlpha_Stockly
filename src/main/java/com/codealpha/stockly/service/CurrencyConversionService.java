package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalExchangeRateResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CurrencyConversionService {

    private static final String BASE_CURRENCY = "INR";

    /*
     * Currencies that STOCKLY currently supports.
     *
     * We can add more later without changing
     * the conversion logic.
     */
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "USD",
            "EUR",
            "GBP",
            "JPY",
            "CHF",
            "CAD",
            "AUD",
            "HKD",
            "SGD",
            "KRW",
            "BRL",
            "ZAR",
            "AED"
    );

    private final ExternalMarketDataClient externalMarketDataClient;

    /*
     * Free fallback exchange-rate provider.
     *
     * No API key required.
     */
    private final RestClient fallbackRestClient;

    /*
     * Cached exchange rates.
     *
     * Example:
     *
     * USD -> 87.50
     * EUR -> 102.20
     * GBP -> 118.40
     * JPY -> 0.58
     */
    private final Map<String, BigDecimal> exchangeRates =
            new ConcurrentHashMap<>();

    public CurrencyConversionService(
            ExternalMarketDataClient externalMarketDataClient
    ) {

        this.externalMarketDataClient =
                externalMarketDataClient;

        this.fallbackRestClient =
                RestClient.builder()
                        .baseUrl("https://api.frankfurter.dev")
                        .build();

        /*
         * INR -> INR
         */
        exchangeRates.put(
                BASE_CURRENCY,
                BigDecimal.ONE
        );
    }

    // =========================================================
    // CONVERT TO INR
    // =========================================================

    public BigDecimal convertToInr(
            BigDecimal amount,
            String currency
    ) {

        if (amount == null) {
            return null;
        }

        String normalizedCurrency =
                normalizeCurrency(currency);

        /*
         * INR requires no conversion.
         */
        if (BASE_CURRENCY.equals(
                normalizedCurrency
        )) {

            return amount.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        /*
         * First use the cached rate.
         */
        BigDecimal rate =
                exchangeRates.get(
                        normalizedCurrency
                );

        /*
         * If the rate isn't cached yet,
         * fetch it.
         */
        if (rate == null) {

            refreshRate(
                    normalizedCurrency
            );

            rate =
                    exchangeRates.get(
                            normalizedCurrency
                    );
        }

        /*
         * Never crash the stock/portfolio
         * endpoint because FX data temporarily
         * failed.
         */
        if (rate == null) {

            System.err.println(
                    "No INR exchange rate available for "
                            + normalizedCurrency
            );

            return null;
        }

        return amount
                .multiply(rate)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    // =========================================================
    // GET EXCHANGE RATE
    // =========================================================

    public BigDecimal getExchangeRate(
            String currency
    ) {

        if (currency == null ||
                currency.isBlank()) {

            return null;
        }

        String normalizedCurrency =
                currency.trim().toUpperCase();

        /*
         * INR -> INR
         */
        if (BASE_CURRENCY.equals(
                normalizedCurrency
        )) {

            return BigDecimal.ONE;
        }

        BigDecimal rate =
                exchangeRates.get(
                        normalizedCurrency
                );

        /*
         * Fetch on demand if this currency
         * hasn't been loaded yet.
         */
        if (rate == null) {

            refreshRate(
                    normalizedCurrency
            );

            rate =
                    exchangeRates.get(
                            normalizedCurrency
                    );
        }

        return rate;
    }

    // =========================================================
    // REFRESH ONE CURRENCY
    // =========================================================

    private void refreshRate(
            String currency
    ) {

        if (currency == null ||
                currency.isBlank()) {

            return;
        }

        String normalizedCurrency =
                currency.trim().toUpperCase();

        if (BASE_CURRENCY.equals(
                normalizedCurrency
        )) {

            exchangeRates.put(
                    BASE_CURRENCY,
                    BigDecimal.ONE
            );

            return;
        }

        if (!SUPPORTED_CURRENCIES.contains(
                normalizedCurrency
        )) {

            System.err.println(
                    "Unsupported currency for INR conversion: "
                            + normalizedCurrency
            );

            return;
        }

        /*
         * =====================================================
         * PRIMARY: TWELVE DATA
         * =====================================================
         */

        try {

            ExternalExchangeRateResponse response =
                    externalMarketDataClient
                            .getExchangeRate(
                                    normalizedCurrency,
                                    BASE_CURRENCY
                            );

            if (response != null &&
                    response.getRate() != null &&
                    response.getRate()
                            .compareTo(
                                    BigDecimal.ZERO
                            ) > 0) {

                exchangeRates.put(
                        normalizedCurrency,
                        response.getRate()
                );

                System.out.println(
                        normalizedCurrency
                                + "/INR exchange rate updated "
                                + "from Twelve Data: "
                                + response.getRate()
                );

                return;
            }

            System.err.println(
                    "Twelve Data returned no valid "
                            + normalizedCurrency
                            + "/INR rate. Trying fallback."
            );

        } catch (Exception exception) {

            System.err.println(
                    "Twelve Data "
                            + normalizedCurrency
                            + "/INR failed: "
                            + exception.getMessage()
            );
        }

        /*
         * =====================================================
         * FALLBACK: FRANKFURTER
         * =====================================================
         */

        try {

            Map<String, Object> response =
                    fallbackRestClient.get()
                            .uri(uriBuilder ->
                                    uriBuilder
                                            .path(
                                                    "/v2/rate/{base}/{quote}"
                                            )
                                            .build(
                                                    normalizedCurrency,
                                                    BASE_CURRENCY
                                            )
                            )
                            .retrieve()
                            .body(Map.class);

            if (response == null) {

                System.err.println(
                        "Frankfurter returned an empty "
                                + normalizedCurrency
                                + "/INR response."
                );

                return;
            }

            Object rateValue =
                    response.get("rate");

            if (rateValue == null) {

                System.err.println(
                        "Frankfurter returned no "
                                + normalizedCurrency
                                + "/INR rate."
                );

                return;
            }

            BigDecimal rate =
                    new BigDecimal(
                            rateValue.toString()
                    );

            if (rate.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                System.err.println(
                        "Frankfurter returned an invalid "
                                + normalizedCurrency
                                + "/INR rate."
                );

                return;
            }

            exchangeRates.put(
                    normalizedCurrency,
                    rate
            );

            System.out.println(
                    normalizedCurrency
                            + "/INR exchange rate updated "
                            + "from Frankfurter fallback: "
                            + rate
            );

        } catch (Exception exception) {

            System.err.println(
                    "Frankfurter "
                            + normalizedCurrency
                            + "/INR fallback failed: "
                            + exception.getMessage()
            );
        }
    }

    // =========================================================
    // REFRESH CACHED RATES
    // =========================================================

    /*
     * Refresh currencies that have already been requested.
     *
     * We intentionally DO NOT request every supported currency
     * every 15 minutes.
     *
     * This prevents unnecessary API calls for currencies that
     * STOCKLY isn't currently using.
     */
    @Scheduled(fixedRate = 900000)
    public void refreshCachedRates() {

        for (String currency :
                exchangeRates.keySet()) {

            if (BASE_CURRENCY.equals(
                    currency
            )) {

                continue;
            }

            refreshRate(currency);
        }
    }

    // =========================================================
    // NORMALIZE CURRENCY
    // =========================================================

    private String normalizeCurrency(
            String currency
    ) {

        if (currency == null ||
                currency.isBlank()) {

            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }

        return currency
                .trim()
                .toUpperCase();
    }
}