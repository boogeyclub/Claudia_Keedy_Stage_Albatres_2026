package cm.odigital.serviceconnectmarket.auth.api;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cm.odigital.serviceconnectmarket.auth.api.dto.ApiErrorResponse;
import cm.odigital.serviceconnectmarket.auth.domain.AuthException;

@RestControllerAdvice
public class AuthExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthExceptionHandler.class);

    private final Clock clock;

    public AuthExceptionHandler(Clock authenticationClock) {
        this.clock = authenticationClock;
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(AuthException exception) {
        logRejection(exception.getStatus(), exception.getCode());
        return ResponseEntity.status(exception.getStatus())
            .body(error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
            .map(ObjectError::getDefaultMessage)
            .findFirst()
            .orElse("The request is not valid.");
        logRejection(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED");
        return ResponseEntity.badRequest().body(error("REQUEST_VALIDATION_FAILED", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        logRejection(HttpStatus.BAD_REQUEST, "REQUEST_BODY_INVALID");
        return ResponseEntity.badRequest().body(error("REQUEST_BODY_INVALID", "The request body is not valid."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        // Used by several protected workflows, including administrator configuration records.
        // Do not expose database constraint names or imply that an account identity was involved.
        logRejection(HttpStatus.CONFLICT, "PROTECTED_DATA_CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(error("PROTECTED_DATA_CONFLICT", "The requested change conflicts with existing protected data."));
    }

    /**
     * Keeps JDBC failures out of the browser while retaining enough non-sensitive diagnostics in
     * the server log to identify a missing/outdated gu schema, an unavailable database, or a
     * database permission issue. SQL values and exception messages are intentionally not logged.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException exception) {
        LOGGER.error(
            "event=api.request.data-access-failed errorCode=DATA_ACCESS_UNAVAILABLE exceptionType={} sqlState={} origin={}",
            exception.getClass().getName(),
            sqlStateFor(exception),
            applicationOrigin(exception)
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(error("DATA_ACCESS_UNAVAILABLE", "The protected data service is temporarily unavailable."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        LOGGER.error(
            "event=api.request.failed errorCode=UNEXPECTED_ERROR exceptionType={} rootCauseType={} origin={}",
            exception.getClass().getName(),
            rootCause(exception).getClass().getName(),
            applicationOrigin(exception)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error("UNEXPECTED_ERROR", "The request could not be completed."));
    }

    private void logRejection(HttpStatus status, String errorCode) {
        LOGGER.warn("event=api.request.rejected status={} errorCode={}", status.value(), errorCode);
    }

    private String sqlStateFor(DataAccessException exception) {
        Throwable specificCause = exception.getMostSpecificCause();
        return specificCause instanceof SQLException sqlException && sqlException.getSQLState() != null
            ? sqlException.getSQLState()
            : "[unavailable]";
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String applicationOrigin(Throwable exception) {
        return Arrays.stream(exception.getStackTrace())
            .filter(element -> element.getClassName().startsWith("cm.odigital.serviceconnectmarket"))
            .findFirst()
            .map(element -> element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber())
            .orElse("[external]");
    }

    private ApiErrorResponse error(String code, String message) {
        return new ApiErrorResponse(code, message, clock.instant());
    }
}
