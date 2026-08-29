package com.recoverai.razorpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.RecoveryCase;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class RazorpayPaymentLinkClient implements PaymentLinkClient {
    private final RecoveryProperties properties; private final ObjectMapper json; private final HttpClient http = HttpClient.newHttpClient();
    public RazorpayPaymentLinkClient(RecoveryProperties properties, ObjectMapper json) { this.properties = properties; this.json = json; }
    public PaymentLinkResult create(RecoveryCase c, String referenceId) {
        if (!properties.razorpay().isConfigured()) throw new IllegalStateException("Razorpay Test Mode credentials are not configured. No recovery action was sent.");
        try {
            Map<String, Object> body = Map.of("amount", c.amountAtRisk(), "currency", c.currency(), "reference_id", referenceId,
                    "description", "RecoverAI recovery for " + c.caseReference(), "customer", Map.of("name", c.customerName(), "email", c.customerEmail() == null ? "" : c.customerEmail()), "notes", Map.of("recoveryCaseId", c.id(), "caseReference", c.caseReference()),
                    "expire_by", Instant.now().plusSeconds(properties.recovery().paymentLinkExpiryHours() * 3600L).getEpochSecond(), "reminder_enable", false,
                    "callback_url", properties.frontendUrl() + "/recovery/success", "callback_method", "get");
            String auth = Base64.getEncoder().encodeToString((properties.razorpay().keyId() + ":" + properties.razorpay().keySecret()).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/payment_links")).header("Authorization", "Basic " + auth).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Razorpay rejected payment-link creation (HTTP " + response.statusCode() + ")");
            return parse(json.readTree(response.body()));
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Razorpay request interrupted", e); }
        catch (Exception e) { if (e instanceof IllegalStateException state) throw state; throw new IllegalStateException("Razorpay payment-link request failed", e); }
    }
    public Optional<PaymentLinkResult> findByReferenceId(String referenceId) {
        if (!properties.razorpay().isConfigured()) throw new IllegalStateException("Razorpay Test Mode credentials are not configured. No recovery action was sent.");
        try { String auth=Base64.getEncoder().encodeToString((properties.razorpay().keyId()+":"+properties.razorpay().keySecret()).getBytes(StandardCharsets.UTF_8)); String query=URLEncoder.encode(referenceId, StandardCharsets.UTF_8); HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/payment_links/?reference_id="+query)).header("Authorization","Basic "+auth).GET().build(); HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString()); if(response.statusCode()==401||response.statusCode()==403) throw new IllegalStateException("Razorpay authentication failed"); if(response.statusCode()==429) throw new IllegalStateException("Razorpay rate limit reached"); if(response.statusCode()<200||response.statusCode()>=300) throw new IllegalStateException("Razorpay reconciliation failed (HTTP "+response.statusCode()+")"); JsonNode links=json.readTree(response.body()).path("payment_links"); return links.isArray()&& !links.isEmpty()?Optional.of(parse(links.get(0))):Optional.empty(); } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Razorpay reconciliation interrupted",e);} catch(Exception e){if(e instanceof IllegalStateException state)throw state;throw new IllegalStateException("Razorpay reconciliation failed",e);}
    }
    private PaymentLinkResult parse(JsonNode node) { return new PaymentLinkResult(node.path("id").asText(),node.path("short_url").asText(),node.path("reference_id").asText(),node.path("status").asText(),node.path("amount_paid").asLong(),false); }
}
