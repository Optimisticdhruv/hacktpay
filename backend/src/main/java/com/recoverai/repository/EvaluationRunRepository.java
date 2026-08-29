package com.recoverai.repository;

import com.recoverai.domain.EvaluationRun;
import java.util.List;
import java.util.Optional;

public interface EvaluationRunRepository {
    EvaluationRun save(EvaluationRun run);
    Optional<EvaluationRun> latest();
    List<EvaluationRun> findRecent(int limit);
}
