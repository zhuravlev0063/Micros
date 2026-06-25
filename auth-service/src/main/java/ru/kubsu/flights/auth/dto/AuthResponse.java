package ru.kubsu.flights.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(UUID userId, String email, String accessToken, String refreshToken, Instant expiresAt) {
}
