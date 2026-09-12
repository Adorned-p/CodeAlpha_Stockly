package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.OrderRequest;
import com.codealpha.stockly.dto.OrderResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    // =========================================================
    // PLACE ORDER
    // =========================================================

    @PostMapping
    public OrderResponse placeOrder(
            @Valid @RequestBody OrderRequest request,
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return orderService.placeOrder(
                user.getEmail(),
                request
        );
    }

    // =========================================================
    // GET ALL USER ORDERS
    // =========================================================

    @GetMapping
    public List<OrderResponse> getOrders(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return orderService.getUserOrders(
                user.getEmail()
        );
    }

    // =========================================================
    // GET SINGLE ORDER
    // =========================================================

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return orderService.getOrder(
                user.getEmail(),
                orderId
        );
    }

    // =========================================================
    // CANCEL ORDER
    // =========================================================

    @DeleteMapping("/{orderId}")
    public OrderResponse cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return orderService.cancelOrder(
                user.getEmail(),
                orderId
        );
    }
}