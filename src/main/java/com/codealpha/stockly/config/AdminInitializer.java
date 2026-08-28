package com.codealpha.stockly.config;

import com.codealpha.stockly.entity.Role;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner createAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {

        return args -> {

            String adminEmail =
                    "admin@stockly.com";

            if (userRepository.existsByEmail(
                    adminEmail
            )) {
                return;
            }

            User admin = new User();

            admin.setName("Stockly Admin");

            admin.setEmail(adminEmail);

            admin.setPassword(
                    passwordEncoder.encode(
                            "Admin@123"
                    )
            );

            admin.setRole(Role.ADMIN);

            admin.setVirtualBalance(
                    new BigDecimal("10000.00")
            );

            admin.setCreatedAt(
                    LocalDateTime.now()
            );

            userRepository.save(admin);

            System.out.println(
                    "================================="
            );

            System.out.println(
                    "Stockly admin created"
            );

            System.out.println(
                    "Email: admin@stockly.com"
            );

            System.out.println(
                    "Password: Admin@123"
            );

            System.out.println(
                    "================================="
            );
        };
    }
}