package ru.kubsu.flights.search.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Table("search_requests")
public record SearchRequest(
        @Id UUID id,
        UUID userId,
        String originIata,
        String destinationIata,
        LocalDate departureDate,
        LocalDate returnDate,
        int passengers,
        Instant createdAt
) {
}
