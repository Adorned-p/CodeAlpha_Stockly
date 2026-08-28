package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.PortfolioResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.PortfolioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(
            PortfolioService portfolioService
    ) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public PortfolioResponse getPortfolio(
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return portfolioService.getPortfolio(
                user.getEmail()
        );
    }

    @GetMapping("/holdings")
    public PortfolioResponse getHoldings(
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return portfolioService.getPortfolio(
                user.getEmail()
        );
    }
}