package com.recoverai.recovery;

import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.*;
import com.recoverai.ollama.OllamaStrategyClient;
import com.recoverai.ollama.OllamaUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecoveryStrategyServiceTest {
    @Test void usesOllamaRecommendationWhenItIsValidAndEnabled() {
        RecoveryStrategyService service = service(true, c -> new StrategyDecision("TRANSIENT_PAYMENT_FAILURE", .86, RecoveryAction.CREATE_PAYMENT_LINK, 0, List.of("Transient failure", "Strong history"), StrategySource.OLLAMA));
        assertEquals(StrategySource.OLLAMA, service.decide(caseOf(499900)).decision().source());
    }

    @Test void fallsBackWhenOllamaIsUnavailable() {
        RecoveryStrategyService service = service(true, c -> { throw new OllamaUnavailableException("OLLAMA_UNAVAILABLE", "offline"); });
        RecoveryStrategyService.StrategyResolution result = service.decide(caseOf(499900));
        assertEquals(StrategySource.DETERMINISTIC_FALLBACK, result.decision().source());
        assertEquals("OLLAMA_UNAVAILABLE", result.fallbackReason());
    }

    @Test void highValueOllamaRecommendationIsEscalatedByThresholds() {
        RecoveryStrategyService service = service(true, c -> new StrategyDecision("TRANSIENT_PAYMENT_FAILURE", .99, RecoveryAction.CREATE_PAYMENT_LINK, 0, List.of("High score"), StrategySource.OLLAMA));
        assertEquals(RecoveryAction.ESCALATE_TO_HUMAN, service.decide(caseOf(5_000_001)).decision().recommendedAction());
    }

    @Test void checkoutPaymentLinkRecommendationIsDowngradedByThresholds() {
        RecoveryStrategyService service = service(true, c -> new StrategyDecision("CHECKOUT_ABANDONMENT", .90, RecoveryAction.CREATE_PAYMENT_LINK, 0, List.of("Candidate action"), StrategySource.OLLAMA));
        RecoveryCase checkout = new RecoveryCase("checkout", "RCV-2", "Demo", null, true, RiskType.CHECKOUT_ABANDONMENT, 499900, "INR", "CARD", "expired", TransactionStatus.CREATED, 4, 0, 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), null, 0, Instant.now(), Instant.now(), null);
        assertEquals(RecoveryAction.WAIT_AND_RETRY, service.decide(checkout).decision().recommendedAction());
    }

    @Test void disabledOllamaUsesFallbackWithoutCallingClient() {
        RecoveryStrategyService service = service(false, c -> { throw new AssertionError("Ollama must not be called"); });
        assertEquals(StrategySource.DETERMINISTIC_FALLBACK, service.decide(caseOf(499900)).decision().source());
    }

    private RecoveryStrategyService service(boolean enabled, OllamaStrategyClient client) {
        RecoveryProperties properties = new RecoveryProperties("memory", false, null, null, new RecoveryProperties.Recovery(3, 24, false), new RecoveryProperties.Thresholds(1, .70, .60, .60, 5_000_000), new RecoveryProperties.Ollama(enabled, "http://localhost:11434", "qwen2.5:3b", 30), new RecoveryProperties.Security(false), new RecoveryProperties.Detection(false), "http://localhost:5173");
        ThresholdStrategyEngine thresholds = new ThresholdStrategyEngine(properties);
        return new RecoveryStrategyService(properties, client, new FallbackStrategyEngine(properties, thresholds), thresholds);
    }

    private RecoveryCase caseOf(long amount) { Instant now = Instant.now(); return new RecoveryCase("case", "RCV-1", "Demo", null, true, RiskType.PAYMENT_FAILURE, amount, "INR", "UPI", "timeout", TransactionStatus.FAILED, 7, 0, 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), null, 0, now, now, null); }
}
