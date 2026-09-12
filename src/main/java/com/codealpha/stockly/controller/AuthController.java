package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.LoginRequest;
import com.codealpha.stockly.dto.LoginResponse;
import com.codealpha.stockly.dto.UserResponse;
import com.codealpha.stockly.dto.RegisterRequest;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword
    ) {

        authService.resetPassword(
                email,
                newPassword
        );

        return "Password reset successfully";
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getVirtualBalance(),
                user.getCreatedAt()
        );
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }
}