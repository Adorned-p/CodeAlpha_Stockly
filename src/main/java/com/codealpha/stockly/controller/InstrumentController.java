package com.codealpha.stockly.controller;

import java.math.BigDecimal;
import com.codealpha.stockly.service.CurrencyConversionService;
import com.codealpha.stockly.dto.CreateInstrumentRequest;
import com.codealpha.stockly.dto.InstrumentSearchResponse;
import com.codealpha.stockly.dto.InstrumentResponse;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.service.InstrumentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    private final CurrencyConversionService currencyConversionService;
    public InstrumentController(
            InstrumentService instrumentService,
            CurrencyConversionService currencyConversionService
    ) {

        this.instrumentService =
                instrumentService;

        this.currencyConversionService =
                currencyConversionService;
    }

    @GetMapping
    public List<InstrumentResponse> getAllInstruments() {

        return instrumentService
                .getAllInstruments()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/search")
    public List<InstrumentSearchResponse> searchInstruments(
            @RequestParam String query
    ) {

        return instrumentService.searchInstruments(query);
    }

    @GetMapping("/{symbol}")
    public InstrumentResponse getInstrument(
            @PathVariable String symbol
    ) {

        return toResponse(
                instrumentService.getBySymbol(symbol)
        );
    }

    @PostMapping("/from-stock/{stockId}")
    public InstrumentResponse createFromStock(
            @PathVariable Long stockId,
            @RequestParam String assetType,
            @RequestParam String country,
            @RequestParam String currency
    ) {

        Instrument instrument =
                instrumentService.createFromStock(
                        stockId,
                        assetType,
                        country,
                        currency
                );

        return toResponse(instrument);
    }

    private InstrumentResponse toResponse(
            Instrument instrument
    ) {

        BigDecimal currentPriceInr;

        BigDecimal exchangeRateToInr;

        if ("INR".equalsIgnoreCase(
                instrument.getCurrency()
        )) {

            currentPriceInr =
                    instrument.getCurrentPrice();

            exchangeRateToInr =
                    BigDecimal.ONE;

        } else {

            currentPriceInr =
                    currencyConversionService
                            .convertToInr(
                                    instrument.getCurrentPrice(),
                                    instrument.getCurrency()
                            );

            exchangeRateToInr =
                    currencyConversionService
                            .getExchangeRate(
                                    instrument.getCurrency()
                            );
        }

        return new InstrumentResponse(
                instrument.getId(),
                instrument.getSymbol(),
                instrument.getName(),
                instrument.getAssetType(),
                instrument.getExchange(),
                instrument.getCountry(),
                instrument.getCurrency(),
                instrument.getCurrentPrice(),
                currentPriceInr,
                exchangeRateToInr,
                instrument.isActive()
        );
    }

    @PostMapping("/from-search")
    public InstrumentResponse createFromSearch(
            @RequestBody CreateInstrumentRequest request
    ) {

        Instrument instrument =
                instrumentService.createFromSearch(
                        request.getSymbol(),
                        request.getName(),
                        request.getAssetType(),
                        request.getExchange(),
                        request.getCountry(),
                        request.getCurrency(),
                        request.getProvider(),
                        request.getProviderSymbol()
                );

        return toResponse(instrument);
    }
}