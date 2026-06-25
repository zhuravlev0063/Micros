package ru.kubsu.flights.search.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("flight_offers")
public record FlightOffer(
        @Id UUID id,
        UUID searchRequestId,
        String providerCode,
        String airline,
        String flightNumber,
        BigDecimal price,
        String currency,
        Instant departureAt,
        Instant arrivalAt,
        int transfersCount,
        String bookingUrl
) {
}
