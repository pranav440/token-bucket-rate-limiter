package com.pranav.token_bucket_rate_limiter.entity;

import com.pranav.token_bucket_rate_limiter.enums.AlgorithmType;
import com.pranav.token_bucket_rate_limiter.enums.ClientStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity representing an API client registered in the Token Bucket
 * Rate Limiter system.
 *
 * <p>Each client has:
 * <ul>
 *   <li>A system-generated {@code clientId} (human-readable, e.g. {@code CLIENT_A1B2C3D4})</li>
 *   <li>A system-generated {@code apiKey} (secret token for rate-limit calls)</li>
 *   <li>Rate configuration: {@code requestsPerSecond} and {@code burstCapacity}</li>
 *   <li>A lazily-loaded {@link BucketState} tracking token availability</li>
 * </ul>
 *
 * <p>Timestamps are managed entirely by JPA lifecycle hooks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String clientId;

    @Column(nullable = false, unique = true)
    private String clientName;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Column(nullable = false)
    private Integer requestsPerSecond;

    @Column(nullable = false)
    private Integer burstCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlgorithmType algorithmType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientStatus status;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @OneToOne(
            mappedBy = "client",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private BucketState bucketState;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}