package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.WalletTransactionResponse;

import java.util.List;
import com.codealpha.stockly.dto.DepositRequest;
import com.codealpha.stockly.dto.WalletResponse;
import com.codealpha.stockly.dto.WithdrawRequest;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(
            WalletService walletService
    ) {
        this.walletService = walletService;
    }


    @GetMapping("/balance")
    public WalletResponse getBalance(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return walletService.getBalance(
                user.getEmail()
        );
    }


    @PostMapping("/deposit")
    public WalletResponse deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return walletService.deposit(
                user.getEmail(),
                request.getAmount()
        );
    }


    @PostMapping("/withdraw")
    public WalletResponse withdraw(
            @Valid @RequestBody WithdrawRequest request,
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return walletService.withdraw(
                user.getEmail(),
                request.getAmount()
        );
    }

    @GetMapping("/transactions")
    public List<WalletTransactionResponse> getTransactions(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return walletService.getTransactions(
                user.getEmail()
        );
    }
}