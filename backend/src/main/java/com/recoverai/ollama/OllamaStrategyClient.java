package com.recoverai.ollama;

import com.recoverai.domain.RecoveryCase;
import com.recoverai.domain.StrategyDecision;

public interface OllamaStrategyClient {
    StrategyDecision recommend(RecoveryCase recoveryCase);
}
