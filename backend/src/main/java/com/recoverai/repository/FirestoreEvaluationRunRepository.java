package com.recoverai.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.recoverai.domain.EvaluationRun;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "firestore")
public class FirestoreEvaluationRunRepository implements EvaluationRunRepository {
    private final Firestore db; private final ObjectMapper json;
    public FirestoreEvaluationRunRepository(Firestore db, ObjectMapper json) { this.db = db; this.json = json; }
    public EvaluationRun save(EvaluationRun run) { try { db.collection("evaluationRuns").document(run.id()).set(Map.of("payload", json.writeValueAsString(run), "createdAt", run.createdAt().toString())).get(); return run; } catch (Exception e) { throw new IllegalStateException("Firestore could not save evaluation run", e); } }
    public Optional<EvaluationRun> latest() { return findRecent(1).stream().findFirst(); }
    public List<EvaluationRun> findRecent(int limit) { try { return db.collection("evaluationRuns").orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING).limit(Math.max(1, limit)).get().get().getDocuments().stream().map(d -> read(d.getString("payload"))).toList(); } catch (Exception e) { throw new IllegalStateException("Firestore could not read evaluation history", e); } }
    private EvaluationRun read(String payload) { try { return json.readValue(payload, EvaluationRun.class); } catch (Exception e) { throw new IllegalStateException("Invalid Firestore evaluation run", e); } }
}
