package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.PortfolioHistoryResponse;
import java.time.LocalDateTime;
import java.util.List;
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

    @GetMapping("/history")
    public List<PortfolioHistoryResponse> getPortfolioHistory(
            @RequestParam(defaultValue = "ONE_DAY") String range,
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        LocalDateTime end =
                LocalDateTime.now();

        LocalDateTime start;

        switch (range.toUpperCase()) {

            case "ONE_WEEK":
                start = end.minusDays(7);
                break;

            case "ONE_MONTH":
                start = end.minusDays(30);
                break;

            case "ONE_YEAR":
                start = end.minusYears(1);
                break;

            case "ONE_DAY":
            default:
                start = end.minusHours(24);
                break;
        }

        return portfolioService.getPortfolioHistory(
                user.getEmail(),
                start,
                end
        );
    }
}