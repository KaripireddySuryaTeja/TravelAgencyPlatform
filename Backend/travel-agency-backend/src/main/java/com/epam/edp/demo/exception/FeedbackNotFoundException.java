package com.epam.edp.demo.exception;

public class FeedbackNotFoundException extends RuntimeException {

    public FeedbackNotFoundException(String message) {
        super(message);
    }
}
