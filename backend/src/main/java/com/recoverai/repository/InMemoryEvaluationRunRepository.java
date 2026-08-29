package com.recoverai.repository;

import com.recoverai.domain.EvaluationRun;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryEvaluationRunRepository implements EvaluationRunRepository {
    private final Map<String, EvaluationRun> runs = new ConcurrentHashMap<>();
    public EvaluationRun save(EvaluationRun run) { runs.put(run.id(), run); return run; }
    public Optional<EvaluationRun> latest() { return findRecent(1).stream().findFirst(); }
    public List<EvaluationRun> findRecent(int limit) { return runs.values().stream().sorted(Comparator.comparing(EvaluationRun::createdAt).reversed()).limit(Math.max(1, limit)).toList(); }
}
