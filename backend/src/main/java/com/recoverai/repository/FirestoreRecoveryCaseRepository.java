package com.recoverai.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.recoverai.domain.RecoveryCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "firestore")
public class FirestoreRecoveryCaseRepository implements RecoveryCaseRepository {
    private final Firestore db; private final ObjectMapper json;
    public FirestoreRecoveryCaseRepository(Firestore db, ObjectMapper json) { this.db = db; this.json = json; }
    public RecoveryCase save(RecoveryCase c) { try { db.collection("recoveryCases").document(c.id()).set(Map.of("payload", json.writeValueAsString(c), "status", c.status().name(), "updatedAt", c.updatedAt().toString())).get(); return c; } catch (Exception e) { throw new IllegalStateException("Firestore could not save recovery case", e); } }
    public Optional<RecoveryCase> findById(String id) { try { var snapshot = db.collection("recoveryCases").document(id).get().get(); return snapshot.exists() ? Optional.of(json.readValue(snapshot.getString("payload"), RecoveryCase.class)) : Optional.empty(); } catch (Exception e) { throw new IllegalStateException("Firestore could not read recovery case", e); } }
    public List<RecoveryCase> findAll() { try { return db.collection("recoveryCases").get().get().getDocuments().stream().map(d -> { try { return json.readValue(d.getString("payload"), RecoveryCase.class); } catch (Exception e) { throw new IllegalStateException("Invalid Firestore recovery case", e); } }).sorted(Comparator.comparing(RecoveryCase::updatedAt).reversed()).toList(); } catch (Exception e) { throw new IllegalStateException("Firestore could not list recovery cases", e); } }
}
