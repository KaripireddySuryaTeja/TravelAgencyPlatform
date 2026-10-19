package com.epam.edp.demo.exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String FIELD_EMAIL     = "email";
    private static final String FIELD_PASSWORD  = "password";
    private static final String INVALID_CREDENTIALS_MSG = "Invalid email or password";
    private static final String KEY_TIMESTAMP         = "timestamp";
    private static final String KEY_STATUS            = "status";
    private static final String KEY_ERROR             = "error";
    private static final String KEY_MESSAGE           = "message";
    private static final String KEY_MODERATION_STATUS = "moderationStatus";
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(body(
                HttpStatus.BAD_REQUEST, "Invalid input provided", fieldErrors));
    }
    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleWeakPassword(WeakPasswordException ex) {
        return ResponseEntity.badRequest().body(body(
                HttpStatus.BAD_REQUEST, ex.getMessage(),
                Map.of(FIELD_PASSWORD, ex.getMessage())));
    }
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(
                HttpStatus.CONFLICT, "Email already exists",
                Map.of(FIELD_EMAIL, "An account with this email already exists")));
    }
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCreds(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(
                HttpStatus.UNAUTHORIZED, "Wrong password or email",
                Map.of(
                        FIELD_EMAIL,    INVALID_CREDENTIALS_MSG,
                        FIELD_PASSWORD, INVALID_CREDENTIALS_MSG
                )));
    }
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleLocked(AccountLockedException ex) {
        Map<String, Object> b = simpleBody(HttpStatus.LOCKED, "Account locked",
                "Your account is temporarily locked due to multiple failed login attempts. Please try again later.");
        b.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.LOCKED).body(b);
    }
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(UnauthenticatedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(simpleBody(HttpStatus.UNAUTHORIZED, "Unauthenticated", ex.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(simpleBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()));
    }
    @ExceptionHandler(FeedbackNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFeedbackNotFound(FeedbackNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(simpleBody(HttpStatus.NOT_FOUND, "Feedback not found", ex.getMessage()));
    }
    @ExceptionHandler(FeedbackNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleFeedbackNotAllowed(FeedbackNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(simpleBody(HttpStatus.UNPROCESSABLE_ENTITY, "Feedback not allowed", ex.getMessage()));
    }
    @ExceptionHandler(FeedbackRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleFeedbackRejected(FeedbackRejectedException ex) {
        log.warn("Feedback rejected by AI moderation: {}", ex.getReason());
        Map<String, Object> b = simpleBody(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Feedback rejected",
                "Your feedback was rejected because it violates our community guidelines.");
        b.put(KEY_MODERATION_STATUS, "FLAGGED");
        b.put("moderationReason", ex.getReason());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(b);
    }
    @ExceptionHandler(ContentModerationException.class)
    public ResponseEntity<Map<String, Object>> handleContentModeration(ContentModerationException ex) {
        String moderationStatus = ex.getModerationStatus();
        log.warn("ContentModerationException [{}]: {} | user='{}'",
                moderationStatus, ex.getTechnicalReason(), ex.getUserMessage());
        if ("NEEDS_EDIT".equals(moderationStatus)) {
            Map<String, Object> b = simpleBody(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Content requires revision",
                    ex.getUserMessage());
            b.put(KEY_MODERATION_STATUS, "NEEDS_EDIT");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(b);
        }
        Map<String, Object> b = simpleBody(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Moderation service unavailable",
                ex.getUserMessage());
        b.put(KEY_MODERATION_STATUS, moderationStatus);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(b);
    }
    @ExceptionHandler(DocumentValidationException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentValidation(DocumentValidationException ex) {
        return ResponseEntity.badRequest()
                .body(simpleBody(HttpStatus.BAD_REQUEST, "Invalid document", ex.getMessage()));
    }
    @ExceptionHandler(MalwareDetectedException.class)
    public ResponseEntity<Map<String, Object>> handleMalwareDetected(MalwareDetectedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(simpleBody(HttpStatus.UNPROCESSABLE_ENTITY, "Malware detected", ex.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return ResponseEntity.status(status)
                .body(simpleBody(status, status.getReasonPhrase(), message));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled RuntimeException", ex);
        return ResponseEntity.badRequest()
                .body(simpleBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()));
    }
    private static Map<String, Object> simpleBody(HttpStatus status, String error, String message) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put(KEY_TIMESTAMP, Instant.now());
        b.put(KEY_STATUS, status.value());
        b.put(KEY_ERROR, error);
        b.put(KEY_MESSAGE, message);
        return b;
    }
    private static Map<String, Object> body(HttpStatus status, String message, Map<String, String> fieldErrors) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put(KEY_TIMESTAMP, Instant.now());
        b.put(KEY_STATUS, status.value());
        b.put(KEY_ERROR, status.getReasonPhrase());
        b.put(KEY_MESSAGE, message);
        b.put("fieldErrors", fieldErrors);
        return b;
    }
}