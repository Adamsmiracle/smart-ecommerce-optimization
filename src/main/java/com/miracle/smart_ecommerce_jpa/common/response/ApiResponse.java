package com.miracle.smart_ecommerce_jpa.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Standardized API response wrapper.
 * All REST endpoints return responses in this format.
 *
 * @param <T> the type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private Boolean status = true;

    private String message;

    private T data;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private Integer statusCode;

    private String path;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<FieldError> errors;

    /**
     * Create a success response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(true)
                .message("Request successful")
                .data(data)
                .statusCode(200)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a success response with data and custom message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a success response with message only (no data)
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(true)
                .message(message)
                .statusCode(200)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a created response (201)
     */
    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .status(true)
                .message(message)
                .data(data)
                .statusCode(201)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .status(false)
                .message(message)
                .statusCode(statusCode)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with path
     */
    public static <T> ApiResponse<T> error(String message, int statusCode, String path) {
        return ApiResponse.<T>builder()
                .status(false)
                .message(message)
                .statusCode(statusCode)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with field errors
     */
    public static <T> ApiResponse<T> error(String message, int statusCode, List<FieldError> errors) {
        return ApiResponse.<T>builder()
                .status(false)
                .message(message)
                .statusCode(statusCode)
                .errors(errors)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Field error for validation errors
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}

