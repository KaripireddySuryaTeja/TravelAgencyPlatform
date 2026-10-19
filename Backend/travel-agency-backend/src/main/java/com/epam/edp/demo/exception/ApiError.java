package com.epam.edp.demo.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, fieldErrors);
    }
}
