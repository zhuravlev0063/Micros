package ru.kubsu.flights.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuthEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(AuthEventConsumer.class);

    @KafkaListener(topics = "search.request-created", groupId = "auth-service-audit")
    public void onSearchRequestCreated(String payload) {
        log.info("Audit event from Search Service: {}", payload);
    }
}
