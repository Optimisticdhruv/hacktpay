package com.recoverai.recovery;

import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FallbackStrategyEngine {
    private final RecoveryProperties properties;
    public FallbackStrategyEngine(RecoveryProperties properties) { this.properties = properties; }
    public StrategyDecision decide(RecoveryCase c) {
        if (c.paymentCaptured()) return decision("PAYMENT_ALREADY_CAPTURED", 0, RecoveryAction.NO_ACTION, "A successful payment already exists");
        if (!c.contactAllowed()) return decision("CONTACT_REQUIRES_HUMAN_REVIEW", .15, RecoveryAction.ESCALATE_TO_HUMAN, "Customer contact is not permitted");
        if (c.amountAtRisk() > properties.thresholds().maximumRecoveryAmountPaise()) return decision("AMOUNT_REQUIRES_HUMAN_REVIEW", .10, RecoveryAction.ESCALATE_TO_HUMAN, "Amount exceeds the configured autonomous recovery threshold");
        if (c.attemptCount() >= properties.recovery().maxAttempts()) return decision("MAX_ATTEMPTS_REACHED", .10, RecoveryAction.ESCALATE_TO_HUMAN, "The maximum recovery attempts has been reached");
        return switch (c.riskType()) {
            case PAYMENT_FAILURE -> paymentFailure(c);
            case CHECKOUT_ABANDONMENT -> thresholdDecision("CHECKOUT_ABANDONMENT", score(c, .56), properties.thresholds().minimumAbandonmentScore(), RecoveryAction.WAIT_AND_RETRY, "Checkout has not yet produced a successful payment");
            case OVERDUE_RECEIVABLE -> thresholdDecision("OVERDUE_RECEIVABLE", score(c, .63), properties.thresholds().minimumOverdueScore(), RecoveryAction.SEND_REMINDER, "Receivable is overdue and contact is permitted");
            case SUBSCRIPTION_FAILURE -> decision("SUBSCRIPTION_RENEWAL_FAILURE", score(c, .58), RecoveryAction.CREATE_PAYMENT_LINK, "Subscription renewal needs a new payment route");
        };
    }
    private StrategyDecision paymentFailure(RecoveryCase c) {
        double score = score(c, .64);
        boolean enoughHistory = c.previousSuccessfulPayments() >= properties.thresholds().minimumSuccessfulPaymentsForPaymentLink();
        if (enoughHistory && score >= properties.thresholds().minimumPaymentFailureScore()) return decision("TRANSIENT_PAYMENT_FAILURE", score, RecoveryAction.CREATE_PAYMENT_LINK, "Payment score and successful-payment history meet the autonomous-link threshold");
        return decision("PAYMENT_FAILURE_NEEDS_REVIEW", score, RecoveryAction.WAIT_AND_RETRY, "Payment score or successful-payment history is below the autonomous-link threshold");
    }
    private StrategyDecision thresholdDecision(String diagnosis, double score, double minimum, RecoveryAction action, String reason) {
        return score >= minimum ? decision(diagnosis, score, action, reason) : decision(diagnosis + "_LOW_CONFIDENCE", score, RecoveryAction.WAIT_AND_RETRY, "Recoverability score is below the configured threshold");
    }
    private double score(RecoveryCase c, double base) { return Math.min(.95, base + Math.min(c.previousSuccessfulPayments(), 5) * .03 - Math.min(c.previousFailedPayments(), 3) * .04); }
    private StrategyDecision decision(String diagnosis, double score, RecoveryAction action, String reason) { return new StrategyDecision(diagnosis, score, action, 0, List.of(reason, "Deterministic fallback strategy used"), true); }
}
