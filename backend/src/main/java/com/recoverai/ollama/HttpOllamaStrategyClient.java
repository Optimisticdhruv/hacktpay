package com.recoverai.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.RecoveryAction;
import com.recoverai.domain.RecoveryCase;
import com.recoverai.domain.StrategyDecision;
import com.recoverai.domain.StrategySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/** Local-only Ollama client. It sends a reduced operational context and no customer PII or secrets. */
@Service
public class HttpOllamaStrategyClient implements OllamaStrategyClient {
    private static final String SYSTEM_PROMPT = """
            You are the strategy reasoning component of RecoverAI. You only analyze a recovery case and recommend one strategy.
            You cannot execute payments, contact customers, or change recovery state. The deterministic threshold and policy engines authorize all actions.
            Use only the supplied fields. Never claim payment success unless transactionStatus is CAPTURED. Do not recommend customer contact when contactAllowed is false.
            Return JSON only with diagnosis, recoverabilityScore (0.0 to 1.0), recommendedAction, delayMinutes, and concise evidence-based reasons.
            Allowed recommendedAction values: NO_ACTION, WAIT_AND_RETRY, SEND_REMINDER, CREATE_PAYMENT_LINK, ESCALATE_TO_HUMAN.
            Prefer ESCALATE_TO_HUMAN when the case is unsafe or ambiguous.
            """;
    private final RecoveryProperties properties;
    private final ObjectMapper json;
    private final HttpClient http;

    @Autowired
    public HttpOllamaStrategyClient(RecoveryProperties properties, ObjectMapper json) {
        this(properties, json, HttpClient.newBuilder().build());
    }

    HttpOllamaStrategyClient(RecoveryProperties properties, ObjectMapper json, HttpClient http) {
        this.properties = properties;
        this.json = json;
        this.http = http;
    }

    @Override
    public StrategyDecision recommend(RecoveryCase recoveryCase) {
        try {
            RecoveryProperties.Ollama config = properties.ollama();
            ObjectNode request = json.createObjectNode();
            request.put("model", config.model());
            request.put("stream", false);
            request.put("system", SYSTEM_PROMPT);
            request.put("prompt", json.writeValueAsString(OllamaCaseContext.from(recoveryCase)));
            request.set("format", responseSchema());
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(normalizeUrl(config.baseUrl()) + "/api/generate"))
                    .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new OllamaUnavailableException("OLLAMA_HTTP_ERROR", "Ollama returned HTTP " + response.statusCode());
            JsonNode root = json.readTree(response.body());
            String body = root.path("response").asText();
            if (body.isBlank()) throw new OllamaUnavailableException("EMPTY_RESPONSE", "Ollama returned an empty response");
            return toDecision(json.readValue(body, OllamaResponse.class));
        } catch (OllamaUnavailableException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new OllamaUnavailableException("OLLAMA_TIMEOUT", "Ollama request timed out", e);
        } catch (java.net.ConnectException e) {
            throw new OllamaUnavailableException("OLLAMA_UNAVAILABLE", "Ollama is not reachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaUnavailableException("OLLAMA_INTERRUPTED", "Ollama request was interrupted", e);
        } catch (Exception e) {
            throw new OllamaUnavailableException("RESPONSE_PARSE_ERROR", "Ollama response could not be validated", e);
        }
    }

    StrategyDecision toDecision(OllamaResponse response) {
        if (response == null || response.diagnosis() == null || !response.diagnosis().matches("[A-Za-z0-9_]{3,80}")) throw new OllamaUnavailableException("INVALID_AI_RESPONSE", "Ollama diagnosis is invalid");
        if (response.recoverabilityScore() == null || !Double.isFinite(response.recoverabilityScore()) || response.recoverabilityScore() < 0 || response.recoverabilityScore() > 1) throw new OllamaUnavailableException("INVALID_SCORE", "Ollama score is invalid");
        if (response.recommendedAction() == null) throw new OllamaUnavailableException("INVALID_ACTION", "Ollama action is missing");
        RecoveryAction action;
        try { action = RecoveryAction.valueOf(response.recommendedAction().trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new OllamaUnavailableException("INVALID_ACTION", "Ollama action is unsupported", e); }
        if (response.delayMinutes() == null || response.delayMinutes() < 0 || response.delayMinutes() > 43_200) throw new OllamaUnavailableException("INVALID_AI_RESPONSE", "Ollama delay is invalid");
        if (response.reasons() == null || response.reasons().isEmpty() || response.reasons().stream().anyMatch(reason -> reason == null || reason.isBlank())) throw new OllamaUnavailableException("INVALID_AI_RESPONSE", "Ollama reasons are invalid");
        return new StrategyDecision(response.diagnosis().toUpperCase(Locale.ROOT), response.recoverabilityScore(), action, response.delayMinutes(), response.reasons().stream().map(String::trim).limit(5).toList(), StrategySource.OLLAMA);
    }

    private ObjectNode responseSchema() {
        ObjectNode schema = json.createObjectNode(); schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("diagnosis").put("type", "string");
        properties.putObject("recoverabilityScore").put("type", "number");
        properties.putObject("recommendedAction").put("type", "string");
        properties.putObject("delayMinutes").put("type", "integer");
        ObjectNode reasons = properties.putObject("reasons"); reasons.put("type", "array"); reasons.putObject("items").put("type", "string");
        schema.putArray("required").add("diagnosis").add("recoverabilityScore").add("recommendedAction").add("delayMinutes").add("reasons");
        return schema;
    }

    private String normalizeUrl(String url) { return url == null ? "http://localhost:11434" : url.replaceAll("/+$", ""); }

    record OllamaCaseContext(String caseReference, String riskType, long amountAtRisk, String currency, String paymentMethod,
                             String failureReason, String transactionStatus, int previousSuccessfulPayments, int previousFailedPayments,
                             int attemptCount, boolean contactAllowed, boolean activePaymentLink, String recoveryStatus) {
        static OllamaCaseContext from(RecoveryCase c) { return new OllamaCaseContext(c.caseReference(), c.riskType().name(), c.amountAtRisk(), c.currency(), c.paymentMethod(), c.failureReason(), c.transactionStatus().name(), c.previousSuccessfulPayments(), c.previousFailedPayments(), c.attemptCount(), c.contactAllowed(), c.activePaymentLink(), c.status().name()); }
    }

    record OllamaResponse(String diagnosis, Double recoverabilityScore, String recommendedAction, Integer delayMinutes, List<String> reasons) {}
}
