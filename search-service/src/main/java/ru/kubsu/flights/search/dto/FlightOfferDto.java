package ru.kubsu.flights.search.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FlightOfferDto(
        UUID id,
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
