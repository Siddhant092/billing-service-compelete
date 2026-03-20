package com.broadnet.billing.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * GLOBAL EXCEPTION HANDLER
 * Centralized exception handling for the entire billing system
 * ============================================================================
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== Error Response DTO =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorResponse {

        private String timestamp;
        private int status;
        private String error;
        private String errorCode;
        private String message;
        private String path;
        private String traceId;
        private Map<String, List<String>> fieldErrors;
        private List<String> errors;
    }

    // ===== Custom Billing Exceptions =====

    /**
     * Handle all custom BillingException subclasses
     */
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<ErrorResponse> handleBillingException(
            BillingException ex,
            HttpServletRequest request) {

        log.warn("Billing exception occurred: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = HttpStatus.resolve(ex.getHttpStatus());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(ex.getHttpStatus())
                .error(status.getReasonPhrase())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handle UsageLimitExceededException with additional details
     */
    @ExceptionHandler(UsageLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleUsageLimitExceeded(
            UsageLimitExceededException ex,
            HttpServletRequest request) {

        log.warn("Usage limit exceeded: {}/{}", ex.getUsed(), ex.getLimit());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(403)
                .error("Forbidden")
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalAccessException.class)
    public ResponseEntity<ErrorResponse> handleIllegalAccessException(IllegalAccessException e) {
        log.warn("Illegal access exception occurred: {} - {}", e.getClass().getName(), e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    /**
     * Handle validation errors (MethodArgumentNotValidException)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, List<String>> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                        .add(error.getDefaultMessage())
        );

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(400)
                .error("Bad Request")
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed")
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .fieldErrors(fieldErrors)
                .build();

        log.warn("Validation error: {}", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String error = String.format(
                "Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType().getSimpleName()
        );

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(400)
                .error("Bad Request")
                .errorCode("TYPE_MISMATCH")
                .message(error)
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle optimistic locking exceptions
     */
    @ExceptionHandler(OptimisticLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(
            OptimisticLockException ex,
            HttpServletRequest request) {

        log.warn("Optimistic locking conflict: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(409)
                .error("Conflict")
                .errorCode("OPTIMISTIC_LOCKING_FAILURE")
                .message("Resource was modified concurrently. Please retry your operation.")
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle resource not found (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(404)
                .error("Not Found")
                .errorCode("ENDPOINT_NOT_FOUND")
                .message("The requested endpoint does not exist")
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(400)
                .error("Bad Request")
                .errorCode("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {

        log.warn("Illegal state: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(400)
                .error("Bad Request")
                .errorCode("INVALID_STATE")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle data access exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error occurred", ex);

        // Determine if we should expose the real message or a generic one
        String message = isProductionEnvironment()
                ? "An unexpected error occurred. Please try again later."
                : ex.getMessage();

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(500)
                .error("Internal Server Error")
                .errorCode("INTERNAL_ERROR")
                .message(message)
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle Stripe-specific exceptions
     */
    @ExceptionHandler(StripeApiException.class)
    public ResponseEntity<ErrorResponse> handleStripeApiException(
            StripeApiException ex,
            HttpServletRequest request) {

        log.error("Stripe API error: {} - {}", ex.getStripeErrorCode(), ex.getMessage());

        HttpStatus status = ex.getHttpStatus() == 402
                ? HttpStatus.PAYMENT_REQUIRED
                : HttpStatus.BAD_GATEWAY;

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(ex.getHttpStatus())
                .error(status.getReasonPhrase())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handle payment declined exceptions
     */
    @ExceptionHandler(StripePaymentDeclinedException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ResponseEntity<ErrorResponse> handlePaymentDeclined(
            StripePaymentDeclinedException ex,
            HttpServletRequest request) {

        log.warn("Payment declined with code: {}", ex.getDeclineCode());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(402)
                .error("Payment Required")
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }

    /**
     * Handle database exceptions
     */
    @ExceptionHandler(DatabaseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDatabaseException(
            DatabaseException ex,
            HttpServletRequest request) {

        log.error("Database error: {}", ex.getMessage(), ex.getCause());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(500)
                .error("Internal Server Error")
                .errorCode(ex.getErrorCode())
                .message(isProductionEnvironment()
                        ? "Database operation failed. Please try again later."
                        : ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle webhook processing exceptions
     */
    @ExceptionHandler(WebhookProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleWebhookProcessingException(
            WebhookProcessingException ex,
            HttpServletRequest request) {

        log.error("Webhook processing failed - Event: {}, ID: {}",
                ex.getEventType(), ex.getStripeEventId());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(500)
                .error("Internal Server Error")
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle transaction exceptions
     */
    @ExceptionHandler(TransactionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleTransactionException(
            TransactionException ex,
            HttpServletRequest request) {

        log.error("Transaction failed: {}", ex.getMessage(), ex.getCause());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(500)
                .error("Internal Server Error")
                .errorCode(ex.getErrorCode())
                .message("Transaction failed. Please try again.")
                .path(request.getRequestURI())
                .traceId(getTraceId(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ===== Helper Methods =====

    /**
     * Get trace ID from request headers for distributed tracing
     */
    private String getTraceId(HttpServletRequest request) {
        // Try to get from X-Trace-ID or X-Request-ID header
        String traceId = request.getHeader("X-Trace-ID");
        if (traceId == null || traceId.isEmpty()) {
            traceId = request.getHeader("X-Request-ID");
        }
        if (traceId == null || traceId.isEmpty()) {
            traceId = java.util.UUID.randomUUID().toString();
        }
        return traceId;
    }

    /**
     * Check if running in production environment
     */
    private boolean isProductionEnvironment() {
        String environment = System.getProperty("environment", "development");
        return "production".equalsIgnoreCase(environment);
    }
}

/**
 * ============================================================================
 * VALIDATION ERROR RESPONSE
 * Specific response structure for validation errors
 * ============================================================================
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class ValidationErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, List<String>> fieldErrors;
    private Map<String, String> globalErrors;

    public static ValidationErrorResponse fromBindingResult(
            org.springframework.validation.BindingResult bindingResult,
            String path) {

        Map<String, List<String>> fieldErrors = new HashMap<>();

        bindingResult.getFieldErrors().forEach(error ->
                fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                        .add(error.getDefaultMessage())
        );

        return ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(400)
                .error("Bad Request")
                .message("Validation failed")
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }
}