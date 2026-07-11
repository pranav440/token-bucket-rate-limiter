package com.pranav.token_bucket_rate_limiter.service.interfaces;

import com.pranav.token_bucket_rate_limiter.dto.request.CreateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.request.UpdateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.ClientResponse;

import java.util.List;

public interface ClientService {

    ClientResponse registerClient(CreateClientRequest request);

    ClientResponse getClientByClientId(String clientId);

    List<ClientResponse> getAllClients();

    ClientResponse updateClient(String clientId, UpdateClientRequest request);

    void enableClient(String clientId);

    void disableClient(String clientId);

    void deleteClient(String clientId);
}