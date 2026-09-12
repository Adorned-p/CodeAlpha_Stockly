package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.AccountSummaryResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.AccountSummaryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountSummaryController {

    private final AccountSummaryService accountSummaryService;

    public AccountSummaryController(
            AccountSummaryService accountSummaryService
    ) {
        this.accountSummaryService =
                accountSummaryService;
    }

    @GetMapping("/summary")
    public AccountSummaryResponse getSummary(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return accountSummaryService.getSummary(
                user.getEmail()
        );
    }
}