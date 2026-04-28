package com.makemytrip.flight.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Generic API response envelope.
 *
 * Every REST endpoint in the flight module wraps its payload in this
 * class to give clients a consistent shape:
 *
 * <pre>
 * {
 *   "success": true,
 *   "message": "Flight status retrieved",
 *   "data": { ... },
 *   "timestamp": "2025-06-15T07:42:30"
 * }
 * </pre>
 *
 * {@code @JsonInclude(NON_NULL)} suppresses the {@code data} key
 * when the response carries no payload (e.g. 204 responses).
 *
 * @param <T> type of the payload carried in {@code data}
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;

    /** The actual payload; null for error or no-content responses. */
    private T data;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ─── Factory helpers ────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
