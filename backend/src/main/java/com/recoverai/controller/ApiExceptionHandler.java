package com.recoverai.controller;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.NoSuchElementException;
import com.recoverai.webhook.*;
@RestControllerAdvice public class ApiExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) ErrorResponse notFound(Exception e, HttpServletRequest r) { return error(404, "NOT_FOUND", e.getMessage(), r); }
    @ExceptionHandler({IllegalStateException.class, UnsupportedOperationException.class}) @ResponseStatus(HttpStatus.CONFLICT) ErrorResponse conflict(Exception e, HttpServletRequest r) { return error(409, "INVALID_STATE", e.getMessage(), r); }
    @ExceptionHandler(WebhookBadRequestException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) ErrorResponse webhookBadRequest(Exception e, HttpServletRequest r) { return error(400, "INVALID_WEBHOOK", e.getMessage(), r); }
    @ExceptionHandler(WebhookUnauthorizedException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED) ErrorResponse webhookUnauthorized(Exception e, HttpServletRequest r) { return error(401, "INVALID_WEBHOOK_SIGNATURE", e.getMessage(), r); }
    @ExceptionHandler(WebhookUnavailableException.class) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE) ErrorResponse webhookUnavailable(Exception e, HttpServletRequest r) { return error(503, "WEBHOOK_UNAVAILABLE", e.getMessage(), r); }
    private ErrorResponse error(int status, String code, String message, HttpServletRequest r) { return new ErrorResponse(Instant.now(), status, code, message, r.getRequestURI()); }
    record ErrorResponse(Instant timestamp, int status, String code, String message, String path) {}
}
