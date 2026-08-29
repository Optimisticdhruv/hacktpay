package com.recoverai.razorpay;
public record PaymentLinkResult(String externalId, String shortUrl, String referenceId, String status, long amountPaid, boolean demoMode) {}
