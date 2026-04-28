package com.makemytrip.flight.repository;

import com.makemytrip.flight.model.FlightInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB repository for {@link FlightInfo} documents.
 *
 * Spring Data generates all CRUD implementations at runtime.
 * Additional query methods follow Spring Data naming conventions.
 */
@Repository
public interface FlightInfoRepository extends MongoRepository<FlightInfo, String> {

    /**
     * Find a flight by its IATA flight number.
     *
     * @param flightNumber e.g. "AI302"
     * @return Optional wrapping the matched flight, or empty if not found
     */
    Optional<FlightInfo> findByFlightNumber(String flightNumber);

    /**
     * Check existence before attempting an insert (avoids
     * duplicate-key exceptions on the unique index).
     */
    boolean existsByFlightNumber(String flightNumber);

    /**
     * Remove a flight record by its number (used in test teardown
     * and admin APIs).
     */
    void deleteByFlightNumber(String flightNumber);
}
