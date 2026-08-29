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

    @Test void rejectsScoreAboveOne() {
        assertThrows(OllamaUnavailableException.class, () -> client.toDecision(new HttpOllamaStrategyClient.OllamaResponse("TRANSIENT_PAYMENT_FAILURE", 1.01, "CREATE_PAYMENT_LINK", 0, List.of("reason"))));
    }

    @Test void rejectsNegativeScoreAndUnknownAction() {
        assertThrows(OllamaUnavailableException.class, () -> client.toDecision(new HttpOllamaStrategyClient.OllamaResponse("TRANSIENT_PAYMENT_FAILURE", -.01, "CREATE_PAYMENT_LINK", 0, List.of("reason"))));
        assertThrows(OllamaUnavailableException.class, () -> client.toDecision(new HttpOllamaStrategyClient.OllamaResponse("TRANSIENT_PAYMENT_FAILURE", .8, "TRANSFER_MONEY", 0, List.of("reason"))));
    }

    private static RecoveryProperties properties() {
        return new RecoveryProperties("memory", false, null, null, new RecoveryProperties.Recovery(3, 24, false), new RecoveryProperties.Thresholds(1, .70, .60, .60, 5_000_000), new RecoveryProperties.Ollama(true, "http://localhost:11434", "llama3.2:latest", 30), "http://localhost:5173");
    }
}
