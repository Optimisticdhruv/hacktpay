package com.recoverai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"recoverai.storage-mode=memory", "recoverai.razorpay.webhook-secret=test-webhook-secret"})
class RazorpayWebhookControllerTest {
    @Autowired MockMvc mvc;
    @Test void acceptsValidWebhookSignature() throws Exception { send("evt-valid-signature", "payment.captured").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted")); }
    @Test void acceptsValidPaymentCaptured() throws Exception { send("evt-captured", "payment.captured").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted")).andExpect(jsonPath("$.eventType").value("payment.captured")); }
    @Test void acceptsPaymentFailed() throws Exception { send("evt-failed", "payment.failed").andExpect(status().isOk()).andExpect(jsonPath("$.eventType").value("payment.failed")); }
    @Test void acceptsOrderPaid() throws Exception { send("evt-order", "order.paid").andExpect(status().isOk()).andExpect(jsonPath("$.eventType").value("order.paid")); }
    @Test void acceptsPaymentLinkPaid() throws Exception { send("evt-payment-link-paid", "payment_link.paid").andExpect(status().isOk()).andExpect(jsonPath("$.eventType").value("payment_link.paid")); }
    @Test void acceptsUnknownEventWithoutProcessingIt() throws Exception { send("evt-unknown", "subscription.charged").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ignored")); }
    @Test void rejectsInvalidSignature() throws Exception { mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", "evt-invalid").header("X-Razorpay-Signature", "00").contentType("application/json").content("{\"event\":\"payment.captured\"}")).andExpect(status().isUnauthorized()); }
    @Test void rejectsMissingSignature() throws Exception { mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", "evt-missing").contentType("application/json").content("{\"event\":\"payment.captured\"}")).andExpect(status().isUnauthorized()); }
    @Test void handlesDuplicateEventIdIdempotently() throws Exception { send("evt-duplicate", "payment.captured").andExpect(status().isOk()); send("evt-duplicate", "payment.captured").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("duplicate")); }
    private org.springframework.test.web.servlet.ResultActions send(String eventId, String event) throws Exception { String body = "{\"event\":\"" + event + "\",\"payload\":{}}"; return mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", eventId).header("X-Razorpay-Signature", signature(body)).contentType("application/json").content(body)); }
    private String signature(String body) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec("test-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8))); }
}
