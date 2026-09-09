package com.example.storefront.common;

/**
 * Small hierarchy of business-level failures. The web layer maps each to an HTTP
 * status in {@link GlobalExceptionHandler}; the messaging layer decides whether
 * to retry based on the same distinction.
 */
public final class DomainExceptions {

    private DomainExceptions() {
    }

    /** Requested entity does not exist -> HTTP 404. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** A domain rule was violated (e.g. empty order) -> HTTP 422. Not retryable. */
    public static class BusinessRuleException extends RuntimeException {
        public BusinessRuleException(String message) {
            super(message);
        }
    }

    /** Optimistic-lock / duplicate / state-conflict -> HTTP 409. Often retryable. */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }
}
