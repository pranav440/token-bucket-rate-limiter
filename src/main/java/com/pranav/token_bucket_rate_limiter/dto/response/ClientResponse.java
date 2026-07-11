package com.pranav.token_bucket_rate_limiter.dto.response;

import com.pranav.token_bucket_rate_limiter.enums.AlgorithmType;
import com.pranav.token_bucket_rate_limiter.enums.ClientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientResponse {
    private String clientId;

    private String clientName;

    private String apiKey;

    private Integer requestsPerSecond;

    private Integer burstCapacity;

    private AlgorithmType algorithmType;

    private ClientStatus status;

    private String ownerEmail;

    private String description;
}