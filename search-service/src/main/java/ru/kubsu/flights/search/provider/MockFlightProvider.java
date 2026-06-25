package ru.kubsu.flights.search.provider;

import org.springframework.stereotype.Component;
import ru.kubsu.flights.search.dto.FlightOfferDto;
import ru.kubsu.flights.search.dto.FlightSearchRequestDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Component
public class MockFlightProvider implements FlightProvider {
    @Override
    public String code() {
        return "mock-air";
    }

    @Override
    public List<FlightOfferDto> search(FlightSearchRequestDto request) {
        ZoneId zone = ZoneId.of("Europe/Moscow");
        LocalDateTime morning = request.departureDate().atTime(8, 30);
        LocalDateTime evening = request.departureDate().atTime(19, 10);
        BigDecimal basePrice = BigDecimal.valueOf(6400L + request.passengers() * 1500L);
        return List.of(
                new FlightOfferDto(
                        UUID.randomUUID(),
                        code(),
                        "KubSU Airlines",
                        "KU-217",
                        basePrice,
                        "RUB",
                        morning.atZone(zone).toInstant(),
                        morning.plusHours(3).plusMinutes(40).atZone(zone).toInstant(),
                        0,
                        "https://example.local/book/KU-217"
                ),
                new FlightOfferDto(
                        UUID.randomUUID(),
                        code(),
                        "Spring Jet",
                        "SJ-404",
                        basePrice.subtract(BigDecimal.valueOf(900)),
                        "RUB",
                        evening.atZone(zone).toInstant(),
                        evening.plusHours(5).plusMinutes(20).atZone(zone).toInstant(),
                        1,
                        "https://example.local/book/SJ-404"
                )
        );
    }
}
