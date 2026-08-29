package com.recoverai.recovery;

import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.RecoveryCase;
import com.recoverai.domain.StrategyDecision;
import com.recoverai.ollama.OllamaStrategyClient;
import com.recoverai.ollama.OllamaUnavailableException;
import org.springframework.stereotype.Service;

/** Selects a recommendation source; it never authorizes or executes a recovery action. */
@Service
public class RecoveryStrategyService {
    private final RecoveryProperties properties;
    private final OllamaStrategyClient ollama;
    private final FallbackStrategyEngine fallback;
    private final ThresholdStrategyEngine thresholds;

    public RecoveryStrategyService(RecoveryProperties properties, OllamaStrategyClient ollama, FallbackStrategyEngine fallback, ThresholdStrategyEngine thresholds) {
        this.properties = properties;
        this.ollama = ollama;
        this.fallback = fallback;
        this.thresholds = thresholds;
    }

    public StrategyResolution decide(RecoveryCase recoveryCase) {
        if (!properties.ollama().enabled()) return fallback(recoveryCase, "OLLAMA_DISABLED");
        try {
            return new StrategyResolution(thresholds.qualify(recoveryCase, ollama.recommend(recoveryCase)), null);
        } catch (OllamaUnavailableException e) {
            return fallback(recoveryCase, e.reason());
        }
    }

    private StrategyResolution fallback(RecoveryCase recoveryCase, String reason) {
        return new StrategyResolution(fallback.decide(recoveryCase), reason);
    }

    public record StrategyResolution(StrategyDecision decision, String fallbackReason) {}
}
