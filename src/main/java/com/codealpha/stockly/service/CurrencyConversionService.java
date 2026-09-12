package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.ExternalExchangeRateResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CurrencyConversionService {

    private static final String BASE_CURRENCY = "INR";

    private final ExternalMarketDataClient externalMarketDataClient;

    /*
     * Free fallback exchange-rate provider.
     *
     * No API key is required.
     */
    private final RestClient fallbackRestClient;

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
                "INR",
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

        if (currency == null ||
                currency.isBlank()) {

            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }

        String normalizedCurrency =
                currency.trim().toUpperCase();

        /*
         * No conversion required.
         */
        if (BASE_CURRENCY.equals(
                normalizedCurrency
        )) {

            return amount.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        BigDecimal rate =
                exchangeRates.get(
                        normalizedCurrency
                );

        /*
         * USD is currently the only foreign
         * currency used by Stockly.
         *
         * Try to refresh the rate when it is
         * not already available.
         */
        if (rate == null &&
                "USD".equals(normalizedCurrency)) {

            refreshUsdInrRate();

            rate =
                    exchangeRates.get(
                            normalizedCurrency
                    );
        }

        /*
         * Never allow a temporary exchange-rate
         * provider failure to crash /api/stocks
         * or /api/portfolio.
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
    // GET RATE
    // =========================================================

    public BigDecimal getExchangeRate(
            String currency
    ) {

        if (currency == null ||
                currency.isBlank()) {

            return null;
        }

        return exchangeRates.get(
                currency.trim().toUpperCase()
        );
    }

    // =========================================================
    // REFRESH USD/INR
    // =========================================================

    /*
     * Refresh every 15 minutes.
     *
     * Primary:
     * Twelve Data
     *
     * Fallback:
     * Frankfurter
     *
     * USD/INR is shared by all USD-denominated
     * instruments, so we do NOT make one request
     * per stock.
     */
    @Scheduled(fixedRate = 900000)
    public void refreshUsdInrRate() {

        /*
         * =====================================================
         * PRIMARY: TWELVE DATA
         * =====================================================
         */

        try {

            ExternalExchangeRateResponse response =
                    externalMarketDataClient
                            .getExchangeRate(
                                    "USD",
                                    "INR"
                            );

            if (response != null &&
                    response.getRate() != null &&
                    response.getRate()
                            .compareTo(
                                    BigDecimal.ZERO
                            ) > 0) {

                exchangeRates.put(
                        "USD",
                        response.getRate()
                );

                System.out.println(
                        "USD/INR exchange rate updated "
                                + "from Twelve Data: "
                                + response.getRate()
                );

                return;
            }

            System.err.println(
                    "Twelve Data returned no valid "
                            + "USD/INR rate. Trying fallback."
            );

        } catch (Exception exception) {

            System.err.println(
                    "Twelve Data USD/INR failed: "
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
                                                    "/v2/rate/USD/INR"
                                            )
                                            .build()
                            )
                            .retrieve()
                            .body(Map.class);

            if (response == null) {

                System.err.println(
                        "Frankfurter returned an empty "
                                + "USD/INR response."
                );

                return;
            }

            Object rateValue =
                    response.get("rate");

            if (rateValue == null) {

                System.err.println(
                        "Frankfurter returned no USD/INR rate."
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
                                + "USD/INR rate."
                );

                return;
            }

            exchangeRates.put(
                    "USD",
                    rate
            );

            System.out.println(
                    "USD/INR exchange rate updated "
                            + "from Frankfurter fallback: "
                            + rate
            );

        } catch (Exception exception) {

            System.err.println(
                    "Frankfurter USD/INR fallback failed: "
                            + exception.getMessage()
            );
        }
    }
}