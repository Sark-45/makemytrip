package com.makemytrip.flight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures the STOMP-over-WebSocket message broker.
 *
 * Architecture overview:
 * <pre>
 *   Browser  ──── ws://host/ws ────►  SockJS Endpoint
 *                                           │
 *                      ┌────────────────────┘
 *                      ▼
 *             In-Process SimpleBroker
 *             /topic/flights/{flightNumber}   ← server → clients
 *             /app/**                         ← clients → server (if needed)
 * </pre>
 *
 * We use the simple in-process broker which is perfectly sufficient for a
 * single-node deployment.  For a multi-node production cluster, replace
 * {@code enableSimpleBroker} with an external broker relay pointing at
 * RabbitMQ or ActiveMQ with STOMP support.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register the STOMP endpoint the browser connects to.
     *
     * SockJS fallback is enabled so that browsers without native WebSocket
     * support (and tools such as Postman) can still communicate using
     * long-polling or iframe transports.
     *
     * {@code setAllowedOriginPatterns("*")} keeps the demo accessible from
     * any origin; restrict this in production to your frontend domain.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
            .addEndpoint("/ws")              // ws://localhost:8080/ws
            .setAllowedOriginPatterns("*")   // CORS – tighten in prod
            .withSockJS();                   // fallback transport
    }

    /**
     * Configure the in-process message broker.
     *
     * <ul>
     *   <li>{@code /topic} – broadcast (1 → many); used by the flight
     *       simulator to fan-out status updates.</li>
     *   <li>{@code /app}   – client-to-server destination prefix (reserved
     *       for future @MessageMapping endpoints).</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Topics the server broadcasts to
        registry.enableSimpleBroker("/topic");

        // Prefix for messages sent FROM the client TO the server
        registry.setApplicationDestinationPrefixes("/app");
    }
}
