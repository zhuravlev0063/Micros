package ru.kubsu.flights.search.provider;

import ru.kubsu.flights.search.dto.FlightOfferDto;
import ru.kubsu.flights.search.dto.FlightSearchRequestDto;

import java.util.List;

public interface FlightProvider {
    String code();

    List<FlightOfferDto> search(FlightSearchRequestDto request);
}
