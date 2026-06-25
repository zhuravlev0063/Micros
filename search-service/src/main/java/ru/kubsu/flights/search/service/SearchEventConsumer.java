package ru.kubsu.flights.search.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SearchEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(SearchEventConsumer.class);

    @KafkaListener(topics = "auth.user-registered", groupId = "search-service-audit")
    public void onUserRegistered(String payload) {
        log.info("Audit event from Auth Service: {}", payload);
    }
}
