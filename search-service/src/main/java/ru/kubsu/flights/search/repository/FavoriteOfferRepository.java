package ru.kubsu.flights.search.repository;

import org.springframework.data.repository.CrudRepository;
import ru.kubsu.flights.search.model.FavoriteOffer;

import java.util.UUID;

public interface FavoriteOfferRepository extends CrudRepository<FavoriteOffer, UUID> {
}
