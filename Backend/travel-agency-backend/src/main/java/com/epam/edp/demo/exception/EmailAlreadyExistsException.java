package com.epam.edp.demo.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }

    public EmailAlreadyExistsException(String email, Throwable cause) {
        super("Email already registered: " + email, cause);
    }
}
