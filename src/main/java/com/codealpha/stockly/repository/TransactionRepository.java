package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Transaction;
import com.codealpha.stockly.entity.TransactionType;
import com.codealpha.stockly.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    long count();

    @Query("""
            SELECT COALESCE(SUM(t.totalAmount), 0)
            FROM Transaction t
            """)
    BigDecimal getTotalTradingVolume();

    List<Transaction> findByUserOrderByExecutedAtDesc(
            User user
    );

    List<Transaction>
    findByUserAndTypeOrderByExecutedAtDesc(
            User user,
            TransactionType type
    );
}