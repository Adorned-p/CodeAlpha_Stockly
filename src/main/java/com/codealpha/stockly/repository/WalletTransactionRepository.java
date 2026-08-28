package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByUserOrderByCreatedAtDesc(
            User user
    );
}