package com.pranav.token_bucket_rate_limiter.service;

import com.pranav.token_bucket_rate_limiter.dto.request.CreateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.request.UpdateClientRequest;
import com.pranav.token_bucket_rate_limiter.dto.response.ClientResponse;
import com.pranav.token_bucket_rate_limiter.entity.BucketState;
import com.pranav.token_bucket_rate_limiter.entity.Client;
import com.pranav.token_bucket_rate_limiter.enums.AlgorithmType;
import com.pranav.token_bucket_rate_limiter.enums.ClientStatus;
import com.pranav.token_bucket_rate_limiter.exception.ClientNotFoundException;
import com.pranav.token_bucket_rate_limiter.exception.DuplicateClientException;
import com.pranav.token_bucket_rate_limiter.repository.BucketStateRepository;
import com.pranav.token_bucket_rate_limiter.repository.ClientRepository;
import com.pranav.token_bucket_rate_limiter.service.impl.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link ClientServiceImpl}.
 *
 * <p>All repository interactions are mocked with Mockito.
 * No Spring context is loaded — these are pure unit tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientServiceImpl Tests")
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private BucketStateRepository bucketStateRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private Client buildActiveClient() {
        BucketState bucket = BucketState.builder()
                .id(1L)
                .availableTokens(10)
                .lastRefillTime(LocalDateTime.now())
                .build();

        Client client = Client.builder()
                .id(1L)
                .clientId("CLIENT_ABC12345")
                .clientName("Test App")
                .apiKey("tb_live_abc123")
                .ownerEmail("owner@example.com")
                .requestsPerSecond(5)
                .burstCapacity(10)
                .algorithmType(AlgorithmType.TOKEN_BUCKET)
                .status(ClientStatus.ACTIVE)
                .description("Test client")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        bucket.setClient(client);
        client.setBucketState(bucket);
        return client;
    }

    private CreateClientRequest buildCreateRequest() {
        return CreateClientRequest.builder()
                .clientName("Test App")
                .ownerEmail("owner@example.com")
                .requestsPerSecond(5)
                .burstCapacity(10)
                .algorithmType(AlgorithmType.TOKEN_BUCKET)
                .description("Test client")
                .build();
    }

    // =========================================================================
    // registerClient()
    // =========================================================================

    @Nested
    @DisplayName("registerClient()")
    class RegisterClient {

        @Test
        @DisplayName("should register a new client and return a populated response")
        void registerClient_success() {
            // Arrange
            CreateClientRequest request = buildCreateRequest();
            Client savedClient = buildActiveClient();

            given(clientRepository.existsByClientName("Test App")).willReturn(false);
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            // Act
            ClientResponse response = clientService.registerClient(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getClientName()).isEqualTo("Test App");
            assertThat(response.getStatus()).isEqualTo(ClientStatus.ACTIVE);
            assertThat(response.getApiKey()).startsWith("tb_live_");

            then(clientRepository).should(times(1)).existsByClientName("Test App");
            then(clientRepository).should(times(1)).save(any(Client.class));
        }

        @Test
        @DisplayName("should throw DuplicateClientException when client name already exists")
        void registerClient_duplicateName_throwsException() {
            // Arrange
            CreateClientRequest request = buildCreateRequest();
            given(clientRepository.existsByClientName("Test App")).willReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> clientService.registerClient(request))
                    .isInstanceOf(DuplicateClientException.class)
                    .hasMessageContaining("already exists");

            then(clientRepository).should(never()).save(any(Client.class));
        }

        @Test
        @DisplayName("should persist a BucketState seeded with the burst capacity")
        void registerClient_bucketStateSeededWithBurstCapacity() {
            // Arrange
            CreateClientRequest request = buildCreateRequest();
            Client savedClient = buildActiveClient();
            given(clientRepository.existsByClientName(anyString())).willReturn(false);
            given(clientRepository.save(any(Client.class))).willReturn(savedClient);

            // Act
            clientService.registerClient(request);

            // Assert — capture the Client passed to save and verify BucketState
            ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
            then(clientRepository).should().save(captor.capture());

            Client captured = captor.getValue();
            assertThat(captured.getBucketState()).isNotNull();
            assertThat(captured.getBucketState().getAvailableTokens())
                    .isEqualTo(request.getBurstCapacity());
        }
    }

    // =========================================================================
    // getClientByClientId()
    // =========================================================================

    @Nested
    @DisplayName("getClientByClientId()")
    class GetClient {

        @Test
        @DisplayName("should return client when found")
        void getClient_found() {
            // Arrange
            Client client = buildActiveClient();
            given(clientRepository.findByClientId("CLIENT_ABC12345")).willReturn(Optional.of(client));

            // Act
            ClientResponse response = clientService.getClientByClientId("CLIENT_ABC12345");

            // Assert
            assertThat(response.getClientId()).isEqualTo("CLIENT_ABC12345");
            assertThat(response.getClientName()).isEqualTo("Test App");
        }

        @Test
        @DisplayName("should throw ClientNotFoundException when clientId does not exist")
        void getClient_notFound_throwsException() {
            // Arrange
            given(clientRepository.findByClientId("NONEXISTENT")).willReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> clientService.getClientByClientId("NONEXISTENT"))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining("NONEXISTENT");
        }
    }

    // =========================================================================
    // getAllClients()
    // =========================================================================

    @Nested
    @DisplayName("getAllClients()")
    class GetAllClients {

        @Test
        @DisplayName("should return all clients mapped to responses")
        void getAllClients_returnsAll() {
            // Arrange
            Client c1 = buildActiveClient();
            Client c2 = buildActiveClient();
            c2.setClientId("CLIENT_XYZ99999");
            c2.setClientName("Another App");

            given(clientRepository.findAll()).willReturn(List.of(c1, c2));

            // Act
            List<ClientResponse> responses = clientService.getAllClients();

            // Assert
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(ClientResponse::getClientName)
                    .containsExactlyInAnyOrder("Test App", "Another App");
        }

        @Test
        @DisplayName("should return empty list when no clients registered")
        void getAllClients_empty() {
            given(clientRepository.findAll()).willReturn(List.of());

            List<ClientResponse> responses = clientService.getAllClients();

            assertThat(responses).isEmpty();
        }
    }

    // =========================================================================
    // updateClient()
    // =========================================================================

    @Nested
    @DisplayName("updateClient()")
    class UpdateClient {

        @Test
        @DisplayName("should update mutable fields and return updated response")
        void updateClient_success() {
            // Arrange
            Client existing = buildActiveClient();
            given(clientRepository.findByClientId("CLIENT_ABC12345")).willReturn(Optional.of(existing));
            given(clientRepository.save(any(Client.class))).willReturn(existing);

            UpdateClientRequest updateReq = UpdateClientRequest.builder()
                    .clientName("Updated App")
                    .ownerEmail("updated@example.com")
                    .requestsPerSecond(10)
                    .burstCapacity(20)
                    .algorithmType(AlgorithmType.TOKEN_BUCKET)
                    .description("Updated description")
                    .build();

            // Act
            ClientResponse response = clientService.updateClient("CLIENT_ABC12345", updateReq);

            // Assert
            assertThat(existing.getClientName()).isEqualTo("Updated App");
            assertThat(existing.getRequestsPerSecond()).isEqualTo(10);
            assertThat(existing.getBurstCapacity()).isEqualTo(20);
        }

        @Test
        @DisplayName("should throw ClientNotFoundException when updating non-existent client")
        void updateClient_notFound_throwsException() {
            given(clientRepository.findByClientId("GHOST")).willReturn(Optional.empty());

            UpdateClientRequest req = UpdateClientRequest.builder()
                    .clientName("X").ownerEmail("x@x.com")
                    .requestsPerSecond(1).burstCapacity(1)
                    .algorithmType(AlgorithmType.TOKEN_BUCKET).description("x")
                    .build();

            assertThatThrownBy(() -> clientService.updateClient("GHOST", req))
                    .isInstanceOf(ClientNotFoundException.class);
        }
    }

    // =========================================================================
    // enableClient() / disableClient()
    // =========================================================================

    @Nested
    @DisplayName("enableClient() / disableClient()")
    class StatusToggle {

        @Test
        @DisplayName("should set status to ACTIVE when enabling a client")
        void enableClient_setsActiveStatus() {
            // Arrange
            Client client = buildActiveClient();
            client.setStatus(ClientStatus.INACTIVE);
            given(clientRepository.findByClientId("CLIENT_ABC12345")).willReturn(Optional.of(client));
            given(clientRepository.save(any(Client.class))).willReturn(client);

            // Act
            clientService.enableClient("CLIENT_ABC12345");

            // Assert
            assertThat(client.getStatus()).isEqualTo(ClientStatus.ACTIVE);
            then(clientRepository).should().save(client);
        }

        @Test
        @DisplayName("should set status to INACTIVE when disabling a client")
        void disableClient_setsInactiveStatus() {
            // Arrange
            Client client = buildActiveClient();
            given(clientRepository.findByClientId("CLIENT_ABC12345")).willReturn(Optional.of(client));
            given(clientRepository.save(any(Client.class))).willReturn(client);

            // Act
            clientService.disableClient("CLIENT_ABC12345");

            // Assert
            assertThat(client.getStatus()).isEqualTo(ClientStatus.INACTIVE);
        }

        @Test
        @DisplayName("should throw ClientNotFoundException when enabling non-existent client")
        void enableClient_notFound_throwsException() {
            given(clientRepository.findByClientId("MISSING")).willReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.enableClient("MISSING"))
                    .isInstanceOf(ClientNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ClientNotFoundException when disabling non-existent client")
        void disableClient_notFound_throwsException() {
            given(clientRepository.findByClientId("MISSING")).willReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.disableClient("MISSING"))
                    .isInstanceOf(ClientNotFoundException.class);
        }
    }

    // =========================================================================
    // deleteClient()
    // =========================================================================

    @Nested
    @DisplayName("deleteClient()")
    class DeleteClient {

        @Test
        @DisplayName("should delete client when found")
        void deleteClient_success() {
            // Arrange
            Client client = buildActiveClient();
            given(clientRepository.findByClientId("CLIENT_ABC12345")).willReturn(Optional.of(client));
            willDoNothing().given(clientRepository).delete(client);

            // Act
            clientService.deleteClient("CLIENT_ABC12345");

            // Assert
            then(clientRepository).should().delete(client);
        }

        @Test
        @DisplayName("should throw ClientNotFoundException when deleting non-existent client")
        void deleteClient_notFound_throwsException() {
            given(clientRepository.findByClientId("GHOST")).willReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.deleteClient("GHOST"))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining("GHOST");
        }
    }
}
