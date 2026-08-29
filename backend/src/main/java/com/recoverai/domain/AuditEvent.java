package com.recoverai.domain;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(String id, String recoveryCaseId, String actorType, String eventType, String message,
                         Map<String, Object> metadata, Instant createdAt) {}
