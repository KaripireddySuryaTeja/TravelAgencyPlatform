package com.epam.edp.demo.exception;

public class AccountLockedException extends RuntimeException {
    private final long retryAfterSeconds;

    public AccountLockedException(long retryAfterSeconds) {
        super("Account is temporarily locked");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
