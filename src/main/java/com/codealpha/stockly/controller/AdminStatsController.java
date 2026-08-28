package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.AdminStatsResponse;
import com.codealpha.stockly.service.AdminStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(
            AdminStatsService adminStatsService
    ) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/stats")
    public AdminStatsResponse getStats() {

        return adminStatsService.getStats();
    }
}