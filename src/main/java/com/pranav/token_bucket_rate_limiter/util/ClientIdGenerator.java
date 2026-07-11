package com.pranav.token_bucket_rate_limiter.util;

import java.util.UUID;

/**
 * Generates short, human-readable client identifiers.
 *
 * <p>Format: {@code CLIENT_<first 8 chars of UUID without dashes>}
 * <br>Example: {@code CLIENT_a1b2c3d4}
 */
public final class ClientIdGenerator {

    private ClientIdGenerator() {
        // Utility class — do not instantiate
    }

    /**
     * Generates a unique client ID.
     *
     * @return a unique client ID prefixed with {@code CLIENT_}
     */
    public static String generate() {
        String cleanUuid = UUID.randomUUID().toString().replace("-", "");
        return "CLIENT_" + cleanUuid.substring(0, 8).toUpperCase();
    }
}