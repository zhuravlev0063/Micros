package ru.kubsu.flights.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class JwtVerifier {
    private final SecretKey key;

    public JwtVerifier(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser verify(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header with Bearer token is required");
        }
        String token = authorizationHeader.substring("Bearer ".length());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        List<String> roles = readRoles(claims.get("roles"));
        if (!roles.contains("USER")) {
            throw new IllegalArgumentException("USER role is required");
        }
        return new AuthenticatedUser(UUID.fromString(claims.getSubject()), roles);
    }

    private List<String> readRoles(Object rolesClaim) {
        if (rolesClaim instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        if (rolesClaim instanceof String value) {
            return List.of(value);
        }
        return List.of();
    }

    public record AuthenticatedUser(UUID userId, List<String> roles) {
    }
}
