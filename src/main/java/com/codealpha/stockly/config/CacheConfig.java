package com.codealpha.stockly.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {

        CaffeineCache marketQuotes =
                new CaffeineCache(
                        "marketQuotes",
                        Caffeine.newBuilder()
                                .expireAfterWrite(
                                        Duration.ofSeconds(30)
                                )
                                .maximumSize(100)
                                .build()
                );

        CaffeineCache marketHistory =
                new CaffeineCache(
                        "marketHistory",
                        Caffeine.newBuilder()
                                .expireAfterWrite(
                                        Duration.ofMinutes(10)
                                )
                                .maximumSize(100)
                                .build()
                );

        CaffeineCache instrumentSearch =
                new CaffeineCache(
                        "instrumentSearch",
                        Caffeine.newBuilder()
                                .expireAfterWrite(
                                        Duration.ofMinutes(30)
                                )
                                .maximumSize(200)
                                .build()
                );

        SimpleCacheManager cacheManager =
                new SimpleCacheManager();

        cacheManager.setCaches(
                List.of(
                        marketQuotes,
                        marketHistory,
                        instrumentSearch
                )
        );

        return cacheManager;
    }
}