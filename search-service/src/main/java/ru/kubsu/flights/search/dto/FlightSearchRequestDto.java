package ru.kubsu.flights.search.dto;

import java.time.LocalDate;

public record FlightSearchRequestDto(
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        int passengers
) {
}
