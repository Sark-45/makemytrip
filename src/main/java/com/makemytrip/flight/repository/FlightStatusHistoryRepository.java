package com.makemytrip.flight.repository;

import com.makemytrip.flight.model.FlightStatusHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for {@link FlightStatusHistory} documents.
 *
 * History entries are append-only; there is no update path.
 */
@Repository
public interface FlightStatusHistoryRepository
        extends MongoRepository<FlightStatusHistory, String> {

    /**
     * Return the complete history for a flight, ordered oldest-first.
     * Used to render the status-change timeline in the frontend.
     *
     * @param flightNumber IATA flight number
     * @return ordered list of history entries (may be empty)
     */
    List<FlightStatusHistory> findByFlightNumberOrderByRecordedAtAsc(
            String flightNumber);

    /**
     * Return the most recent N history entries for a flight.
     * Useful for "last 5 updates" summary panel.
     */
    List<FlightStatusHistory> findTop5ByFlightNumberOrderByRecordedAtDesc(
            String flightNumber);
}
