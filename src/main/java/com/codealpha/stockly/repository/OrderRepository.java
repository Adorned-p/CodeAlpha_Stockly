package com.codealpha.stockly.repository;

import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.Order;
import com.codealpha.stockly.entity.OrderSide;
import com.codealpha.stockly.entity.OrderStatus;
import com.codealpha.stockly.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    // =========================================================
    // GET ALL ORDERS WITH INSTRUMENT
    // =========================================================

    @Query("""
            SELECT o
            FROM Order o
            JOIN FETCH o.instrument
            """)
    List<Order> findAllWithInstrument();

    // =========================================================
    // USER ORDERS
    // =========================================================

    List<Order> findByUserOrderByCreatedAtDesc(
            User user
    );

    List<Order> findByUserAndStatus(
            User user,
            OrderStatus status
    );

    // =========================================================
    // INSTRUMENT ORDERS
    // =========================================================

    List<Order> findByInstrumentAndStatusOrderByCreatedAtAsc(
            Instrument instrument,
            OrderStatus status
    );

    List<Order> findByInstrumentAndSideAndStatusOrderByCreatedAtAsc(
            Instrument instrument,
            OrderSide side,
            OrderStatus status
    );
}