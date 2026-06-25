package ru.kubsu.flights.search.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kubsu.flights.search.dto.FlightOfferDto;
import ru.kubsu.flights.search.dto.FlightSearchRequestDto;
import ru.kubsu.flights.search.dto.SearchResponse;
import ru.kubsu.flights.search.model.FavoriteOffer;
import ru.kubsu.flights.search.model.FlightOffer;
import ru.kubsu.flights.search.model.SearchRequest;
import ru.kubsu.flights.search.provider.FlightProvider;
import ru.kubsu.flights.search.repository.FavoriteOfferRepository;
import ru.kubsu.flights.search.repository.FlightOfferRepository;
import ru.kubsu.flights.search.repository.SearchRequestRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SearchService {
    private final List<FlightProvider> providers;
    private final SearchRequestRepository requests;
    private final FlightOfferRepository offers;
    private final FavoriteOfferRepository favorites;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public SearchService(
            List<FlightProvider> providers,
            SearchRequestRepository requests,
            FlightOfferRepository offers,
            FavoriteOfferRepository favorites,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.providers = providers;
        this.requests = requests;
        this.offers = offers;
        this.favorites = favorites;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public SearchResponse search(UUID userId, FlightSearchRequestDto dto) {
        validate(dto);
        SearchRequest savedRequest = requests.save(new SearchRequest(
                UUID.randomUUID(),
                userId,
                dto.origin().trim().toUpperCase(),
                dto.destination().trim().toUpperCase(),
                dto.departureDate(),
                dto.returnDate(),
                dto.passengers(),
                Instant.now()
        ));

        List<FlightOfferDto> savedOffers = providers.stream()
                .flatMap(provider -> safeProviderSearch(provider, dto).stream())
                .map(offer -> saveOffer(savedRequest.id(), offer))
                .sorted(Comparator.comparing(FlightOfferDto::price))
                .toList();

        kafkaTemplate.send("search.request-created", userId.toString(), "{\"searchRequestId\":\"" + savedRequest.id() + "\"}");
        return new SearchResponse(savedRequest.id(), savedOffers);
    }

    public List<SearchRequest> history(UUID userId) {
        return requests.findRecentByUserId(userId);
    }

    public FavoriteOffer addFavorite(UUID userId, UUID flightOfferId) {
        if (!offers.existsById(flightOfferId)) {
            throw new IllegalArgumentException("Flight offer not found");
        }
        return favorites.save(new FavoriteOffer(UUID.randomUUID(), userId, flightOfferId, Instant.now()));
    }

    private List<FlightOfferDto> safeProviderSearch(FlightProvider provider, FlightSearchRequestDto dto) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return CompletableFuture
                        .supplyAsync(() -> provider.search(dto))
                        .get(2, TimeUnit.SECONDS);
            } catch (Exception exception) {
                lastError = exception instanceof RuntimeException runtimeException
                        ? runtimeException
                        : new RuntimeException(exception);
            }
        }
        kafkaTemplate.send(
                "search.provider-failed",
                provider.code(),
                "{\"provider\":\"" + provider.code() + "\",\"error\":\"" + lastError.getClass().getSimpleName() + "\"}"
        );
        return List.of();
    }

    private FlightOfferDto saveOffer(UUID searchRequestId, FlightOfferDto dto) {
        FlightOffer offer = offers.save(new FlightOffer(
                UUID.randomUUID(),
                searchRequestId,
                dto.providerCode(),
                dto.airline(),
                dto.flightNumber(),
                dto.price(),
                dto.currency(),
                dto.departureAt(),
                dto.arrivalAt(),
                dto.transfersCount(),
                dto.bookingUrl()
        ));
        return new FlightOfferDto(
                offer.id(),
                offer.providerCode(),
                offer.airline(),
                offer.flightNumber(),
                offer.price(),
                offer.currency(),
                offer.departureAt(),
                offer.arrivalAt(),
                offer.transfersCount(),
                offer.bookingUrl()
        );
    }

    private void validate(FlightSearchRequestDto dto) {
        if (dto.origin() == null || dto.origin().isBlank()) {
            throw new IllegalArgumentException("Origin is required");
        }
        if (dto.destination() == null || dto.destination().isBlank()) {
            throw new IllegalArgumentException("Destination is required");
        }
        if (dto.departureDate() == null || dto.departureDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Departure date must not be in the past");
        }
        if (dto.passengers() < 1 || dto.passengers() > 9) {
            throw new IllegalArgumentException("Passengers count must be between 1 and 9");
        }
    }
}
