package com.pranav.token_bucket_rate_limiter.service.interfaces;

import com.pranav.token_bucket_rate_limiter.dto.request.RateLimitRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.RateLimitResponse;

public interface RateLimiterService {
    RateLimitResponse checkRateLimit(RateLimitRequest request);
}