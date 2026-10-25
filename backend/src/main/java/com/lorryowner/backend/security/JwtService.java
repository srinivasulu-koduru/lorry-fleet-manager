package com.lorryowner.backend.security;

import com.lorryowner.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service managing JWT creation, signature, parsing, and validation.
 * Uses JJWT library with HMAC-SHA256.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:}") String configuredSecret,
            @Value("${jwt.expiration-ms:604800000}") long expirationMs
    ) {
        this.expirationMs = expirationMs;

        if (configuredSecret != null && configuredSecret.trim().length() >= 32) {
            this.signingKey = Keys.hmacShaKeyFor(configuredSecret.trim().getBytes(StandardCharsets.UTF_8));
            log.info("JWT service initialized with configured secret key.");
        } else {
            // Safe fallback for development/testing when JWT_SECRET is not explicitly provided in environment
            byte[] randomKey = new byte[32];
            new SecureRandom().nextBytes(randomKey);
            this.signingKey = Keys.hmacShaKeyFor(randomKey);
            log.warn("JWT_SECRET environment variable not provided or shorter than 32 characters. " +
                    "Using securely generated runtime ephemeral key for development. Set JWT_SECRET in production.");
        }
    }

    /**
     * Generates a signed JWT bearer token containing user claims.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("pwdVer", user.getPasswordUpdatedAt() != null ? user.getPasswordUpdatedAt().toEpochMilli() : 0L);

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts all claims from a signed JWT token.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object val = extractClaims(token).get("userId");
        if (val instanceof Number num) {
            return num.longValue();
        }
        return null;
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public Date extractIssuedAt(String token) {
        return extractClaims(token).getIssuedAt();
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Validates that the token signature is authentic, not expired,
     * matches the specified user, user is active, and was not issued prior to a password update.
     */
    public boolean isTokenValid(String token, User user) {
        try {
            Claims claims = extractClaims(token);
            String username = claims.getSubject();
            Date expiration = claims.getExpiration();
            Date issuedAt = claims.getIssuedAt();

            if (!user.getUsername().equals(username)) {
                return false;
            }

            if (!user.isActive()) {
                return false;
            }

            if (expiration.before(new Date())) {
                return false;
            }

            // Invalidate token if issued prior to the user's latest password change
            if (user.getPasswordUpdatedAt() != null) {
                Object pwdVerClaim = claims.get("pwdVer");
                long tokenPwdVer = pwdVerClaim instanceof Number num ? num.longValue() : 0L;
                long userPwdVer = user.getPasswordUpdatedAt().toEpochMilli();
                if (tokenPwdVer != userPwdVer) {
                    log.debug("Token rejected for user {}: issued before latest password update.", username);
                    return false;
                }
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
