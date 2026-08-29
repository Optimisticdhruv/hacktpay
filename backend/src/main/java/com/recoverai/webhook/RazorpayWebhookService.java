package com.recoverai.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.WebhookEvent;
import com.recoverai.repository.WebhookEventRepository;
import com.recoverai.service.RecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

@Service
public class RazorpayWebhookService {
    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookService.class);
    private static final Set<String> HANDLED_EVENTS = Set.of("payment.captured", "payment.failed", "order.paid", "payment_link.paid");
    private final RecoveryProperties properties; private final WebhookEventRepository events; private final ObjectMapper json; private final RecoveryService recoveries;
    public RazorpayWebhookService(RecoveryProperties properties, WebhookEventRepository events, ObjectMapper json, RecoveryService recoveries) { this.properties = properties; this.events = events; this.json = json; this.recoveries = recoveries; }
    public WebhookResult accept(String eventId, String signature, byte[] payload) {
        if (eventId == null || eventId.isBlank()) throw new WebhookBadRequestException("Missing x-razorpay-event-id header");
        if (signature == null || signature.isBlank()) throw new WebhookUnauthorizedException("Missing X-Razorpay-Signature header");
        if (!isSignatureValid(signature, payload)) throw new WebhookUnauthorizedException("Invalid Razorpay webhook signature");
        if (events.findByEventId(eventId).isPresent()) { log.info("Ignored duplicate Razorpay webhook eventId={}", eventId); return new WebhookResult("duplicate", null); }
        try {
            JsonNode root = json.readTree(payload); String eventType = root.path("event").asText();
            if (eventType.isBlank()) throw new WebhookBadRequestException("Webhook payload has no event type");
            String status = HANDLED_EVENTS.contains(eventType) ? "ACCEPTED" : "IGNORED_UNKNOWN_EVENT";
            events.save(new WebhookEvent(eventId, eventType, sha256(payload), status, Instant.now(), Instant.now()));
            String linkId = firstText(root, "payload.payment_link.entity.id", "payload.payment.entity.notes.razorpayPaymentLinkId", "payload.payment.entity.notes.payment_link_id");
            String recoveryCaseId = firstText(root, "payload.payment_link.entity.notes.recoveryCaseId", "payload.payment.entity.notes.recoveryCaseId");
            String paymentId = firstText(root, "payload.payment.entity.id", "payload.payment_link.entity.payment_id");
            if ("payment.captured".equals(eventType) || "order.paid".equals(eventType) || "payment_link.paid".equals(eventType)) {
                if (linkId != null) recoveries.markPaymentCaptured(linkId, paymentId);
                else recoveries.markPaymentCapturedForCase(recoveryCaseId, paymentId);
            }
            if ("payment.failed".equals(eventType)) recoveries.recordPaymentFailure(recoveryCaseId);
            log.info("Accepted Razorpay webhook eventId={}, eventType={}, status={}", eventId, eventType, status);
            return new WebhookResult(status.equals("ACCEPTED") ? "accepted" : "ignored", eventType);
        } catch (WebhookBadRequestException e) { throw e; }
        catch (Exception e) { throw new WebhookBadRequestException("Invalid JSON webhook payload"); }
    }
    private String firstText(JsonNode root, String... paths) { for (String path : paths) { JsonNode node=root; for (String part : path.split("\\.")) node=node.path(part); if (!node.isMissingNode() && !node.asText().isBlank()) return node.asText(); } return null; }
    private boolean isSignatureValid(String signature, byte[] payload) {
        String secret = properties.razorpay().webhookSecret();
        if (secret == null || secret.isBlank()) throw new WebhookUnavailableException("Razorpay webhook secret is not configured");
        try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")); byte[] expected = mac.doFinal(payload); byte[] supplied = HexFormat.of().parseHex(signature); return MessageDigest.isEqual(expected, supplied); }
        catch (IllegalArgumentException e) { return false; }
        catch (Exception e) { throw new WebhookUnavailableException("Webhook signature verification is unavailable"); }
    }
    private String sha256(byte[] payload) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload)); } catch (Exception e) { throw new IllegalStateException(e); } }
    public record WebhookResult(String status, String eventType) {}
}
