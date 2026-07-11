package com.pranav.token_bucket_rate_limiter.controller;

import com.pranav.token_bucket_rate_limiter.dto.request.CreateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.request.UpdateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.ClientResponse;
import com.pranav.token_bucket_rate_limiter.service.interfaces.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller providing CRUD operations for API client management.
 *
 * <p>Clients are entities that consume the Token Bucket Rate Limiter service.
 * Each client is provisioned with a unique {@code clientId}, an {@code apiKey},
 * and an initial token bucket.
 */
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Client Management", description = "APIs for registering, retrieving, updating and managing API clients")
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Register a new client", description = "Creates a new API client with a generated clientId, apiKey, and initial token bucket")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "409", description = "Client name already exists")
    })
    @PostMapping
    public ResponseEntity<ClientResponse> registerClient(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse clientResponse = clientService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(clientResponse);
    }

    @Operation(summary = "Get all clients", description = "Returns a list of all registered API clients")
    @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        List<ClientResponse> clients = clientService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    @Operation(summary = "Get client by clientId", description = "Returns a single client identified by their unique clientId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client found"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable String clientId) {
        ClientResponse clientResponse = clientService.getClientByClientId(clientId);
        return ResponseEntity.ok(clientResponse);
    }

    @Operation(summary = "Update client", description = "Updates the mutable properties of an existing client (name, email, rate config, algorithm, description)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PutMapping("/{clientId}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable String clientId,
            @Valid @RequestBody UpdateClientRequest request) {
        ClientResponse clientResponse = clientService.updateClient(clientId, request);
        return ResponseEntity.ok(clientResponse);
    }

    @Operation(summary = "Enable client", description = "Sets the client status to ACTIVE, allowing it to consume tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client enabled"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PatchMapping("/{clientId}/enable")
    public ResponseEntity<Void> enableClient(@PathVariable String clientId) {
        clientService.enableClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Disable client", description = "Sets the client status to INACTIVE, blocking all rate-limit requests")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client disabled"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PatchMapping("/{clientId}/disable")
    public ResponseEntity<Void> disableClient(@PathVariable String clientId) {
        clientService.disableClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete client", description = "Permanently removes the client and its associated token bucket (cascade)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client deleted"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }
}