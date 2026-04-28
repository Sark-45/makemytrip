package com.makemytrip.flight.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine in-process cache configuration.
 *
 * Cache name: {@code flightStatus}
 * - Maximum 500 entries (one per unique flight number in the engine)
 * - Expires 30 seconds after last write (matching the simulator tick interval)
 * - Statistics recording enabled for operational visibility
 *
 * The @CacheEvict annotation in {@link com.makemytrip.flight.service.FlightStatusService}
 * proactively removes stale entries so the cache TTL is a backstop, not the
 * primary eviction mechanism.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("flightStatus");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats());  // expose hit/miss metrics via Actuator
        return manager;
    }
}
