package com.recoverai.repository;

import com.recoverai.domain.WebhookEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryWebhookEventRepository implements WebhookEventRepository {
    private final ConcurrentMap<String, WebhookEvent> events = new ConcurrentHashMap<>();
    public Optional<WebhookEvent> findByEventId(String eventId) { return Optional.ofNullable(events.get(eventId)); }
    public WebhookEvent save(WebhookEvent event) { events.putIfAbsent(event.eventId(), event); return events.get(event.eventId()); }
}
