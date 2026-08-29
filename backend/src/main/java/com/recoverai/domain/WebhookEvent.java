package com.recoverai.domain;

import java.time.Instant;

/** Immutable receipt of a Razorpay delivery; payloads are deliberately not retained. */
public record WebhookEvent(String eventId, String eventType, String payloadHash, String processingStatus,
                           Instant receivedAt, Instant processedAt) {}
