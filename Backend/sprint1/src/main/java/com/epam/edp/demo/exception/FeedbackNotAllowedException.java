package com.epam.edp.demo.exception;

public class FeedbackNotAllowedException extends RuntimeException {

    public FeedbackNotAllowedException(String message) {
        super(message);
    }
}
