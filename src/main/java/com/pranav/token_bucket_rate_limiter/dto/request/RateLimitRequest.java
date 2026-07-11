package com.pranav.token_bucket_rate_limiter.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for checking the rate limit of an API client.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateLimitRequest {

    @NotBlank(message = "API Key cannot be blank")
    private String apiKey;
}