package com.recoverai.repository;

import com.recoverai.domain.RecoveryCase;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryRecoveryCaseRepository implements RecoveryCaseRepository {
    private final Map<String, RecoveryCase> cases = new ConcurrentHashMap<>();
    public RecoveryCase save(RecoveryCase recoveryCase) { cases.put(recoveryCase.id(), recoveryCase); return recoveryCase; }
    public Optional<RecoveryCase> findById(String id) { return Optional.ofNullable(cases.get(id)); }
    public List<RecoveryCase> findAll() { return cases.values().stream().sorted(Comparator.comparing(RecoveryCase::updatedAt).reversed()).toList(); }
}
