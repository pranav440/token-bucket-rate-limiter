package com.pranav.token_bucket_rate_limiter.repository;

import com.pranav.token_bucket_rate_limiter.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByClientId(String clientId);

    Optional<Client> findByApiKey(String apiKey);

    boolean existsByClientName(String clientName);
}