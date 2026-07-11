package com.pranav.token_bucket_rate_limiter.service;

import com.pranav.token_bucket_rate_limiter.dto.request.RateLimitRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.RateLimitResponse;
import com.pranav.token_bucket_rate_limiter.entity.BucketState;
import com.pranav.token_bucket_rate_limiter.entity.Client;
import com.pranav.token_bucket_rate_limiter.enums.AlgorithmType;
import com.pranav.token_bucket_rate_limiter.enums.ClientStatus;
import com.pranav.token_bucket_rate_limiter.exception.BucketNotFoundException;
import com.pranav.token_bucket_rate_limiter.exception.InvalidApiKeyException;
import com.pranav.token_bucket_rate_limiter.repository.BucketStateRepository;
import com.pranav.token_bucket_rate_limiter.repository.ClientRepository;
import com.pranav.token_bucket_rate_limiter.service.impl.RateLimiterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link RateLimiterServiceImpl}.
 *
 * <p>Verifies the Token Bucket algorithm behaviour:
 * <ul>
 *   <li>Valid request → token consumed → 200 response</li>
 *   <li>Invalid API key → exception</li>
 *   <li>Disabled client → exception</li>
 *   <li>Empty bucket → request denied with retryAfter</li>
 *   <li>Bucket refill based on elapsed time</li>
 *   <li>retryAfter calculation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterServiceImpl Tests")
class RateLimiterServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private BucketStateRepository bucketStateRepository;

    @InjectMocks
    private RateLimiterServiceImpl rateLimiterService;

    private static final String VALID_API_KEY = "tb_live_abc123";

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private Client buildClient(ClientStatus status, int rps, int burstCapacity) {
        return Client.builder()
                .id(1L)
                .clientId("CLIENT_ABC12345")
                .clientName("Test App")
                .apiKey(VALID_API_KEY)
                .ownerEmail("owner@example.com")
                .requestsPerSecond(rps)
                .burstCapacity(burstCapacity)
                .algorithmType(AlgorithmType.TOKEN_BUCKET)
                .status(status)
                .description("Test client")
                .build();
    }

    private BucketState buildBucket(Client client, int availableTokens, LocalDateTime lastRefill) {
        return BucketState.builder()
                .id(1L)
                .availableTokens(availableTokens)
                .lastRefillTime(lastRefill)
                .client(client)
                .build();
    }

    private RateLimitRequest requestWith(String apiKey) {
        return new RateLimitRequest(apiKey);
    }

    // =========================================================================
    // Valid request — token consumption
    // =========================================================================

    @Nested
    @DisplayName("Valid requests")
    class ValidRequest {

        @Test
        @DisplayName("should allow request and consume one token when bucket is not empty")
        void checkRateLimit_allowed_consumesToken() {
            // Arrange
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            BucketState bucket = buildBucket(client, 5, LocalDateTime.now().minusSeconds(0));

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any(BucketState.class))).willReturn(bucket);

            // Act
            RateLimitResponse response = rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            // Assert
            assertThat(response.isAllowed()).isTrue();
            assertThat(response.getRemainingTokens()).isEqualTo(4); // 5 - 1 = 4
            assertThat(response.getRetryAfter()).isZero();
            assertThat(response.getTimestamp()).isNotNull();

            then(bucketStateRepository).should().save(bucket);
        }

        @Test
        @DisplayName("should allow request when only 1 token remains")
        void checkRateLimit_allowedWithLastToken() {
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            BucketState bucket = buildBucket(client, 1, LocalDateTime.now());

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any())).willReturn(bucket);

            RateLimitResponse response = rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            assertThat(response.isAllowed()).isTrue();
            assertThat(response.getRemainingTokens()).isZero();
        }
    }

    // =========================================================================
    // Invalid API key
    // =========================================================================

    @Nested
    @DisplayName("Invalid API key")
    class InvalidApiKey {

        @Test
        @DisplayName("should throw InvalidApiKeyException when API key does not exist")
        void checkRateLimit_invalidApiKey_throwsException() {
            given(clientRepository.findByApiKey("invalid_key")).willReturn(Optional.empty());

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(requestWith("invalid_key")))
                    .isInstanceOf(InvalidApiKeyException.class)
                    .hasMessageContaining("Invalid API Key");

            then(bucketStateRepository).shouldHaveNoInteractions();
        }
    }

    // =========================================================================
    // Disabled client
    // =========================================================================

    @Nested
    @DisplayName("Disabled client")
    class DisabledClient {

        @Test
        @DisplayName("should throw InvalidApiKeyException when client is INACTIVE")
        void checkRateLimit_inactiveClient_throwsException() {
            Client inactiveClient = buildClient(ClientStatus.INACTIVE, 5, 10);
            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(inactiveClient));

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY)))
                    .isInstanceOf(InvalidApiKeyException.class)
                    .hasMessageContaining("disabled");

            then(bucketStateRepository).shouldHaveNoInteractions();
        }
    }

    // =========================================================================
    // Bucket empty — denied
    // =========================================================================

    @Nested
    @DisplayName("Bucket empty")
    class BucketEmpty {

        @Test
        @DisplayName("should deny request and return positive retryAfter when bucket is empty")
        void checkRateLimit_bucketEmpty_deniesRequest() {
            // Arrange: 0 tokens, no elapsed time (no refill)
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            BucketState bucket = buildBucket(client, 0, LocalDateTime.now());

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any())).willReturn(bucket);

            // Act
            RateLimitResponse response = rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            // Assert
            assertThat(response.isAllowed()).isFalse();
            assertThat(response.getRemainingTokens()).isZero();
            assertThat(response.getRetryAfter()).isPositive();
        }

        @Test
        @DisplayName("should still persist bucket state (refill update) even when denied")
        void checkRateLimit_bucketEmpty_stillSavesBucket() {
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            BucketState bucket = buildBucket(client, 0, LocalDateTime.now());

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any())).willReturn(bucket);

            rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            // Save must ALWAYS be called (whether allowed or denied) to persist refill
            then(bucketStateRepository).should(times(1)).save(bucket);
        }
    }

    // =========================================================================
    // Bucket refill
    // =========================================================================

    @Nested
    @DisplayName("Bucket refill based on elapsed time")
    class BucketRefill {

        @Test
        @DisplayName("should refill tokens based on elapsed seconds × requestsPerSecond")
        void checkRateLimit_refillsCorrectly() {
            // Arrange: 0 tokens, but 2 seconds elapsed, RPS=5 → refill = 10 tokens (capped at 10)
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            LocalDateTime twoSecondsAgo = LocalDateTime.now().minusSeconds(2);
            BucketState bucket = buildBucket(client, 0, twoSecondsAgo);

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any())).willReturn(bucket);

            // Act
            RateLimitResponse response = rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            // Assert: refill (10 tokens) applied, 1 consumed → 9 remaining
            assertThat(response.isAllowed()).isTrue();
            assertThat(response.getRemainingTokens()).isEqualTo(9);
        }

        @Test
        @DisplayName("should cap refilled tokens at burstCapacity")
        void checkRateLimit_refillCappedAtBurstCapacity() {
            // Arrange: 0 tokens, 100 seconds elapsed — refill would be huge, but capped at 10
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            LocalDateTime longAgo = LocalDateTime.now().minusSeconds(100);
            BucketState bucket = buildBucket(client, 0, longAgo);

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any())).willReturn(bucket);

            RateLimitResponse response = rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            // burstCapacity = 10. After consuming 1 → 9 remaining
            assertThat(response.isAllowed()).isTrue();
            assertThat(response.getRemainingTokens()).isEqualTo(9);
        }

        @Test
        @DisplayName("should not refill tokens when elapsed time is less than 1 second")
        void checkRateLimit_noRefillUnder1Second() {
            // Arrange: 3 tokens, 0 seconds elapsed — no refill
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            BucketState bucket = buildBucket(client, 3, LocalDateTime.now());

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any())).willReturn(bucket);

            RateLimitResponse response = rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            // No refill, 1 consumed → 2 remaining
            assertThat(response.isAllowed()).isTrue();
            assertThat(response.getRemainingTokens()).isEqualTo(2);
        }
    }

    // =========================================================================
    // retryAfter calculation
    // =========================================================================

    @Nested
    @DisplayName("retryAfter calculation")
    class RetryAfter {

        @Test
        @DisplayName("should return retryAfter = 1000/rps when bucket is empty")
        void checkRateLimit_retryAfterCalculation() {
            // Arrange: RPS = 5, so retryAfter = 1000/5 = 200 ms
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            BucketState bucket = buildBucket(client, 0, LocalDateTime.now());

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any())).willReturn(bucket);

            RateLimitResponse response = rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            assertThat(response.isAllowed()).isFalse();
            assertThat(response.getRetryAfter()).isEqualTo(200L); // 1000 / 5
        }

        @Test
        @DisplayName("should return retryAfter = 0 when request is allowed")
        void checkRateLimit_retryAfterZeroWhenAllowed() {
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            BucketState bucket = buildBucket(client, 5, LocalDateTime.now());

            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.of(bucket));
            given(bucketStateRepository.save(any())).willReturn(bucket);

            RateLimitResponse response = rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY));

            assertThat(response.getRetryAfter()).isZero();
        }
    }

    // =========================================================================
    // BucketNotFoundException
    // =========================================================================

    @Nested
    @DisplayName("BucketNotFoundException")
    class BucketNotFound {

        @Test
        @DisplayName("should throw BucketNotFoundException when bucket state is missing")
        void checkRateLimit_bucketMissing_throwsException() {
            Client client = buildClient(ClientStatus.ACTIVE, 5, 10);
            given(clientRepository.findByApiKey(VALID_API_KEY)).willReturn(Optional.of(client));
            given(bucketStateRepository.findByClient(client)).willReturn(Optional.empty());

            assertThatThrownBy(() -> rateLimiterService.checkRateLimit(requestWith(VALID_API_KEY)))
                    .isInstanceOf(BucketNotFoundException.class)
                    .hasMessageContaining("CLIENT_ABC12345");
        }
    }
}
