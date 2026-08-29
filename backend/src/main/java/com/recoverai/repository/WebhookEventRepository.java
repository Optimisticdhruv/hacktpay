package com.recoverai.repository;

import com.recoverai.domain.WebhookEvent;
import java.util.Optional;

public interface WebhookEventRepository {
    Optional<WebhookEvent> findByEventId(String eventId);
    WebhookEvent save(WebhookEvent event);
}
