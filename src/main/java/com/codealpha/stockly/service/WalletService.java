package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.WalletTransactionResponse;
import com.codealpha.stockly.dto.WalletResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.entity.WalletTransaction;
import com.codealpha.stockly.entity.WalletTransactionType;
import com.codealpha.stockly.repository.UserRepository;
import com.codealpha.stockly.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final UserRepository userRepository;

    private final WalletTransactionRepository
            walletTransactionRepository;

    public WalletService(
            UserRepository userRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.userRepository = userRepository;
        this.walletTransactionRepository =
                walletTransactionRepository;
    }

    // =========================================================
    // GET BALANCE
    // =========================================================

    public WalletResponse getBalance(
            String userEmail
    ) {

        User user = findUser(userEmail);

        BigDecimal virtualBalance =
                getSafeBalance(
                        user.getVirtualBalance()
                );

        BigDecimal reservedBalance =
                getSafeBalance(
                        user.getReservedBalance()
                );

        BigDecimal availableBalance =
                virtualBalance.subtract(
                        reservedBalance
                );

        /*
         * Safety check.
         */
        if (availableBalance.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            availableBalance =
                    BigDecimal.ZERO;
        }

        return new WalletResponse(
                virtualBalance,
                reservedBalance,
                availableBalance
        );
    }

    // =========================================================
    // DEPOSIT
    // =========================================================

    @Transactional
    public WalletResponse deposit(
            String userEmail,
            BigDecimal amount
    ) {

        validateAmount(amount);

        User user = findUser(userEmail);

        BigDecimal currentBalance =
                getSafeBalance(
                        user.getVirtualBalance()
                );

        BigDecimal newBalance =
                currentBalance.add(amount);

        user.setVirtualBalance(
                newBalance
        );

        userRepository.save(user);

        WalletTransaction transaction =
                new WalletTransaction();

        transaction.setUser(user);

        transaction.setType(
                WalletTransactionType.DEPOSIT
        );

        transaction.setAmount(amount);

        transaction.setBalanceAfter(
                newBalance
        );

        transaction.setCreatedAt(
                java.time.LocalDateTime.now()
        );

        walletTransactionRepository.save(
                transaction
        );

        return buildResponse(user);
    }

    // =========================================================
    // WITHDRAW
    // =========================================================

    @Transactional
    public WalletResponse withdraw(
            String userEmail,
            BigDecimal amount
    ) {

        validateAmount(amount);

        User user = findUser(userEmail);

        BigDecimal currentBalance =
                getSafeBalance(
                        user.getVirtualBalance()
                );

        BigDecimal reservedBalance =
                getSafeBalance(
                        user.getReservedBalance()
                );

        BigDecimal availableBalance =
                currentBalance.subtract(
                        reservedBalance
                );

        if (availableBalance.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            availableBalance =
                    BigDecimal.ZERO;
        }

        /*
         * Withdrawal must use AVAILABLE balance,
         * not total balance.
         *
         * This prevents withdrawing money that is
         * currently reserved for an open BUY order.
         */
        if (amount.compareTo(
                availableBalance
        ) > 0) {

            throw new IllegalArgumentException(
                    "Insufficient available virtual balance"
            );
        }

        BigDecimal newBalance =
                currentBalance.subtract(amount);

        user.setVirtualBalance(
                newBalance
        );

        userRepository.save(user);

        WalletTransaction transaction =
                new WalletTransaction();

        transaction.setUser(user);

        transaction.setType(
                WalletTransactionType.WITHDRAW
        );

        transaction.setAmount(amount);

        transaction.setBalanceAfter(
                newBalance
        );

        transaction.setCreatedAt(
                java.time.LocalDateTime.now()
        );

        walletTransactionRepository.save(
                transaction
        );

        return buildResponse(user);
    }

    // =========================================================
    // GET TRANSACTIONS
    // =========================================================

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

    // =========================================================
    // FIND USER
    // =========================================================

    private User findUser(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
    }

    // =========================================================
    // BUILD RESPONSE
    // =========================================================

    private WalletResponse buildResponse(
            User user
    ) {

        BigDecimal virtualBalance =
                getSafeBalance(
                        user.getVirtualBalance()
                );

        BigDecimal reservedBalance =
                getSafeBalance(
                        user.getReservedBalance()
                );

        BigDecimal availableBalance =
                virtualBalance.subtract(
                        reservedBalance
                );

        if (availableBalance.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            availableBalance =
                    BigDecimal.ZERO;
        }

        return new WalletResponse(
                virtualBalance,
                reservedBalance,
                availableBalance
        );
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateAmount(
            BigDecimal amount
    ) {

        if (amount == null) {

            throw new IllegalArgumentException(
                    "Amount is required"
            );
        }

        if (amount.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than 0"
            );
        }
    }

    // =========================================================
    // NULL-SAFE BALANCE
    // =========================================================

    private BigDecimal getSafeBalance(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}