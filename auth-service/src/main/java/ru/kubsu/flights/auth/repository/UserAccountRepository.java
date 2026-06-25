package ru.kubsu.flights.auth.repository;

import org.springframework.data.repository.CrudRepository;
import ru.kubsu.flights.auth.model.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends CrudRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
