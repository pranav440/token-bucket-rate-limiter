package com.pranav.token_bucket_rate_limiter.util;

import java.util.UUID;

/**
 * Generates unique API keys for new clients.
 *
 * <p>Format: {@code tb_live_<32-char UUID without dashes>}
 * <br>Example: {@code tb_live_a1b2c3d4e5f6...}
 */
public final class ApiKeyGenerator {

    private ApiKeyGenerator() {
        // Utility class — do not instantiate
    }

    /**
     * Generates a cryptographically random API key.
     *
     * @return a unique API key prefixed with {@code tb_live_}
     */
    public static String generate() {
        String cleanUuid = UUID.randomUUID().toString().replace("-", "");
        return "tb_live_" + cleanUuid;
    }
}
