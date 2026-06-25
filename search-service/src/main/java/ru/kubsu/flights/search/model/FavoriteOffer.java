package ru.kubsu.flights.search.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("favorite_offers")
public record FavoriteOffer(@Id UUID id, UUID userId, UUID flightOfferId, Instant createdAt) {
}
