package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.BuyRequest;
import com.codealpha.stockly.dto.SellRequest;
import com.codealpha.stockly.dto.TradeResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/buy")
    public TradeResponse buyStock(
            @Valid @RequestBody BuyRequest request,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return tradeService.buyStock(
                user.getEmail(),
                request
        );
    }

    @PostMapping("/sell")
    public TradeResponse sellStock(
            @Valid @RequestBody SellRequest request,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return tradeService.sellStock(
                user.getEmail(),
                request
        );
    }
}