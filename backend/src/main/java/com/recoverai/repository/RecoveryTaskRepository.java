package com.recoverai.repository;
import com.recoverai.domain.RecoveryTask;
import java.util.List;
import java.util.Optional;
public interface RecoveryTaskRepository { RecoveryTask save(RecoveryTask task); Optional<RecoveryTask> findById(String id); List<RecoveryTask> findByRecoveryCaseId(String recoveryCaseId); }
