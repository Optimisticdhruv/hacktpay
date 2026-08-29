package com.recoverai.domain;

import java.time.Instant;
import java.util.List;

public record RecoveryCase(
        String id, String caseReference, String customerName, String customerEmail, boolean contactAllowed,
        RiskType riskType, long amountAtRisk, String currency, String paymentMethod, String failureReason,
        TransactionStatus transactionStatus, int previousSuccessfulPayments, int previousFailedPayments,
        int attemptCount, boolean activePaymentLink, RecoveryStatus status, String diagnosis,
        Double recoverabilityScore, RecoveryAction recommendedAction, List<String> reasons,
        long amountRecovered, Instant createdAt, Instant updatedAt, Instant resolvedAt) {
    public boolean paymentCaptured() { return transactionStatus == TransactionStatus.CAPTURED; }
}
