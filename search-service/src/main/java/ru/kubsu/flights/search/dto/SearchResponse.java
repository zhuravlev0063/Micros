package ru.kubsu.flights.search.dto;

import java.util.List;
import java.util.UUID;

public record SearchResponse(UUID searchRequestId, List<FlightOfferDto> offers) {
}
