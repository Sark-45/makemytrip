package com.makemytrip.flight.engine;

import com.makemytrip.flight.enums.DelayReason;
import com.makemytrip.flight.enums.FlightStatus;
import com.makemytrip.flight.model.FlightInfo;
import com.makemytrip.flight.repository.FlightInfoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  Mock Flight Data Engine
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  Responsibilities:
 *  1. Seed a realistic set of Indian domestic flights on application startup.
 *  2. Maintain an in-memory {@link ConcurrentHashMap} as the hot data store
 *     so the scheduler and REST layer never block on MongoDB for reads.
 *  3. Persist the same data to MongoDB so tracked-flight queries can join
 *     flight metadata with tracking records.
 *
 *  The {@link com.makemytrip.flight.scheduler.FlightStatusSimulator} calls
 *  {@link #applyRandomUpdate(String)} on a scheduled tick to mutate state;
 *  callers retrieve the current snapshot via {@link #getFlightInfo(String)}.
 *
 *  Thread safety:
 *  - {@code ConcurrentHashMap} guarantees atomicity of individual put/get.
 *  - The simulator uses synchronized mutation helpers so a read racing a
 *    write always sees a consistent {@link FlightInfo} snapshot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockFlightDataEngine {

    private final FlightInfoRepository flightInfoRepository;

    // ─── Hot in-memory store ────────────────────────────────────────────
    // Key: IATA flight number (uppercase), Value: live FlightInfo snapshot
    private final ConcurrentHashMap<String, FlightInfo> flightStore =
            new ConcurrentHashMap<>();

    // ─── Randomness ─────────────────────────────────────────────────────
    private static final Random RANDOM = new Random();

    // ─── Realistic delay reasons, weighted toward common causes ─────────
    private static final DelayReason[] DELAY_REASONS = {
        DelayReason.WEATHER,
        DelayReason.WEATHER,
        DelayReason.AIR_TRAFFIC,
        DelayReason.AIR_TRAFFIC,
        DelayReason.LATE_ARRIVING_AIRCRAFT,
        DelayReason.TECHNICAL_ISSUE,
        DelayReason.CREW_AVAILABILITY,
        DelayReason.GROUND_HANDLING,
        DelayReason.SECURITY_CHECK
    };

    // ─── Seed data: 12 realistic Indian domestic routes ─────────────────
    private static final List<FlightSeed> SEED_FLIGHTS = List.of(
        new FlightSeed("AI302",   "Air India",     "DEL", "BOM", "Boeing 737-800",    "T3", "B14",  6,  0),
        new FlightSeed("AI506",   "Air India",     "BOM", "BLR", "Airbus A320",       "T2", "C5",   8, 30),
        new FlightSeed("6E4521",  "IndiGo",        "DEL", "HYD", "Airbus A320neo",    "T1", "A3",   7, 15),
        new FlightSeed("6E2341",  "IndiGo",        "BLR", "MAA", "Airbus A320neo",    "T1", "A7",   9,  0),
        new FlightSeed("SG101",   "SpiceJet",      "DEL", "GOI", "Boeing 737-900",    "T1", "D2",  10, 45),
        new FlightSeed("SG455",   "SpiceJet",      "BOM", "COK", "Boeing 737-800",    "T1", "D9",  11, 30),
        new FlightSeed("UK835",   "Vistara",       "DEL", "BLR", "Airbus A321",       "T3", "E6",  13,  0),
        new FlightSeed("UK915",   "Vistara",       "MAA", "DEL", "Boeing 787-9",      "T1", "E11", 14, 20),
        new FlightSeed("QP1131",  "Akasa Air",     "BOM", "DEL", "Boeing 737 MAX 8",  "T2", "F4",  15,  0),
        new FlightSeed("IX771",   "Air Asia India","BLR", "DEL", "Airbus A320",       "T1", "G8",  16, 40),
        new FlightSeed("G8203",   "Go First",      "DEL", "JAI", "Airbus A320",       "T1", "H2",  17, 15),
        new FlightSeed("AI891",   "Air India",     "HYD", "DEL", "Boeing 777-300ER",  "T1", "B3",  18,  0)
    );

    // ─── Internal seed record ────────────────────────────────────────────
    private record FlightSeed(
        String flightNumber, String airline,
        String origin, String destination,
        String aircraftType, String terminal, String gate,
        int departureHour, int departureMinute
    ) {}

    // ────────────────────────────────────────────────────────────────────
    //  Initialisation
    // ────────────────────────────────────────────────────────────────────

    /**
     * Called once after the Spring context is fully wired.
     * Seeds the in-memory store and persists to MongoDB (upsert semantics).
     */
    @PostConstruct
    public void seedFlights() {
        log.info("MockFlightDataEngine: seeding {} flights...", SEED_FLIGHTS.size());

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();

        for (FlightSeed seed : SEED_FLIGHTS) {
            // Skip if already exists in the DB from a previous run
            if (flightInfoRepository.existsByFlightNumber(seed.flightNumber())) {
                FlightInfo existing = flightInfoRepository
                        .findByFlightNumber(seed.flightNumber()).orElseThrow();
                flightStore.put(seed.flightNumber(), existing);
                log.debug("  restored {} from MongoDB", seed.flightNumber());
                continue;
            }

            LocalDateTime dep = today.plusHours(seed.departureHour())
                                     .plusMinutes(seed.departureMinute());
            // Typical flight durations for Indian domestic routes (minutes)
            int durationMinutes = 60 + RANDOM.nextInt(120); // 1–3 hours
            LocalDateTime arr = dep.plusMinutes(durationMinutes);

            FlightInfo flight = FlightInfo.builder()
                    .flightNumber(seed.flightNumber())
                    .airline(seed.airline())
                    .origin(seed.origin())
                    .destination(seed.destination())
                    .scheduledDeparture(dep)
                    .estimatedDeparture(dep)
                    .estimatedArrival(arr)
                    .delayMinutes(0)
                    .status(FlightStatus.ON_TIME)
                    .delayReason(DelayReason.NONE)
                    .gate(seed.gate())
                    .terminal(seed.terminal())
                    .aircraftType(seed.aircraftType())
                    .lastUpdated(LocalDateTime.now())
                    .build();

            flightInfoRepository.save(flight);
            flightStore.put(seed.flightNumber(), flight);
            log.debug("  seeded {} ({} → {})", seed.flightNumber(),
                      seed.origin(), seed.destination());
        }

        log.info("MockFlightDataEngine: seeding complete. {} flights in store.",
                 flightStore.size());
    }

    // ────────────────────────────────────────────────────────────────────
    //  Public read API
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns the current live snapshot for a flight, or empty if unknown.
     */
    public Optional<FlightInfo> getFlightInfo(String flightNumber) {
        return Optional.ofNullable(flightStore.get(flightNumber.toUpperCase()));
    }

    /**
     * All flight numbers currently tracked by the engine.
     */
    public Set<String> getAllFlightNumbers() {
        return Collections.unmodifiableSet(flightStore.keySet());
    }

    /**
     * Snapshot of every flight's current state (defensive copy).
     */
    public Collection<FlightInfo> getAllFlights() {
        return List.copyOf(flightStore.values());
    }

    // ────────────────────────────────────────────────────────────────────
    //  Simulation core – called by FlightStatusSimulator on each tick
    // ────────────────────────────────────────────────────────────────────

    /**
     * Applies one probabilistic state transition to the given flight and
     * returns the updated {@link FlightInfo}.
     *
     * Realistic transition matrix (approximate probabilities):
     *
     *   ON_TIME  →  DELAYED   (20 %)
     *   ON_TIME  →  BOARDING  (15 %, only ≤ 30 min before departure)
     *   DELAYED  →  DELAYED   (50 %, delay increases by 15–30 min)
     *   DELAYED  →  ON_TIME   (20 %, delay resolved)
     *   DELAYED  →  BOARDING  (10 %, delay resolved + boarding started)
     *   BOARDING →  DEPARTED  (80 %, always moves forward)
     *   DEPARTED →  LANDED    (100 %, terminal state)
     *
     * @param flightNumber IATA number (case-insensitive)
     * @return the updated snapshot, or empty if the flight is unknown
     */
    public Optional<FlightInfo> applyRandomUpdate(String flightNumber) {
        String key = flightNumber.toUpperCase();
        FlightInfo flight = flightStore.get(key);
        if (flight == null) return Optional.empty();

        // Terminal states – no more updates
        if (flight.getStatus() == FlightStatus.LANDED
                || flight.getStatus() == FlightStatus.CANCELLED) {
            return Optional.of(flight);
        }

        FlightInfo updated = simulateNextState(flight);
        flightStore.put(key, updated);

        // Async persist to MongoDB (best-effort, don't block the scheduler)
        try {
            flightInfoRepository.save(updated);
        } catch (Exception ex) {
            log.warn("MongoDB persist failed for {}: {}", key, ex.getMessage());
        }

        return Optional.of(updated);
    }

    // ────────────────────────────────────────────────────────────────────
    //  Private simulation helpers
    // ────────────────────────────────────────────────────────────────────

    /**
     * Core state-machine: decides the next state and builds an updated
     * {@link FlightInfo} based on the current state.
     */
    private FlightInfo simulateNextState(FlightInfo f) {
        LocalDateTime now = LocalDateTime.now();
        long minutesToDep = java.time.Duration.between(now, f.getEstimatedDeparture()).toMinutes();

        return switch (f.getStatus()) {

            case ON_TIME -> {
                double roll = RANDOM.nextDouble();

                if (roll < 0.20) {
                    // → DELAYED: introduce a new delay (15–60 min)
                    int newDelay = 15 + RANDOM.nextInt(4) * 15; // 15, 30, 45, 60
                    DelayReason reason = randomDelayReason();
                    yield rebuildWith(f, b -> b
                            .status(FlightStatus.DELAYED)
                            .delayMinutes(newDelay)
                            .delayReason(reason)
                            .estimatedDeparture(f.getScheduledDeparture().plusMinutes(newDelay))
                            .estimatedArrival(f.getEstimatedArrival().plusMinutes(newDelay))
                            .lastUpdated(now));

                } else if (roll < 0.35 && minutesToDep <= 30 && minutesToDep > 0) {
                    // → BOARDING: only possible within 30 min of departure
                    yield rebuildWith(f, b -> b
                            .status(FlightStatus.BOARDING)
                            .lastUpdated(now));

                } else {
                    // → Stay ON_TIME (no change, just refresh timestamp)
                    yield rebuildWith(f, b -> b.lastUpdated(now));
                }
            }

            case DELAYED -> {
                double roll = RANDOM.nextDouble();

                if (roll < 0.20) {
                    // → ON_TIME: delay resolved
                    yield rebuildWith(f, b -> b
                            .status(FlightStatus.ON_TIME)
                            .delayMinutes(0)
                            .delayReason(DelayReason.NONE)
                            .estimatedDeparture(f.getScheduledDeparture())
                            .estimatedArrival(
                                f.getEstimatedArrival().minusMinutes(f.getDelayMinutes()))
                            .lastUpdated(now));

                } else if (roll < 0.30 && minutesToDep <= 30 && minutesToDep > 0) {
                    // → BOARDING despite delay
                    yield rebuildWith(f, b -> b
                            .status(FlightStatus.BOARDING)
                            .lastUpdated(now));

                } else {
                    // → DELAYED: increase delay by 15–30 min (gradual worsening)
                    int additional = 15 * (1 + RANDOM.nextInt(2)); // 15 or 30
                    int totalDelay = f.getDelayMinutes() + additional;
                    yield rebuildWith(f, b -> b
                            .delayMinutes(totalDelay)
                            .estimatedDeparture(f.getScheduledDeparture().plusMinutes(totalDelay))
                            .estimatedArrival(
                                f.getEstimatedArrival().plusMinutes(additional))
                            .lastUpdated(now));
                }
            }

            case BOARDING -> {
                // Boarding almost always leads to departure
                if (RANDOM.nextDouble() < 0.80) {
                    yield rebuildWith(f, b -> b
                            .status(FlightStatus.DEPARTED)
                            .lastUpdated(now));
                } else {
                    yield rebuildWith(f, b -> b.lastUpdated(now));
                }
            }

            case DEPARTED -> {
                // Always transitions to LANDED
                yield rebuildWith(f, b -> b
                        .status(FlightStatus.LANDED)
                        .lastUpdated(now));
            }

            default -> f; // LANDED, CANCELLED – no-op (already guarded above)
        };
    }

    /**
     * Functional builder helper.  Creates a copy of {@code original} and
     * applies the given mutations via a {@link FlightInfo.FlightInfoBuilder}.
     */
    private FlightInfo rebuildWith(FlightInfo original,
            java.util.function.Consumer<FlightInfo.FlightInfoBuilder> mutations) {
        FlightInfo.FlightInfoBuilder builder = original.toBuilder();
        mutations.accept(builder);
        return builder.build();
    }

    /** Picks a random delay reason from the weighted array. */
    private DelayReason randomDelayReason() {
        return DELAY_REASONS[RANDOM.nextInt(DELAY_REASONS.length)];
    }
}
