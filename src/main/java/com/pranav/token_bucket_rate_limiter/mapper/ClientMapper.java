package com.pranav.token_bucket_rate_limiter.mapper;

import com.pranav.token_bucket_rate_limiter.dto.request.CreateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.ClientResponse;
import com.pranav.token_bucket_rate_limiter.entity.Client;
import com.pranav.token_bucket_rate_limiter.enums.ClientStatus;
import com.pranav.token_bucket_rate_limiter.util.ApiKeyGenerator;
import com.pranav.token_bucket_rate_limiter.util.ClientIdGenerator;

/**
 * Stateless mapper utility for converting between {@link Client} entities
 * and their DTO representations.
 *
 * <p>On entity creation, this mapper sets the generated {@code clientId},
 * {@code apiKey}, and initial {@code ACTIVE} status.
 * Timestamps are managed by JPA lifecycle hooks ({@code @PrePersist} / {@code @PreUpdate}).
 */
public final class ClientMapper {

    private ClientMapper() {
        // Utility class — do not instantiate
    }

    /**
     * Maps a {@link CreateClientRequest} to a new {@link Client} entity.
     *
     * @param request the incoming registration request
     * @return a fully initialised (unsaved) Client entity
     */
    public static Client toEntity(CreateClientRequest request) {
        return Client.builder()
                .clientName(request.getClientName())
                .ownerEmail(request.getOwnerEmail())
                .requestsPerSecond(request.getRequestsPerSecond())
                .burstCapacity(request.getBurstCapacity())
                .algorithmType(request.getAlgorithmType())
                .description(request.getDescription())
                .clientId(ClientIdGenerator.generate())
                .apiKey(ApiKeyGenerator.generate())
                .status(ClientStatus.ACTIVE)
                .build();
    }

    /**
     * Maps a persisted {@link Client} entity to a {@link ClientResponse} DTO.
     *
     * @param client the entity to map
     * @return a response DTO safe to return to API consumers
     */
    public static ClientResponse toResponse(Client client) {
        return ClientResponse.builder()
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .apiKey(client.getApiKey())
                .requestsPerSecond(client.getRequestsPerSecond())
                .burstCapacity(client.getBurstCapacity())
                .algorithmType(client.getAlgorithmType())
                .status(client.getStatus())
                .ownerEmail(client.getOwnerEmail())
                .description(client.getDescription())
                .build();
    }
}