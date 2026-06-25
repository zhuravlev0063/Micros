package ru.kubsu.flights.search.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import ru.kubsu.flights.search.model.FlightOffer;

import java.util.List;
import java.util.UUID;

public interface FlightOfferRepository extends CrudRepository<FlightOffer, UUID> {
    @Query("select * from flight_offers where search_request_id = :searchRequestId order by price asc")
    List<FlightOffer> findBySearchRequestIdOrderByPrice(UUID searchRequestId);
}
