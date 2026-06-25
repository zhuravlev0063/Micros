package ru.kubsu.flights.auth.repository;

import org.springframework.data.repository.CrudRepository;
import ru.kubsu.flights.auth.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
