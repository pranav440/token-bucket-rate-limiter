package com.pranav.token_bucket_rate_limiter.service.impl;

import com.pranav.token_bucket_rate_limiter.dto.request.RateLimitRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.RateLimitResponse;
import com.pranav.token_bucket_rate_limiter.entity.BucketState;
import com.pranav.token_bucket_rate_limiter.entity.Client;
import com.pranav.token_bucket_rate_limiter.enums.ClientStatus;
import com.pranav.token_bucket_rate_limiter.exception.BucketNotFoundException;
import com.pranav.token_bucket_rate_limiter.exception.InvalidApiKeyException;
import com.pranav.token_bucket_rate_limiter.repository.BucketStateRepository;
import com.pranav.token_bucket_rate_limiter.repository.ClientRepository;
import com.pranav.token_bucket_rate_limiter.service.interfaces.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Core service implementing the Token Bucket rate-limiting algorithm.
 *
 * <p>Algorithm flow:
 * <ol>
 *   <li>Validate API key and client status.</li>
 *   <li>Acquire a pessimistic write lock on BucketState.</li>
 *   <li>Compute elapsed time since last refill.</li>
 *   <li>Refill tokens up to the client's burst capacity.</li>
 *   <li>Consume one token if available; otherwise reject the request.</li>
 *   <li>Persist the updated BucketState.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private final ClientRepository clientRepository;
    private final BucketStateRepository bucketStateRepository;

    @Override
    @Transactional
    public RateLimitResponse checkRateLimit(RateLimitRequest request) {

        // Step 1: Validate API key — throws 401 if not found
        Client client = clientRepository.findByApiKey(request.getApiKey())
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API Key."));

        log.info("Processing rate-limit check for client [{}]", client.getClientId());

        // Step 2: Reject inactive clients
        if (client.getStatus() == ClientStatus.INACTIVE) {
            log.warn("Rate-limit request rejected — client [{}] is disabled", client.getClientId());
            throw new InvalidApiKeyException("Client account is disabled.");
        }

        // Step 3: Load BucketState with a pessimistic write lock
        BucketState bucket = bucketStateRepository.findByClient(client)
                .orElseThrow(() -> new BucketNotFoundException("Bucket state not found for client: " + client.getClientId()));

        log.debug("BucketState loaded for client [{}] — current tokens: {}", client.getClientId(), bucket.getAvailableTokens());

        // Step 4: Calculate elapsed time since last refill
        LocalDateTime now = LocalDateTime.now();
        Duration elapsed = Duration.between(bucket.getLastRefillTime(), now);
        long elapsedSeconds = elapsed.getSeconds();

        // Step 5: Compute and apply token refill (capped at burst capacity)
        long refillAmount = elapsedSeconds * client.getRequestsPerSecond();
        int refilledTokens = (int) Math.min(
                bucket.getAvailableTokens() + refillAmount,
                client.getBurstCapacity()
        );

        bucket.setAvailableTokens(refilledTokens);
        bucket.setLastRefillTime(now);

        // Step 6: Decide — allow or deny the request
        boolean allowed;
        long retryAfterMs;

        if (bucket.getAvailableTokens() > 0) {
            // Consume one token
            bucket.setAvailableTokens(bucket.getAvailableTokens() - 1);
            allowed = true;
            retryAfterMs = 0L;
            log.info("Request ALLOWED for client [{}] — remaining tokens: {}",
                    client.getClientId(), bucket.getAvailableTokens());
        } else {
            // Bucket empty — reject and advise when next token will be available
            allowed = false;
            retryAfterMs = 1000L / client.getRequestsPerSecond();
            log.warn("Request DENIED for client [{}] — bucket empty. Retry after {} ms",
                    client.getClientId(), retryAfterMs);
        }

        // Step 7: Persist updated BucketState (always — refill must be recorded)
        bucketStateRepository.save(bucket);

        return RateLimitResponse.builder()
                .allowed(allowed)
                .remainingTokens(bucket.getAvailableTokens())
                .retryAfter(retryAfterMs)
                .timestamp(now)
                .build();
    }
}