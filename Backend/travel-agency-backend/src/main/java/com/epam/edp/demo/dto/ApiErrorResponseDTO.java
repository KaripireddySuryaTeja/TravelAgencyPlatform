package com.epam.edp.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "ApiErrorResponse", description = "Standard error response payload")
public class ApiErrorResponseDTO {

    @Schema(description = "Timestamp when the error occurred", example = "2026-05-11T10:15:30Z")
    private String timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private Integer status;

    @Schema(description = "HTTP status text or error category", example = "Bad Request")
    private String error;

    @Schema(description = "Human readable error message", example = "Invalid input provided")
    private String message;

    @Schema(description = "Validation errors by field, when applicable")
    private Map<String, String> fieldErrors;

    @Schema(description = "Seconds until retry is allowed, when account is locked", example = "120")
    private Integer retryAfterSeconds;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(Integer retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
