package com.epam.edp.demo.security;


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
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.ttl-hours:24}") long ttlHours
    ) {
        byte[] raw = resolveSecretBytes(base64Secret);
        if (raw.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret must decode to at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.ttl = Duration.ofHours(ttlHours);
    }

    public static byte[] resolveSecretBytes(String secret) {
        byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length >= MIN_SECRET_BYTES || rawBytes.length < MIN_SECRET_BYTES) {
                return decoded;
            }
            log.debug("JWT secret decodes to fewer than {} bytes; treating long raw secret as UTF-8 instead", MIN_SECRET_BYTES);
        } catch (IllegalArgumentException e) {
            log.debug("JWT secret is not valid base64, treating as raw UTF-8: {}", e.getMessage());
        }
        return rawBytes;
    }

    public IssuedToken issue(String userId, String email, String firstName, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("firstName", firstName);
        if (role != null) claims.put("role", role);
        String token = Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
        return new IssuedToken(token, exp);
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtException(e);
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {}

    public static class InvalidJwtException extends RuntimeException {
        public InvalidJwtException(String msg) { super(msg); }
        public InvalidJwtException(Throwable cause) { super(cause.getMessage(), cause); }
    }
}

