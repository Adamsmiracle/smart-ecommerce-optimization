package com.miracle.smart_ecommerce_security.exception;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ResourceNotFoundException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.NOT_FOUND)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof BadRequestException || ex instanceof IllegalArgumentException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof UnauthorizedException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof ForbiddenException || ex instanceof org.springframework.security.access.AccessDeniedException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.FORBIDDEN)
                    .message("Access denied. Insufficient role privileges.")
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof DuplicateResourceException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        if (ex instanceof InsufficientStockException || ex instanceof OrderProcessingException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        
        return null;
    }

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
                .timestamp(Instant.now())
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
                .timestamp(Instant.now())
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
                .timestamp(Instant.now())
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
                .timestamp(Instant.now())
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
                .timestamp(Instant.now())
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
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<ApiError>> handlePropertyReference(PropertyReferenceException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String propertyName = ex.getPropertyName();
        String entityType = extractEntityType(ex);
        
        // Provide valid sort properties based on entity type
        String validProperties = getValidSortProperties(entityType);
        
        String detail = String.format("Invalid sort property '%s' for entity type '%s'. " +
            "Valid sort properties: %s. " +
            "Use format: ?sort=property,asc or ?sort=property,desc", 
            propertyName, entityType, validProperties);
        
        ApiError err = new ApiError(ErrorCode.BAD_REQUEST, "Invalid Sort Parameter", detail, path, cid, clientIp);
        log.warn("Invalid sort parameter '{}' for entity '{}' at {}: {} - cid={}", propertyName, entityType, path, ex.getMessage(), cid);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Invalid Sort Parameter")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        // Create detailed error information
        String details = String.format("Insufficient stock for product %s (%s): available=%d, requested=%d", 
            ex.getProductName(), ex.getProductId(), ex.getAvailable(), ex.getRequested());
        
        // Add operation context if available
        if (ex.getOperation() != null) {
            details += String.format(" during %s", ex.getOperation());
        }
        
        // Add order context if available
        if (ex.getOrderId() != null) {
            details += String.format(" for order %s", ex.getOrderId());
        }
        
        // Create enhanced error data with inventory information
        Map<String, Object> errorData = Map.of(
            "code", ErrorCode.INSUFFICIENT_STOCK,
            "message", "Insufficient Stock",
            "details", details,
            "path", path,
            "correlationId", cid,
            "clientIp", clientIp,
            "inventory", Map.of(
                "productId", ex.getProductId(),
                "productName", ex.getProductName(),
                "available", ex.getAvailable(),
                "requested", ex.getRequested(),
                "shortfall", ex.getShortfall(),
                "completelyOutOfStock", ex.isCompletelyOutOfStock(),
                "hasPartialStock", ex.hasPartialStock(),
                "operation", ex.getOperation() != null ? ex.getOperation() : "unknown",
                "orderId", ex.getOrderId() != null ? ex.getOrderId() : null,
                "suggestion", ex.getSuggestionMessage()
            )
        );
        
        ApiError err = new ApiError(ErrorCode.INSUFFICIENT_STOCK, "Insufficient Stock", details, path, cid, clientIp);
        
        // Log with appropriate level based on severity
        if (ex.isCompletelyOutOfStock()) {
            log.error("Product completely out of stock {} - product: {}, available: {}, requested: {} - cid={}", 
                path, ex.getProductId(), ex.getAvailable(), ex.getRequested(), cid);
        } else {
            log.warn("Insufficient stock at {} - product: {}, available: {}, requested: {}, shortfall: {} - cid={}", 
                path, ex.getProductId(), ex.getAvailable(), ex.getRequested(), ex.getShortfall(), cid);
        }
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Insufficient Stock")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleOrderProcessing(OrderProcessingException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.ORDER_PROCESSING_ERROR, "Order Processing Error", ex.getMessage(), path, cid, clientIp);
        log.warn("Order processing error at {}: {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Order Processing Error")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<ApiError>> handlePaymentException(PaymentException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.PAYMENT_FAILED, "Payment Failed", ex.getMessage(), path, cid, clientIp);
        log.warn("Payment error at {}: {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Payment Failed")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(CartException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleCartException(CartException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.CART_ERROR, "Cart Error", ex.getMessage(), path, cid, clientIp);
        log.warn("Cart error at {}: {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Cart Error")
                .data(err)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.FORBIDDEN, "Forbidden", ex.getMessage(), path, cid, clientIp);
        log.warn("Forbidden access {}: {} - cid={}", path, ex.getMessage(), cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Forbidden")
                .data(err)
                .statusCode(HttpStatus.FORBIDDEN.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ========================================================================
    // JPA AND DATABASE EXCEPTION HANDLERS
    // ========================================================================

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleDatabaseException(DatabaseException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = String.format("Database error during %s operation", 
            ex.getOperation() != null ? ex.getOperation() : "unknown");
        if (ex.getEntityType() != null) {
            details += String.format(" on %s", ex.getEntityType());
            if (ex.getEntityId() != null) {
                details += String.format(" with ID %s", ex.getEntityId());
            }
        }
        
        ApiError err = new ApiError(ErrorCode.DATABASE_ERROR, "Database Error", details, path, cid, clientIp);
        log.error("Database exception {} - operation: {}, entity: {} - cid={}", 
            path, ex.getOperation(), ex.getEntityType(), cid, ex);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Database operation failed")
                .data(err)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(EntityStateException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleEntityStateException(EntityStateException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = String.format("Entity %s with ID %s is in state '%s' but operation requires state '%s'", 
            ex.getEntityType(), ex.getEntityId(), ex.getCurrentState(), ex.getRequiredState());
        
        ApiError err = new ApiError(ErrorCode.INVALID_STATE, "Invalid Entity State", details, path, cid, clientIp);
        log.warn("Entity state exception {} - entity: {}, current: {}, required: {} - cid={}", 
            path, ex.getEntityType(), ex.getCurrentState(), ex.getRequiredState(), cid);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Entity is in invalid state for this operation")
                .data(err)
                .statusCode(HttpStatus.CONFLICT.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(OptimisticLockingException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleOptimisticLockingException(OptimisticLockingException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = String.format("Optimistic locking failed for %s with ID %s. Expected version %d but found version %d", 
            ex.getEntityType(), ex.getEntityId(), ex.getExpectedVersion(), ex.getActualVersion());
        
        ApiError err = new ApiError(ErrorCode.OPTIMISTIC_LOCK, "Concurrent Modification", details, path, cid, clientIp);
        log.warn("Optimistic locking exception {} - entity: {}, expected: {}, actual: {} - cid={}", 
            path, ex.getEntityType(), ex.getExpectedVersion(), ex.getActualVersion(), cid);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Resource has been modified by another user")
                .data(err)
                .statusCode(HttpStatus.CONFLICT.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(jakarta.persistence.OptimisticLockException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleJpaOptimisticLockException(jakarta.persistence.OptimisticLockException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = "JPA optimistic locking failed. The entity has been modified by another transaction.";
        
        ApiError err = new ApiError(ErrorCode.OPTIMISTIC_LOCK, "Concurrent Modification", details, path, cid, clientIp);
        log.warn("JPA optimistic locking exception {} - cid={}", path, cid, ex);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Resource has been modified by another user")
                .data(err)
                .statusCode(HttpStatus.CONFLICT.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(jakarta.persistence.PessimisticLockException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleJpaPessimisticLockException(jakarta.persistence.PessimisticLockException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = "JPA pessimistic locking failed. Could not acquire lock on the resource.";
        
        ApiError err = new ApiError(ErrorCode.LOCK_TIMEOUT, "Lock Timeout", details, path, cid, clientIp);
        log.warn("JPA pessimistic locking exception {} - cid={}", path, cid, ex);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Resource is currently locked by another operation")
                .data(err)
                .statusCode(HttpStatus.CONFLICT.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleJpaEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = "JPA entity not found: " + ex.getMessage();
        
        ApiError err = new ApiError(ErrorCode.ENTITY_NOT_FOUND, "Entity Not Found", details, path, cid, clientIp);
        log.warn("JPA entity not found {} - cid={}", path, cid, ex);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Requested entity not found")
                .data(err)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(jakarta.persistence.EntityExistsException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleJpaEntityExistsException(jakarta.persistence.EntityExistsException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = "JPA entity already exists: " + ex.getMessage();
        
        ApiError err = new ApiError(ErrorCode.DUPLICATE_RESOURCE, "Entity Already Exists", details, path, cid, clientIp);
        log.warn("JPA entity already exists {} - cid={}", path, cid, ex);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Entity already exists")
                .data(err)
                .statusCode(HttpStatus.CONFLICT.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(jakarta.persistence.PersistenceException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleJpaPersistenceException(jakarta.persistence.PersistenceException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = "JPA persistence error: " + ex.getMessage();
        
        ApiError err = new ApiError(ErrorCode.DATABASE_ERROR, "Persistence Error", details, path, cid, clientIp);
        log.error("JPA persistence exception {} - cid={}", path, cid, ex);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Database persistence operation failed")
                .data(err)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(org.springframework.transaction.TransactionException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleTransactionException(org.springframework.transaction.TransactionException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        
        String details = "Transaction error: " + ex.getMessage();
        
        ApiError err = new ApiError(ErrorCode.TRANSACTION_ERROR, "Transaction Error", details, path, cid, clientIp);
        log.error("Transaction exception {} - cid={}", path, cid, ex);
        
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Transaction failed")
                .data(err)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
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
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Handles Spring Security access denied exceptions (403 Forbidden).
     * Covers both @PreAuthorize/@Secured denials (AuthorizationDeniedException)
     * and filter-chain denials (AccessDeniedException).
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest req) {
        String path = req.getRequestURI();
        String cid = getCorrelationId();
        String clientIp = getClientIp(req);
        ApiError err = new ApiError(ErrorCode.FORBIDDEN, "Access Denied",
                "You do not have permission to access this resource. Insufficient role privileges.", path, cid, clientIp);
        log.warn("ACCESS_DENIED {} — IP: {} — cid={}", path, clientIp, cid);
        ApiResponse<ApiError> body = ApiResponse.<ApiError>builder()
                .status(false)
                .message("Access denied. Insufficient role privileges.")
                .data(err)
                .statusCode(HttpStatus.FORBIDDEN.value())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Extracts entity type from PropertyReferenceException message.
     * Example message: "No property 'string' found for type 'User'"
     */
    private String extractEntityType(PropertyReferenceException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("for type")) {
            int start = message.indexOf("for type") + 9;
            int end = message.indexOf("'", start + 1);
            if (end > start) {
                return message.substring(start, end).trim();
            }
        }
        return "Unknown";
    }
    
    /**
     * Returns valid sort properties for different entity types.
     */
    private String getValidSortProperties(String entityType) {
        switch (entityType) {
            case "Address":
                return "id, addressLine, city, region, country, postalCode, addressType, isDefault, createdAt, updatedAt";
            case "User":
                return "id, firstName, lastName, emailAddress, phoneNumber, isActive, role, createdAt, updatedAt";
            case "Product":
                return "id, name, price, stockQuantity, isActive, createdAt, updatedAt";
            case "Category":
                return "id, categoryName, createdAt, updatedAt";
            case "Order":
                return "id, orderNumber, status, totalAmount, createdAt, updatedAt";
            case "Review":
                return "id, rating, title, createdAt, updatedAt";
            case "Cart":
                return "id, createdAt, updatedAt";
            case "PaymentMethod":
                return "id, type, provider, isActive, createdAt, updatedAt";
            case "ShippingMethod":
                return "id, name, cost, isActive, createdAt, updatedAt";
            default:
                return "id, createdAt, updatedAt";
        }
    }
}
