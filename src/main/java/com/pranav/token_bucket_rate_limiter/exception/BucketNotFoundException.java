package com.pranav.token_bucket_rate_limiter.exception;

public class BucketNotFoundException extends RuntimeException{
    public BucketNotFoundException(String message){
        super(message);
    }
}