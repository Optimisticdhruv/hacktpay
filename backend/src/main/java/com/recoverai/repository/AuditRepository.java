package com.recoverai.repository;

import com.recoverai.domain.AuditEvent;
import java.util.List;

public interface AuditRepository { AuditEvent save(AuditEvent auditEvent); List<AuditEvent> findByRecoveryCaseId(String recoveryCaseId); }
