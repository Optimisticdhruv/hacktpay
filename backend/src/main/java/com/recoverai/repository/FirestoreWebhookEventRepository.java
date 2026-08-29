package com.recoverai.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.recoverai.domain.WebhookEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "firestore")
public class FirestoreWebhookEventRepository implements WebhookEventRepository {
    private final Firestore db; private final ObjectMapper json;
    public FirestoreWebhookEventRepository(Firestore db, ObjectMapper json) { this.db = db; this.json = json; }
    public Optional<WebhookEvent> findByEventId(String eventId) { try { var doc = db.collection("webhookEvents").document(eventId).get().get(); return doc.exists() ? Optional.of(json.readValue(doc.getString("payload"), WebhookEvent.class)) : Optional.empty(); } catch (Exception e) { throw new IllegalStateException("Firestore could not read webhook event", e); } }
    public WebhookEvent save(WebhookEvent event) { try { db.collection("webhookEvents").document(event.eventId()).create(Map.of("payload", json.writeValueAsString(event), "eventType", event.eventType(), "receivedAt", event.receivedAt().toString())).get(); return event; } catch (Exception e) { throw new IllegalStateException("Firestore could not save webhook event", e); } }
}
