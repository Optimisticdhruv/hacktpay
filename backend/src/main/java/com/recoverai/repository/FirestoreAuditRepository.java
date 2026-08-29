package com.recoverai.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.recoverai.domain.AuditEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "firestore")
public class FirestoreAuditRepository implements AuditRepository {
    private final Firestore db; private final ObjectMapper json;
    public FirestoreAuditRepository(Firestore db, ObjectMapper json) { this.db = db; this.json = json; }
    public AuditEvent save(AuditEvent event) { try { db.collection("auditEvents").document(event.id()).set(Map.of("payload", json.writeValueAsString(event), "recoveryCaseId", event.recoveryCaseId(), "createdAt", event.createdAt().toString())).get(); return event; } catch (Exception e) { throw new IllegalStateException("Firestore could not save audit event", e); } }
    public List<AuditEvent> findByRecoveryCaseId(String id) { try { return db.collection("auditEvents").whereEqualTo("recoveryCaseId", id).get().get().getDocuments().stream().map(d -> { try { return json.readValue(d.getString("payload"), AuditEvent.class); } catch (Exception e) { throw new IllegalStateException("Invalid Firestore audit event", e); } }).sorted(Comparator.comparing(AuditEvent::createdAt)).toList(); } catch (Exception e) { throw new IllegalStateException("Firestore could not list audit events", e); } }
}
