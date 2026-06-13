package uz.thinkhub.igw.starter.tokencache;

import java.time.Instant;
import java.util.Objects;

/**
 * An OAuth-style access token with a known expiration time.
 *
 * <p>Used by {@link TokenCache} and {@link CachingTokenProvider}. A {@code null}
 * expiry is not allowed; tokens without a known expiry can use a far-future
 * {@link Instant} to indicate "no expiry."
 */
public record Token(String value, Instant expiresAt) {

    public Token {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
