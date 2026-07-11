package com.pranav.token_bucket_rate_limiter.exception;



public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(String message) {
        super(message);
    }
}