package com.codealpha.stockly.service;

import com.codealpha.stockly.exception.ResourceNotFoundException;
import com.codealpha.stockly.entity.Role;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // =========================================================
    // EXISTING METHODS
    // =========================================================

    public Optional<User> findByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {

        return userRepository.existsByEmail(email);
    }

    public User saveUser(User user) {

        return userRepository.save(user);
    }


    // =========================================================
    // ADMIN - GET ALL USERS
    // =========================================================

    public List<User> getAllUsers() {

        return userRepository.findAll();
    }


    // =========================================================
    // ADMIN - GET USER BY ID
    // =========================================================

    public User getUserById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + id
                        )
                );
    }


    // =========================================================
    // ADMIN - CHANGE USER ROLE
    // =========================================================

    @Transactional
    public User updateUserRole(
            Long id,
            Role role,
            String adminEmail
    ) {

        User user = getUserById(id);

        /*
         * Prevent an admin from accidentally
         * removing their own admin privileges.
         */
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {

            throw new IllegalArgumentException(
                    "You cannot change your own role"
            );
        }

        user.setRole(role);

        return userRepository.save(user);
    }

    @Transactional
    public User updateVirtualBalance(
            Long id,
            BigDecimal amount
    ) {

        User user = getUserById(id);

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Balance amount cannot be negative"
            );
        }

        user.setVirtualBalance(amount);

        return userRepository.save(user);
    }
}