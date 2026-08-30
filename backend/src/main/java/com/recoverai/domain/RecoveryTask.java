package com.recoverai.domain;
import java.time.Instant;
public record RecoveryTask(String id, String recoveryCaseId, RecoveryTaskType type, RecoveryTaskStatus status, Instant dueAt, Instant createdAt, Instant completedAt) {}
