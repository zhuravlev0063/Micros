package ru.kubsu.flights.search.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.kubsu.flights.search.dto.FlightSearchRequestDto;
import ru.kubsu.flights.search.dto.SearchResponse;
import ru.kubsu.flights.search.model.FavoriteOffer;
import ru.kubsu.flights.search.model.SearchRequest;
import ru.kubsu.flights.search.service.SearchService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/flights")
    SearchResponse search(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
            @RequestParam(defaultValue = "1") int passengers
    ) {
        return searchService.search(userId, new FlightSearchRequestDto(origin, destination, departureDate, returnDate, passengers));
    }

    @GetMapping("/history")
    List<SearchRequest> history(@RequestHeader("X-User-Id") UUID userId) {
        return searchService.history(userId);
    }

    @PostMapping("/favorites")
    FavoriteOffer favorite(@RequestHeader("X-User-Id") UUID userId, @RequestParam UUID flightOfferId) {
        return searchService.addFavorite(userId, flightOfferId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
