package ru.kubsu.flights.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtVerifierTest {
    @Test
    void verifiesJwtSubject() {
        String secret = "test-secret-change-this-value-test-secret-change-this";
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("roles", List.of("USER"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        JwtVerifier verifier = new JwtVerifier(secret);

        assertThat(verifier.verify("Bearer " + token).userId()).isEqualTo(userId);
    }
}
