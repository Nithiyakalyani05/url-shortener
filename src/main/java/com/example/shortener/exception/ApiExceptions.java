package com.example.shortener.exception;

/** Domain exceptions, each mapped to an HTTP status in {@link GlobalExceptionHandler}. */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String code) {
            super("No short link found for code '" + code + "'");
        }
    }

    public static class ExpiredException extends RuntimeException {
        public ExpiredException(String code) {
            super("Short link '" + code + "' has expired");
        }
    }

    public static class AliasTakenException extends RuntimeException {
        public AliasTakenException(String alias) {
            super("Alias '" + alias + "' is already in use");
        }
    }

    public static class InvalidUrlException extends RuntimeException {
        public InvalidUrlException(String message) {
            super(message);
        }
    }
}
