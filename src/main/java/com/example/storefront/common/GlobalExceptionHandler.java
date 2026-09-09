package com.example.storefront.common;

import com.example.storefront.common.DomainExceptions.BusinessRuleException;
import com.example.storefront.common.DomainExceptions.ConflictException;
import com.example.storefront.common.DomainExceptions.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into RFC-9457 {@link ProblemDetail} responses so every
 * error the API returns has the same JSON shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail onNotFound(NotFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail onBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    @ExceptionHandler({ConflictException.class, OptimisticLockingFailureException.class})
    public ProblemDetail onConflict(Exception ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail onAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return problem(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Request validation failed.", req);
        body.setProperty("violations", violations);
        return body;
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    private ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }
}
