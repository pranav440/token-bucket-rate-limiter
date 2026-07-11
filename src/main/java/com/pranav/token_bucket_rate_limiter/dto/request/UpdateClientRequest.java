package com.pranav.token_bucket_rate_limiter.dto.request;

import com.pranav.token_bucket_rate_limiter.enums.AlgorithmType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateClientRequest {
    @NotBlank(message = "Client name is required")
    private String clientName;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Invalid email format")
    private String ownerEmail;

    @NotNull(message = "Requests Per Second is required")
    @Min(value = 1, message = "Requests Per Second must be at least 1")
    private Integer requestsPerSecond;

    @NotNull(message = "Burst Capacity is required")
    @Min(value = 1, message = "Burst Capacity must be at least 1")
    private Integer burstCapacity;

    @NotNull(message = "Algorithm Type is required")
    private AlgorithmType algorithmType;

    @NotBlank(message = "Description is required")
    private String description;
}