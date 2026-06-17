package com.hospital.booking.exception;

import com.hospital.booking.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Auth Errors ───────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<String>> handleBadCredentials(BadCredentialsException ex, WebRequest req) {
        log.warn("Bad credentials attempt: {}", req.getDescription(false));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Invalid email or password", req.getDescription(false)));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<String>> handleDisabled(DisabledException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Account is disabled. Please contact support.", req.getDescription(false)));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<String>> handleLocked(LockedException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Account is locked. Please contact support.", req.getDescription(false)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDenied(AccessDeniedException ex, WebRequest req) {
        log.warn("Access denied: {} -> {}", req.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("You do not have permission to perform this action.", req.getDescription(false)));
    }

    // ── Validation Errors ─────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String field = ((FieldError) e).getField();
            errors.put(field, e.getDefaultMessage());
        });
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
            .body(ApiResponse.<Map<String, String>>builder()
                .success(false).message("Validation failed: please check the highlighted fields.")
                .data(errors).build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingParam(MissingServletRequestParameterException ex, WebRequest req) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Required parameter '" + ex.getParameterName() + "' is missing.", req.getDescription(false)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest req) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'.", req.getDescription(false)));
    }

    // ── DB / Data Errors ──────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest req) {
        log.error("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("Duplicate entry")) {
            if (msg.contains("email")) return ResponseEntity.badRequest()
                .body(ApiResponse.error("This email address is already registered.", req.getDescription(false)));
            if (msg.contains("phone")) return ResponseEntity.badRequest()
                .body(ApiResponse.error("This phone number is already registered.", req.getDescription(false)));
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Duplicate entry: this record already exists.", req.getDescription(false)));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("Data conflict. Please check your input and try again.", req.getDescription(false)));
    }

    // ── 404 ───────────────────────────────────────────────────────────────────

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(NoHandlerFoundException ex, WebRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Endpoint not found: " + ex.getRequestURL(), req.getDescription(false)));
    }

    // ── Business Logic Errors ─────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArg(IllegalArgumentException ex, WebRequest req) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ex.getMessage(), req.getDescription(false)));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntime(RuntimeException ex, WebRequest req) {
        String msg = ex.getMessage();
        log.error("Runtime exception at {}: {}", req.getDescription(false), msg);
        // Surface known business errors to the client as 400
        if (msg != null && (
                msg.startsWith("Appointment not found") ||
                msg.startsWith("Doctor not found") ||
                msg.startsWith("Patient not found") ||
                msg.startsWith("User not found") ||
                msg.startsWith("Access denied") ||
                msg.startsWith("Slot not available") ||
                msg.contains("already") ||
                msg.contains("cannot") ||
                msg.contains("Invalid"))) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(msg, req.getDescription(false)));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred. Please try again.", req.getDescription(false)));
    }

    // ── Catch-All ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneral(Exception ex, WebRequest req) {
        log.error("Unhandled exception at {}: {}", req.getDescription(false), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal server error. Our team has been notified.", req.getDescription(false)));
    }
}
