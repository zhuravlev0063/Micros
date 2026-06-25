package ru.kubsu.flights.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kubsu.flights.auth.dto.AuthResponse;
import ru.kubsu.flights.auth.dto.LoginRequest;
import ru.kubsu.flights.auth.dto.LogoutRequest;
import ru.kubsu.flights.auth.dto.RefreshRequest;
import ru.kubsu.flights.auth.dto.RegisterRequest;
import ru.kubsu.flights.auth.model.RefreshToken;
import ru.kubsu.flights.auth.model.UserAccount;
import ru.kubsu.flights.auth.repository.RefreshTokenRepository;
import ru.kubsu.flights.auth.repository.UserAccountRepository;
import ru.kubsu.flights.auth.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final long refreshTokenDays;

    public AuthService(
            UserAccountRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            KafkaTemplate<String, String> kafkaTemplate,
            JdbcTemplate jdbcTemplate,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays
    ) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.kafkaTemplate = kafkaTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegistration(request);
        if (users.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        Instant now = Instant.now();
        UserAccount user = users.save(new UserAccount(
                UUID.randomUUID(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                "ACTIVE",
                now,
                now
        ));
        assignUserRole(user.id());
        kafkaTemplate.send("auth.user-registered", user.id().toString(), "{\"userId\":\"" + user.id() + "\"}");
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAccount user = users.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            kafkaTemplate.send("auth.login-failed", user.email(), "{\"email\":\"" + user.email() + "\"}");
            throw new IllegalArgumentException("Invalid email or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = sha256(request.refreshToken());
        RefreshToken token = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid"));
        if (!token.activeAt(Instant.now())) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }
        UserAccount user = users.findById(token.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        String hash = sha256(request.refreshToken());
        RefreshToken token = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is invalid"));
        refreshTokens.save(new RefreshToken(
                token.id(),
                token.userId(),
                token.tokenHash(),
                token.expiresAt(),
                Instant.now(),
                token.createdAt()
        ));
    }

    private AuthResponse issueTokens(UserAccount user) {
        JwtService.TokenIssue access = jwtService.issueAccessToken(user, rolesFor(user.id()));
        String refreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        refreshTokens.save(new RefreshToken(
                UUID.randomUUID(),
                user.id(),
                sha256(refreshToken),
                Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS),
                null,
                Instant.now()
        ));
        return new AuthResponse(user.id(), user.email(), access.token(), refreshToken, access.expiresAt());
    }

    private void assignUserRole(UUID userId) {
        jdbcTemplate.update("""
                insert into user_roles (user_id, role_id)
                select ?, id from roles where code = 'USER'
                on conflict do nothing
                """, userId);
    }

    private List<String> rolesFor(UUID userId) {
        List<String> roles = jdbcTemplate.queryForList("""
                select r.code
                from roles r
                join user_roles ur on ur.role_id = r.id
                where ur.user_id = ?
                order by r.code
                """, String.class, userId);
        return roles.isEmpty() ? List.of("USER") : roles;
    }

    private void validateRegistration(RegisterRequest request) {
        if (request.email() == null || !request.email().contains("@")) {
            throw new IllegalArgumentException("Email is invalid");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        if (request.firstName() == null || request.firstName().isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (request.lastName() == null || request.lastName().isBlank()) {
            throw new IllegalArgumentException("Last name is required");
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
