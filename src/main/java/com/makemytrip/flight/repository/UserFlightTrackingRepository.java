package com.makemytrip.flight.repository;

import com.makemytrip.flight.model.UserFlightTracking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for {@link UserFlightTracking} documents.
 */
@Repository
public interface UserFlightTrackingRepository
        extends MongoRepository<UserFlightTracking, String> {

    /**
     * All flights currently tracked by a given user.
     *
     * @param userId identifier of the user
     * @return list of tracking entries (may be empty)
     */
    List<UserFlightTracking> findByUserId(String userId);

    /**
     * Look up whether a specific user is already tracking a flight.
     * Used to enforce the "no duplicate tracking" business rule.
     */
    Optional<UserFlightTracking> findByUserIdAndFlightNumber(
            String userId, String flightNumber);

    /**
     * Remove a tracking entry by user + flight pair.
     * Matches the DELETE /api/flights/untrack endpoint.
     */
    void deleteByUserIdAndFlightNumber(String userId, String flightNumber);

    /**
     * Check if a user is already tracking the given flight.
     */
    boolean existsByUserIdAndFlightNumber(String userId, String flightNumber);
}
