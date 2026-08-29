package com.recoverai.repository;

import com.recoverai.domain.AuditEvent;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(prefix = "recoverai", name = "storage-mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuditRepository implements AuditRepository {
    private final List<AuditEvent> events = new CopyOnWriteArrayList<>();
    public AuditEvent save(AuditEvent auditEvent) { events.add(auditEvent); return auditEvent; }
    public List<AuditEvent> findByRecoveryCaseId(String recoveryCaseId) { return events.stream().filter(e -> e.recoveryCaseId().equals(recoveryCaseId)).sorted(Comparator.comparing(AuditEvent::createdAt)).toList(); }
}
