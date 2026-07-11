package com.pranav.token_bucket_rate_limiter.repository;

import com.pranav.token_bucket_rate_limiter.entity.BucketState;
import com.pranav.token_bucket_rate_limiter.entity.Client;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BucketStateRepository extends JpaRepository<BucketState, Long> {

    /**
     * Acquires a pessimistic write lock on the BucketState row to prevent
     * concurrent token consumption for the same client.
     *
     * @param client the owning client entity
     * @return the locked BucketState if present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BucketState> findByClient(Client client);
}
