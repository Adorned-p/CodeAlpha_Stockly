package com.codealpha.stockly.controller;

import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.AiPortfolioService;
import com.codealpha.stockly.service.AiService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AiPortfolioService aiPortfolioService;

    public AiController(
            AiService aiService,
            AiPortfolioService aiPortfolioService
    ) {
        this.aiService = aiService;
        this.aiPortfolioService =
                aiPortfolioService;
    }

    // =========================================================
    // AI CONNECTION TEST
    // =========================================================

    @GetMapping("/test")
    public String testAi() {
        return aiService.testConnection();
    }

    // =========================================================
    // STOCK ANALYSIS
    // =========================================================

    @GetMapping("/stock/{symbol}")
    public String analyzeStock(
            @PathVariable String symbol
    ) {
        return aiService.analyzeStock(symbol);
    }

    // =========================================================
    // PORTFOLIO ANALYSIS
    // =========================================================

    @GetMapping("/portfolio")
    public String analyzePortfolio(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Authenticated user not found."
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof User user)) {

            throw new IllegalStateException(
                    "Authenticated principal is not a Stockly user."
            );
        }

        System.out.println(
                "AI Portfolio Request - User ID: "
                        + user.getId()
        );

        return aiPortfolioService.analyzePortfolio(
                user
        );
    }
}