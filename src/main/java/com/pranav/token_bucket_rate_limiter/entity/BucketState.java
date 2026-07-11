package com.pranav.token_bucket_rate_limiter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents the token bucket state for an API client.
 * Each client has exactly one BucketState, managed via a OneToOne relationship.
 * The row is acquired with a pessimistic write lock during rate-limit checks
 * to prevent concurrent token over-consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bucket_states")
public class BucketState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer availableTokens;

    @Column(nullable = false)
    private LocalDateTime lastRefillTime;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
}