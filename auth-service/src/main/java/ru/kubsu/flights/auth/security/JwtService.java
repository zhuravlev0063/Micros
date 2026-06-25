package ru.kubsu.flights.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.kubsu.flights.auth.model.UserAccount;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public TokenIssue issueAccessToken(UserAccount user, List<String> roles) {
        Instant expiresAt = Instant.now().plus(accessTokenMinutes, ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .subject(user.id().toString())
                .claims(Map.of("email", user.email(), "roles", roles))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new TokenIssue(token, expiresAt);
    }

    public record TokenIssue(String token, Instant expiresAt) {
    }
}
