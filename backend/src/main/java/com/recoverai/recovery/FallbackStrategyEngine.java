package com.recoverai.recovery;

import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FallbackStrategyEngine {
    private final RecoveryProperties properties;
    private final ThresholdStrategyEngine thresholds;
    public FallbackStrategyEngine(RecoveryProperties properties, ThresholdStrategyEngine thresholds) { this.properties = properties; this.thresholds = thresholds; }
    public StrategyDecision decide(RecoveryCase c) {
        StrategyDecision candidate = switch (c.riskType()) {
            case PAYMENT_FAILURE -> paymentFailure(c);
            case CHECKOUT_ABANDONMENT -> thresholdDecision("CHECKOUT_ABANDONMENT", score(c, .56), properties.thresholds().minimumAbandonmentScore(), RecoveryAction.WAIT_AND_RETRY, "Checkout has not yet produced a successful payment");
            case OVERDUE_RECEIVABLE -> thresholdDecision("OVERDUE_RECEIVABLE", score(c, .63), properties.thresholds().minimumOverdueScore(), RecoveryAction.SEND_REMINDER, "Receivable is overdue and contact is permitted");
            case SUBSCRIPTION_FAILURE -> decision("SUBSCRIPTION_RENEWAL_FAILURE", score(c, .58), RecoveryAction.CREATE_PAYMENT_LINK, "Subscription renewal needs a new payment route");
        };
        return thresholds.qualify(c, candidate);
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
    private StrategyDecision decision(String diagnosis, double score, RecoveryAction action, String reason) { return new StrategyDecision(diagnosis, score, action, 0, List.of(reason, "Deterministic fallback strategy used"), StrategySource.DETERMINISTIC_FALLBACK); }
}
