package com.makemytrip.flight.scheduler;

import com.makemytrip.flight.engine.MockFlightDataEngine;
import com.makemytrip.flight.model.FlightInfo;
import com.makemytrip.flight.service.FlightStatusService;
import com.makemytrip.flight.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  FlightStatusSimulator
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  Scheduled component that drives the mock real-time flight simulation.
 *
 *  Every tick (20–30 s, configured via fixedDelay):
 *  1. Iterates over all flight numbers in the data engine.
 *  2. Asks the engine to apply one probabilistic state transition.
 *  3. If the state actually changed, records history + evicts cache.
 *  4. Pushes a WebSocket notification regardless of whether the state
 *     changed (so the frontend countdown timer always refreshes).
 *
 *  The two @Scheduled methods use different intervals to stagger updates
 *  and make the simulation feel more organic:
 *  - Primary tick: every 20 s  (most flights updated here)
 *  - Secondary tick: every 30 s (remaining flights get a slower cycle)
 *
 *  In practice, Spring's scheduler runs single-threaded by default; the
 *  pool size in application.properties (spring.task.scheduling.pool.size=5)
 *  prevents the secondary tick from blocking if the primary runs long.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class FlightStatusSimulator {

    private final MockFlightDataEngine dataEngine;
    private final FlightStatusService flightStatusService;
    private final NotificationService notificationService;

    // ────────────────────────────────────────────────────────────────────
    //  Primary simulation tick – every 20 seconds
    // ────────────────────────────────────────────────────────────────────

    /**
     * Updates the first half of the flight list (index 0, 2, 4 …).
     * Using {@code fixedDelay} (not fixedRate) ensures we never pile up
     * tasks if an iteration takes longer than the interval.
     *
     * initialDelay = 15 s gives the application time to fully start and
     * seed the data engine before the first tick fires.
     */
    @Scheduled(fixedDelayString = "20000", initialDelayString = "15000")
    public void primaryTick() {
        runSimulationForFlights(0, 2); // every even-indexed flight
    }

    // ────────────────────────────────────────────────────────────────────
    //  Secondary simulation tick – every 30 seconds
    // ────────────────────────────────────────────────────────────────────

    /**
     * Updates the second half of the flight list (index 1, 3, 5 …).
     * Staggering updates avoids a thundering-herd of WebSocket messages
     * all arriving on the frontend at exactly the same instant.
     */
    @Scheduled(fixedDelayString = "30000", initialDelayString = "25000")
    public void secondaryTick() {
        runSimulationForFlights(1, 2); // every odd-indexed flight
    }

    // ────────────────────────────────────────────────────────────────────
    //  Core simulation loop
    // ────────────────────────────────────────────────────────────────────

    /**
     * Iterates over the flights whose array index satisfies
     * {@code (index % modulus) == remainder} and processes each one.
     *
     * @param remainder select flights where (i % modulus) == remainder
     * @param modulus   stride; 2 splits the list in half
     */
    private void runSimulationForFlights(int remainder, int modulus) {
        String[] flightNumbers = dataEngine.getAllFlightNumbers()
                .toArray(new String[0]);

        int processed = 0;

        for (int i = 0; i < flightNumbers.length; i++) {
            if (i % modulus != remainder) continue;

            String fn = flightNumbers[i];

            // Capture the state BEFORE mutation so we can diff it
            FlightInfo before = dataEngine.getFlightInfo(fn).orElse(null);

            // Ask the engine to apply a random state transition
            Optional<FlightInfo> afterOpt = dataEngine.applyRandomUpdate(fn);
            if (afterOpt.isEmpty()) continue;

            FlightInfo after = afterOpt.get();

            // Only record history if something actually changed
            boolean stateChanged = hasSignificantChange(before, after);
            if (stateChanged) {
                flightStatusService.recordHistoryAndEvictCache(before, after);
            }

            // Always push a WebSocket notification (even just for ETA refresh)
            notificationService.notifyStatusChange(before, after);
            processed++;
        }

        if (processed > 0) {
            log.debug("Simulation tick processed {} flights (remainder={}/{})",
                      processed, remainder, modulus);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    //  Helper
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the two snapshots differ in a way that warrants
     * recording a history entry (status or delay changed).
     *
     * Trivial timestamp-only refreshes are intentionally excluded to
     * avoid polluting the history collection.
     */
    private boolean hasSignificantChange(FlightInfo before, FlightInfo after) {
        if (before == null) return true;
        return before.getStatus() != after.getStatus()
                || before.getDelayMinutes() != after.getDelayMinutes();
    }
}
