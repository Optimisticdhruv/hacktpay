package com.recoverai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.recoverai.service.RecoveryService;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"recoverai.storage-mode=memory", "recoverai.razorpay.webhook-secret=test-webhook-secret", "recoverai.detection.razorpay-failure-enabled=true"})
class RazorpayWebhookControllerTest {
    @Autowired MockMvc mvc;
    @Autowired RecoveryService recoveries;
    @Test void acceptsValidWebhookSignature() throws Exception { send("evt-valid-signature", "payment.captured").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted")); }
    @Test void acceptsValidPaymentCaptured() throws Exception { send("evt-captured", "payment.captured").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted")).andExpect(jsonPath("$.eventType").value("payment.captured")); }
    @Test void acceptsPaymentFailed() throws Exception { send("evt-failed", "payment.failed").andExpect(status().isOk()).andExpect(jsonPath("$.eventType").value("payment.failed")); }
    @Test void createsHumanReviewCaseForAnySignedUnlinkedExternalFailure() throws Exception {
        String body = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_external_1\",\"amount\":499900,\"currency\":\"INR\",\"method\":\"upi\",\"error_code\":\"PAYMENT_TIMEOUT\"}}}}";
        mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", "evt-external-failure").header("X-Razorpay-Signature", signature(body)).contentType("application/json").content(body)).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertTrue(recoveries.list().stream().anyMatch(c -> c.id().equals("razorpay-failure-pay_external_1") && !c.contactAllowed()));
    }
    @Test void doesNotCreateSecondCaseForFailureLinkedToRecoverAiWorkflow() throws Exception {
        String body = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_linked_1\",\"amount\":499900,\"notes\":{\"recoveryCaseId\":\"existing-case\"}}}}}";
        mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", "evt-linked-failure").header("X-Razorpay-Signature", signature(body)).contentType("application/json").content(body)).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertFalse(recoveries.list().stream().anyMatch(c -> c.id().equals("razorpay-failure-pay_linked_1")));
    }
    @Test void merchantReviewCanExplicitlyEnableContactOnDetectedFailure() throws Exception {
        String failure = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_review_1\",\"amount\":499900}}}}";
        mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", "evt-review-failure").header("X-Razorpay-Signature", signature(failure)).contentType("application/json").content(failure)).andExpect(status().isOk());
        mvc.perform(post("/api/recovery-cases/razorpay-failure-pay_review_1/review").contentType("application/json").content("{\"customerName\":\"Approved test customer\",\"customerEmail\":\"customer@example.test\",\"contactAllowed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contactAllowed").value(true)).andExpect(jsonPath("$.customerName").value("Approved test customer"));
    }
    @Test void acceptsOrderPaid() throws Exception { send("evt-order", "order.paid").andExpect(status().isOk()).andExpect(jsonPath("$.eventType").value("order.paid")); }
    @Test void acceptsPaymentLinkPaid() throws Exception { send("evt-payment-link-paid", "payment_link.paid").andExpect(status().isOk()).andExpect(jsonPath("$.eventType").value("payment_link.paid")); }
    @Test void acceptsUnknownEventWithoutProcessingIt() throws Exception { send("evt-unknown", "subscription.charged").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ignored")); }
    @Test void rejectsInvalidSignature() throws Exception { mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", "evt-invalid").header("X-Razorpay-Signature", "00").contentType("application/json").content("{\"event\":\"payment.captured\"}")).andExpect(status().isUnauthorized()); }
    @Test void rejectsMissingSignature() throws Exception { mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", "evt-missing").contentType("application/json").content("{\"event\":\"payment.captured\"}")).andExpect(status().isUnauthorized()); }
    @Test void handlesDuplicateEventIdIdempotently() throws Exception { send("evt-duplicate", "payment.captured").andExpect(status().isOk()); send("evt-duplicate", "payment.captured").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("duplicate")); }
    private org.springframework.test.web.servlet.ResultActions send(String eventId, String event) throws Exception { String body = "{\"event\":\"" + event + "\",\"payload\":{}}"; return mvc.perform(post("/api/webhooks/razorpay").header("x-razorpay-event-id", eventId).header("X-Razorpay-Signature", signature(body)).contentType("application/json").content(body)); }
    private String signature(String body) throws Exception { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec("test-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256")); return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8))); }
}
