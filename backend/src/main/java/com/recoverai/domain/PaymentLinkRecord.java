package com.recoverai.domain;

import java.time.Instant;

/** Maps an external Razorpay Payment Link to one recovery case. */
public record PaymentLinkRecord(String razorpayPaymentLinkId, String recoveryCaseId, String referenceId,
                                String status, Instant createdAt, Instant paidAt) {}
