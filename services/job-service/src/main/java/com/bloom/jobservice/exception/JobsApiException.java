package com.bloom.jobservice.exception;

public class JobsApiException extends RuntimeException {
    public JobsApiException(String message) {
        super(message);
    }

    public JobsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}