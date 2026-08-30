package com.recoverai.repository;
import com.recoverai.domain.RecoveryTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryRecoveryTaskRepository implements RecoveryTaskRepository {
    private final Map<String, RecoveryTask> tasks = new ConcurrentHashMap<>();
    public RecoveryTask save(RecoveryTask task) { tasks.put(task.id(), task); return task; }
    public Optional<RecoveryTask> findById(String id) { return Optional.ofNullable(tasks.get(id)); }
    public List<RecoveryTask> findByRecoveryCaseId(String recoveryCaseId) { return tasks.values().stream().filter(task -> task.recoveryCaseId().equals(recoveryCaseId)).sorted(Comparator.comparing(RecoveryTask::dueAt)).toList(); }
}
