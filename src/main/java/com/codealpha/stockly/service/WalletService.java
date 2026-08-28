package com.codealpha.stockly.service;

import java.util.List;
import com.codealpha.stockly.dto.WalletTransactionResponse;
import com.codealpha.stockly.entity.WalletTransaction;
import com.codealpha.stockly.entity.WalletTransactionType;
import com.codealpha.stockly.repository.WalletTransactionRepository;
import com.codealpha.stockly.dto.WalletResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final UserRepository userRepository;

    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(
            UserRepository userRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.userRepository = userRepository;
        this.walletTransactionRepository =
                walletTransactionRepository;
    }

    public WalletResponse getBalance(String userEmail) {

        User user = findUser(userEmail);

        return new WalletResponse(
                user.getVirtualBalance()
        );
    }


    @Transactional
    public WalletResponse deposit(
            String userEmail,
            BigDecimal amount
    ) {

        validateAmount(amount);

        User user = findUser(userEmail);

        BigDecimal currentBalance =
                user.getVirtualBalance();

        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        BigDecimal newBalance =
                currentBalance.add(amount);

        user.setVirtualBalance(newBalance);

        userRepository.save(user);

        WalletTransaction transaction =
                new WalletTransaction();

        transaction.setUser(user);
        transaction.setType(
                WalletTransactionType.DEPOSIT
        );
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setCreatedAt(
                java.time.LocalDateTime.now()
        );

        walletTransactionRepository.save(
                transaction
        );

        return new WalletResponse(
                newBalance
        );
    }


    @Transactional
    public WalletResponse withdraw(
            String userEmail,
            BigDecimal amount
    ) {

        validateAmount(amount);

        User user = findUser(userEmail);

        BigDecimal currentBalance =
                user.getVirtualBalance();

        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }

        if (amount.compareTo(currentBalance) > 0) {

            throw new IllegalArgumentException(
                    "Insufficient virtual balance"
            );
        }

        BigDecimal newBalance =
                currentBalance.subtract(amount);

        user.setVirtualBalance(newBalance);

        userRepository.save(user);

        WalletTransaction transaction =
                new WalletTransaction();

        transaction.setUser(user);
        transaction.setType(
                WalletTransactionType.WITHDRAW
        );
        transaction.setAmount(amount);
        transaction.setBalanceAfter(newBalance);
        transaction.setCreatedAt(
                java.time.LocalDateTime.now()
        );

        walletTransactionRepository.save(
                transaction
        );

        return new WalletResponse(
                newBalance
        );
    }


    private User findUser(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }


    private void validateAmount(
            BigDecimal amount
    ) {

        if (amount == null) {

            throw new IllegalArgumentException(
                    "Amount is required"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than 0"
            );
        }
    }

    public List<WalletTransactionResponse> getTransactions(
            String userEmail
    ) {

        User user = findUser(userEmail);

        return walletTransactionRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(transaction ->
                        new WalletTransactionResponse(
                                transaction.getId(),
                                transaction.getType().name(),
                                transaction.getAmount(),
                                transaction.getBalanceAfter(),
                                transaction.getCreatedAt()
                        )
                )
                .toList();
    }
}