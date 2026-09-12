package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.DashboardResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse getDashboard(
            Authentication authentication
    ) {

        User user =
                (User) authentication.getPrincipal();

        return dashboardService.getDashboard(
                user.getEmail()
        );
    }
}