package com.codealpha.stockly.dto;

import com.codealpha.stockly.entity.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private BigDecimal virtualBalance;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String name,
            String email,
            Role role,
            BigDecimal virtualBalance,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.virtualBalance = virtualBalance;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public BigDecimal getVirtualBalance() {
        return virtualBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}