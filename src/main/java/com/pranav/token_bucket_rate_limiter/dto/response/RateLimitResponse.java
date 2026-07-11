package com.pranav.token_bucket_rate_limiter.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RateLimitResponse {
    private boolean allowed;

    private Integer remainingTokens;

    private Long retryAfter;

    private LocalDateTime timestamp;
}