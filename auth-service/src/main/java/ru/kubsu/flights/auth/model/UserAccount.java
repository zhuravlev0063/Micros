package ru.kubsu.flights.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("users")
public record UserAccount(
        @Id UUID id,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
