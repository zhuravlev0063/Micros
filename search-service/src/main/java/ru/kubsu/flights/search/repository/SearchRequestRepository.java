package ru.kubsu.flights.search.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import ru.kubsu.flights.search.model.SearchRequest;

import java.util.List;
import java.util.UUID;

public interface SearchRequestRepository extends CrudRepository<SearchRequest, UUID> {
    @Query("select * from search_requests where user_id = :userId order by created_at desc limit 20")
    List<SearchRequest> findRecentByUserId(UUID userId);
}
