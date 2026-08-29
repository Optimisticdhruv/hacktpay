package com.recoverai.ollama;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.RecoveryAction;
import com.recoverai.domain.StrategyDecision;
import com.recoverai.domain.StrategySource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpOllamaStrategyClientTest {
    private final HttpOllamaStrategyClient client = new HttpOllamaStrategyClient(properties(), new ObjectMapper());

    @Test void acceptsStrictValidStructuredResponse() {
        StrategyDecision decision = client.toDecision(new HttpOllamaStrategyClient.OllamaResponse("TRANSIENT_PAYMENT_FAILURE", .86, "CREATE_PAYMENT_LINK", 0, List.of("Transient failure", "Strong history")));
        assertEquals(StrategySource.OLLAMA, decision.source());
        assertEquals(RecoveryAction.CREATE_PAYMENT_LINK, decision.recommendedAction());
    }

    @Test void normalizesPercentageScoresAndCommonActionAliases() {
        StrategyDecision decision = client.toDecision(new HttpOllamaStrategyClient.OllamaResponse("Payment timeout detected", 72.0, "retry", 5, List.of("Payment timed out")));
        assertEquals("PAYMENT_TIMEOUT_DETECTED", decision.diagnosis());
        assertEquals(.72, decision.recoverabilityScore());
        assertEquals(RecoveryAction.WAIT_AND_RETRY, decision.recommendedAction());
    }

    @Test void rejectsNegativeScoreAndUnknownAction() {
        assertThrows(OllamaUnavailableException.class, () -> client.toDecision(new HttpOllamaStrategyClient.OllamaResponse("TRANSIENT_PAYMENT_FAILURE", 100.01, "CREATE_PAYMENT_LINK", 0, List.of("reason"))));
        assertThrows(OllamaUnavailableException.class, () -> client.toDecision(new HttpOllamaStrategyClient.OllamaResponse("TRANSIENT_PAYMENT_FAILURE", -.01, "CREATE_PAYMENT_LINK", 0, List.of("reason"))));
        assertThrows(OllamaUnavailableException.class, () -> client.toDecision(new HttpOllamaStrategyClient.OllamaResponse("TRANSIENT_PAYMENT_FAILURE", .8, "TRANSFER_MONEY", 0, List.of("reason"))));
    }

    private static RecoveryProperties properties() {
        return new RecoveryProperties("memory", false, null, null, new RecoveryProperties.Recovery(3, 24, false), new RecoveryProperties.Thresholds(1, .70, .60, .60, 5_000_000), new RecoveryProperties.Ollama(true, "http://localhost:11434", "llama3.2:latest", 30), new RecoveryProperties.Security(false), new RecoveryProperties.Detection(false), "http://localhost:5173");
    }
}
