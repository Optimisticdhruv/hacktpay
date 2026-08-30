package com.recoverai.repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.recoverai.domain.RecoveryTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "firestore")
public class FirestoreRecoveryTaskRepository implements RecoveryTaskRepository {
    private final Firestore db; private final ObjectMapper json;
    public FirestoreRecoveryTaskRepository(Firestore db, ObjectMapper json) { this.db = db; this.json = json; }
    public RecoveryTask save(RecoveryTask task) { try { db.collection("recoveryTasks").document(task.id()).set(Map.of("payload", json.writeValueAsString(task), "recoveryCaseId", task.recoveryCaseId(), "dueAt", task.dueAt().toString())).get(); return task; } catch (Exception exception) { throw new IllegalStateException("Firestore could not save recovery task", exception); } }
    public Optional<RecoveryTask> findById(String id) { try { var document = db.collection("recoveryTasks").document(id).get().get(); return document.exists() ? Optional.of(read(document.getString("payload"))) : Optional.empty(); } catch (Exception exception) { throw new IllegalStateException("Firestore could not read recovery task", exception); } }
    public List<RecoveryTask> findByRecoveryCaseId(String recoveryCaseId) { try { return db.collection("recoveryTasks").whereEqualTo("recoveryCaseId", recoveryCaseId).get().get().getDocuments().stream().map(document -> read(document.getString("payload"))).sorted(Comparator.comparing(RecoveryTask::dueAt)).toList(); } catch (Exception exception) { throw new IllegalStateException("Firestore could not list recovery tasks", exception); } }
    private RecoveryTask read(String payload) { try { return json.readValue(payload, RecoveryTask.class); } catch (Exception exception) { throw new IllegalStateException("Invalid Firestore recovery task", exception); } }
}
