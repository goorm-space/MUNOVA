package com.space.munova.core.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StaticVariables {
    // ========================================================================
    // JWT
    public static final String ROLE_CLAIM_KEY = "authorities";
    public static final String NAME_CLAIM_KEY = "username";
    public static final String REFRESH_TOKEN_COOKIE_KEY = "refresh-token";

    // ========================================================================
    // AUTH
    public static final String DEVICE_ID_HEADER_PREFIX = "X-Device-Id";
    public static final String AUTH_HEADER_PREFIX = "Bearer ";

    // ========================================================================
    // REDIS
    public static final String REDISSON_LOCK_PREFIX = "LOCK:";

    // ========================================================================
}
