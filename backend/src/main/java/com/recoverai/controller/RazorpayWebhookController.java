package com.recoverai.controller;

import com.recoverai.webhook.RazorpayWebhookService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class RazorpayWebhookController {
    private final RazorpayWebhookService webhooks;
    public RazorpayWebhookController(RazorpayWebhookService webhooks) { this.webhooks = webhooks; }
    @PostMapping(value = "/razorpay", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RazorpayWebhookService.WebhookResult receive(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                                        @RequestHeader(value = "x-razorpay-event-id", required = false) String eventId,
                                                        @RequestBody byte[] payload) {
        return webhooks.accept(eventId, signature, payload);
    }
}
