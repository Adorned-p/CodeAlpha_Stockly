package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.TransactionResponse;
import com.codealpha.stockly.entity.TransactionType;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.TransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService
    ) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponse> getTransactions(
            Authentication authentication,
            @RequestParam(required = false) TransactionType type
    ) {

        User user =
                (User) authentication.getPrincipal();

        return transactionService.getUserTransactions(
                user.getEmail(),
                type
        );
    }
}