package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.TransactionResponse;
import com.codealpha.stockly.entity.Transaction;
import com.codealpha.stockly.entity.TransactionType;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.TransactionRepository;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionResponse> getUserTransactions(
            String userEmail,
            TransactionType type
    ) {

        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        List<Transaction> transactions;

        if (type == null) {

            transactions =
                    transactionRepository
                            .findByUserOrderByExecutedAtDesc(user);

        } else {

            transactions =
                    transactionRepository
                            .findByUserAndTypeOrderByExecutedAtDesc(
                                    user,
                                    type
                            );
        }

        return transactions.stream()
                .map(transaction ->
                        new TransactionResponse(
                                transaction.getId(),
                                transaction.getStock().getSymbol(),
                                transaction.getStock().getCompanyName(),
                                transaction.getType(),
                                transaction.getQuantity(),
                                transaction.getPrice(),
                                transaction.getTotalAmount(),
                                transaction.getExecutedAt()
                        )
                )
                .toList();
    }
}