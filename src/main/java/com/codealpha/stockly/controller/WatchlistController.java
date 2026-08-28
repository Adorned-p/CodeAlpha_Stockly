package com.codealpha.stockly.controller;

import com.codealpha.stockly.dto.WatchlistResponse;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(
            WatchlistService watchlistService
    ) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<WatchlistResponse> getWatchlist(
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return watchlistService.getWatchlist(
                user.getEmail()
        );
    }

    @PostMapping("/{symbol}")
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistResponse addToWatchlist(
            @PathVariable String symbol,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return watchlistService.addToWatchlist(
                user.getEmail(),
                symbol
        );
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWatchlist(
            @PathVariable String symbol,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        watchlistService.removeFromWatchlist(
                user.getEmail(),
                symbol
        );
    }
}