package com.miracle.smart_ecommerce_jpa.exception;

import com.miracle.smart_ecommerce_jpa.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

// explicit import to avoid compile ordering issues


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        String details = ex.getConstraintViolations().stream()
                .map(v -> (v.getPropertyPath() == null ? "" : v.getPropertyPath().toString()) + (v.getMessage() == null ? "" : ": " + v.getMessage()))
                .collect(Collectors.joining("; "));
        ApiError err = new ApiError(ErrorCode.VALIDATION_FAILED, "Validation Failed", details, path, cid, clientIp);
        log.info("Constraint violation {}: {} - cid={}", path, details, cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Validation Failed")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private String getCorrelationId() {
        return MDC.get("correlationId");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        log.debug("No static resource found for {} - cid={}", path, cid);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.RESOURCE_NOT_FOUND, "Not Found", ex.getMessage(), path, cid, clientIp);
        log.info("Resource not found: {} - {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Not Found")
                .data(err)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.BAD_REQUEST, "Bad Request", ex.getMessage(), path, cid, clientIp);
        log.warn("Bad request {}: {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Bad Request")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.UNAUTHORIZED, "Unauthorized", ex.getMessage(), path, cid, clientIp);
        log.warn("Unauthorized access {}: {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Unauthorized")
                .data(err)
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.VALIDATION_FAILED, "Validation Failed", details, path, cid, clientIp);
        log.info("Validation failed {}: {} - cid={}", path, details, cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Validation Failed")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        String detail = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
        ApiError err = new ApiError(ErrorCode.DATA_INTEGRITY, "Data Integrity Violation", detail, path, cid, clientIp);
        log.error("Data integrity violation {}: {} - cid={}", path, detail, cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Data Integrity Violation")
                .data(err)
                .statusCode(HttpStatus.CONFLICT.value())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        ApiError err = new ApiError(ErrorCode.BAD_REQUEST, "Malformed JSON or invalid field type", detail, path, cid, clientIp);
        log.warn("Malformed JSON at {}: {} - cid={}", path, detail, cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Malformed JSON or invalid field type")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.DUPLICATE_RESOURCE, "Duplicate Resource", ex.getMessage(), path, cid, clientIp);
        log.warn("Duplicate resource at {}: {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Duplicate Resource")
                .data(err)
                .statusCode(HttpStatus.CONFLICT.value())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.INSUFFICIENT_STOCK, "Insufficient Stock", ex.getMessage(), path, cid, clientIp);
        log.warn("Insufficient stock at {}: {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Insufficient Stock")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiError>> handleAll(Exception ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.INTERNAL_ERROR, "Internal Server Error", ex.getMessage(), path, cid, clientIp);
        log.error("Unhandled exception {} - cid={}", path, cid, ex);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Internal Server Error")
                .data(err)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
