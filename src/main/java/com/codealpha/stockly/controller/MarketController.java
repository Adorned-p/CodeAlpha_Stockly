package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.MarketIndexResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    @GetMapping("/indices")
    public List<MarketIndexResponse> getMarketIndices() {

        return List.of(

                new MarketIndexResponse(
                        "NIFTY50",
                        "NIFTY 50",
                        new BigDecimal("24813.75"),
                        new BigDecimal("211.52"),
                        new BigDecimal("0.86")
                ),

                new MarketIndexResponse(
                        "SENSEX",
                        "SENSEX",
                        new BigDecimal("81330.43"),
                        new BigDecimal("597.88"),
                        new BigDecimal("0.74")
                )

        );
    }
}