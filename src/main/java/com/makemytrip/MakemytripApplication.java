package com.makemytrip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MakeMyTrip Spring Boot application entry point.
 *
 * Feature modules activated here:
 *  ✦ @EnableScheduling  → powers the FlightStatusSimulator ticks
 *  ✦ @EnableCaching     → activates Caffeine cache for flight status reads
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class MakemytripApplication {

    public static void main(String[] args) {
        SpringApplication.run(MakemytripApplication.class, args);
    }
}
