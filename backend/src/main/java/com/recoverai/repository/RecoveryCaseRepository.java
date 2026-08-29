package com.recoverai.repository;

import com.recoverai.domain.RecoveryCase;
import java.util.List;
import java.util.Optional;

public interface RecoveryCaseRepository {
    RecoveryCase save(RecoveryCase recoveryCase);
    Optional<RecoveryCase> findById(String id);
    List<RecoveryCase> findAll();
}
