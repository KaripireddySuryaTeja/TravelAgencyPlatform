package com.epam.edp.demo.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class JwtServiceTest {

    @Test
    void resolveSecretBytes_treatsNonCanonicalBase64LikeRawSecret() {
        String rawSecret = "mysecretkey123456789012345678901234";

        byte[] resolved = JwtService.resolveSecretBytes(rawSecret);

        assertArrayEquals(rawSecret.getBytes(StandardCharsets.UTF_8), resolved);
    }

    @Test
    void resolveSecretBytes_decodesCanonicalBase64Secret() {
        byte[] keyBytes = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
        String base64Secret = Base64.getEncoder().encodeToString(keyBytes);

        byte[] resolved = JwtService.resolveSecretBytes(base64Secret);

        assertArrayEquals(keyBytes, resolved);
    }
}