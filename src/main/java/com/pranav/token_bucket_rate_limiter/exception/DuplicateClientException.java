package com.pranav.token_bucket_rate_limiter.exception;

public class DuplicateClientException extends RuntimeException {

    public DuplicateClientException(String message) {
        super(message);
    }
}