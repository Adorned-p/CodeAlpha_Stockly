package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.UpdateBalanceRequest;
import com.codealpha.stockly.dto.UserResponse;
import com.codealpha.stockly.entity.Role;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.codealpha.stockly.dto.UpdateBalanceRequest;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }


    // =========================================================
    // CHECK EMAIL
    // =========================================================

    @GetMapping("/check-email")
    public boolean checkEmail(
            @RequestParam String email
    ) {

        return userService.emailExists(email);
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    @GetMapping("/me")
    public UserResponse getCurrentUser(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return toResponse(user);
    }


    // =========================================================
    // ADMIN - GET ALL USERS
    // =========================================================

    @GetMapping("/admin")
    public List<UserResponse> getAllUsers() {

        return userService
                .getAllUsers()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // =========================================================
    // ADMIN - GET USER BY ID
    // =========================================================

    @GetMapping("/admin/{id}")
    public UserResponse getUserById(
            @PathVariable Long id
    ) {

        User user =
                userService.getUserById(id);

        return toResponse(user);
    }


    // =========================================================
    // ADMIN - UPDATE ROLE
    // =========================================================

    @PutMapping("/admin/{id}/role")
    public UserResponse updateUserRole(
            @PathVariable Long id,
            @RequestParam Role role,
            Authentication authentication
    ) {

        User admin =
                (User) authentication.getPrincipal();

        User updatedUser =
                userService.updateUserRole(
                        id,
                        role,
                        admin.getEmail()
                );

        return toResponse(updatedUser);
    }


    // =========================================================
    // ENTITY → RESPONSE
    // =========================================================

    private UserResponse toResponse(
            User user
    ) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getVirtualBalance(),
                user.getCreatedAt()
        );
    }

    @PutMapping("/admin/{id}/balance")
    public UserResponse updateVirtualBalance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBalanceRequest request
    ) {

        User updatedUser =
                userService.updateVirtualBalance(
                        id,
                        request.getAmount()
                );

        return toResponse(updatedUser);
    }
}