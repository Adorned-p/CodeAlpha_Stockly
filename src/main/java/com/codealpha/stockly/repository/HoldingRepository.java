package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Holding;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByUser(User user);

    Optional<Holding> findByUserAndStock(User user, Stock stock);

    boolean existsByUserAndStock(User user, Stock stock);
}