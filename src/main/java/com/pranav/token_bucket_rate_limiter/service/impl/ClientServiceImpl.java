package com.pranav.token_bucket_rate_limiter.service.impl;

import com.pranav.token_bucket_rate_limiter.dto.request.CreateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.request.UpdateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.ClientResponse;
import com.pranav.token_bucket_rate_limiter.entity.BucketState;
import com.pranav.token_bucket_rate_limiter.entity.Client;
import com.pranav.token_bucket_rate_limiter.enums.ClientStatus;
import com.pranav.token_bucket_rate_limiter.exception.ClientNotFoundException;
import com.pranav.token_bucket_rate_limiter.exception.DuplicateClientException;
import com.pranav.token_bucket_rate_limiter.mapper.ClientMapper;
import com.pranav.token_bucket_rate_limiter.repository.BucketStateRepository;
import com.pranav.token_bucket_rate_limiter.repository.ClientRepository;
import com.pranav.token_bucket_rate_limiter.service.interfaces.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for managing API clients in the Token Bucket
 * Rate Limiter system.
 *
 * <p>Each registered client is automatically provisioned with a unique
 * {@code clientId}, a {@code apiKey}, and an initial {@link BucketState}
 * seeded to the client's configured burst capacity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final BucketStateRepository bucketStateRepository;

    @Override
    @Transactional
    public ClientResponse registerClient(CreateClientRequest request) {

        if (clientRepository.existsByClientName(request.getClientName())) {
            log.warn("Duplicate client registration attempt for name: [{}]", request.getClientName());
            throw new DuplicateClientException("A client with that name already exists.");
        }

        Client client = ClientMapper.toEntity(request);

        BucketState bucketState = BucketState.builder()
                .availableTokens(client.getBurstCapacity())
                .lastRefillTime(LocalDateTime.now())
                .client(client)
                .build();

        client.setBucketState(bucketState);

        Client savedClient = clientRepository.save(client);
        log.info("Client registered — id: [{}], name: [{}]", savedClient.getClientId(), savedClient.getClientName());

        return ClientMapper.toResponse(savedClient);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientByClientId(String clientId) {

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        return ClientMapper.toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {

        return clientRepository.findAll()
                .stream()
                .map(ClientMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClientResponse updateClient(String clientId, UpdateClientRequest request) {

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        client.setClientName(request.getClientName());
        client.setOwnerEmail(request.getOwnerEmail());
        client.setRequestsPerSecond(request.getRequestsPerSecond());
        client.setBurstCapacity(request.getBurstCapacity());
        client.setAlgorithmType(request.getAlgorithmType());
        client.setDescription(request.getDescription());
        // updatedAt is managed by @PreUpdate — no manual set needed

        Client updatedClient = clientRepository.save(client);
        log.info("Client updated — id: [{}]", clientId);

        return ClientMapper.toResponse(updatedClient);
    }

    @Override
    @Transactional
    public void enableClient(String clientId) {

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        client.setStatus(ClientStatus.ACTIVE);
        clientRepository.save(client);
        log.info("Client enabled — id: [{}]", clientId);
    }

    @Override
    @Transactional
    public void disableClient(String clientId) {

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        client.setStatus(ClientStatus.INACTIVE);
        clientRepository.save(client);
        log.info("Client disabled — id: [{}]", clientId);
    }

    @Override
    @Transactional
    public void deleteClient(String clientId) {

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client not found: " + clientId));

        clientRepository.delete(client);
        log.info("Client deleted — id: [{}]", clientId);
    }
}