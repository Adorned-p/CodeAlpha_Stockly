package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Execution;
import com.codealpha.stockly.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionRepository
        extends JpaRepository<Execution, Long> {

    List<Execution> findByOrderOrderByExecutedAtDesc(
            Order order
    );
}