package com.pranav.token_bucket_rate_limiter.controller;

import com.pranav.token_bucket_rate_limiter.dto.request.RateLimitRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.RateLimitResponse;
import com.pranav.token_bucket_rate_limiter.service.interfaces.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the Token Bucket rate-limiting endpoint.
 *
 * <p>Clients POST their API key; the service checks the bucket state
 * and returns 200 (allowed) or 429 (too many requests).
 */
@RestController
@RequestMapping("/api/v1/rate-limit")
@RequiredArgsConstructor
@Tag(name = "Rate Limiter", description = "Endpoint for performing token-bucket rate-limit checks against registered API clients")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    @Operation(
            summary = "Check rate limit",
            description = "Validates the API key, refills the token bucket based on elapsed time, " +
                    "and returns 200 if a token was consumed or 429 if the bucket is empty."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request allowed — token consumed successfully"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded — bucket is empty"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API key"),
            @ApiResponse(responseCode = "400", description = "Validation failure")
    })
    @PostMapping
    public ResponseEntity<RateLimitResponse> checkRateLimit(@Valid @RequestBody RateLimitRequest request) {
        RateLimitResponse response = rateLimiterService.checkRateLimit(request);
        if (response.isAllowed()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }
}