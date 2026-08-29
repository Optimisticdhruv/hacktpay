package com.recoverai.recovery;

import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Applies the non-negotiable configurable business limits to every strategy source. */
@Service
public class ThresholdStrategyEngine {
    private final RecoveryProperties properties;

    public ThresholdStrategyEngine(RecoveryProperties properties) {
        this.properties = properties;
    }

    public StrategyDecision qualify(RecoveryCase caseData, StrategyDecision candidate) {
        if (caseData.paymentCaptured()) return override(candidate, "PAYMENT_ALREADY_CAPTURED", 0, RecoveryAction.NO_ACTION, "A successful payment already exists");
        if (!caseData.contactAllowed()) return override(candidate, "CONTACT_REQUIRES_HUMAN_REVIEW", .15, RecoveryAction.ESCALATE_TO_HUMAN, "Customer contact is not permitted");
        if (caseData.amountAtRisk() > properties.thresholds().maximumRecoveryAmountPaise()) return override(candidate, "AMOUNT_REQUIRES_HUMAN_REVIEW", .10, RecoveryAction.ESCALATE_TO_HUMAN, "Amount exceeds the configured autonomous recovery threshold");
        if (caseData.attemptCount() >= properties.recovery().maxAttempts()) return override(candidate, "MAX_ATTEMPTS_REACHED", .10, RecoveryAction.ESCALATE_TO_HUMAN, "The maximum recovery attempts has been reached");

        if (caseData.riskType() == RiskType.PAYMENT_FAILURE && candidate.recommendedAction() == RecoveryAction.CREATE_PAYMENT_LINK) {
            boolean enoughHistory = caseData.previousSuccessfulPayments() >= properties.thresholds().minimumSuccessfulPaymentsForPaymentLink();
            boolean enoughScore = candidate.recoverabilityScore() >= properties.thresholds().minimumPaymentFailureScore();
            if (!enoughHistory || !enoughScore) return override(candidate, "PAYMENT_FAILURE_NEEDS_REVIEW", candidate.recoverabilityScore(), RecoveryAction.WAIT_AND_RETRY, "Payment score or successful-payment history is below the autonomous-link threshold");
        }
        if (caseData.riskType() == RiskType.CHECKOUT_ABANDONMENT && candidate.recommendedAction() != RecoveryAction.NO_ACTION
                && candidate.recommendedAction() != RecoveryAction.ESCALATE_TO_HUMAN && candidate.recommendedAction() != RecoveryAction.WAIT_AND_RETRY) {
            return override(candidate, "CHECKOUT_ABANDONMENT", candidate.recoverabilityScore(), RecoveryAction.WAIT_AND_RETRY, "Checkout abandonment is limited to a wait-and-retry recommendation");
        }
        if (caseData.riskType() == RiskType.OVERDUE_RECEIVABLE && candidate.recommendedAction() != RecoveryAction.NO_ACTION
                && candidate.recommendedAction() != RecoveryAction.ESCALATE_TO_HUMAN && candidate.recommendedAction() != RecoveryAction.WAIT_AND_RETRY) {
            if (candidate.recoverabilityScore() < properties.thresholds().minimumOverdueScore()) {
                return override(candidate, candidate.diagnosis() + "_LOW_CONFIDENCE", candidate.recoverabilityScore(), RecoveryAction.WAIT_AND_RETRY, "Recoverability score is below the configured threshold");
            }
            return override(candidate, candidate.diagnosis(), candidate.recoverabilityScore(), RecoveryAction.SEND_REMINDER, "Overdue receivables are limited to a policy-qualified reminder");
        }
        return candidate;
    }

    private StrategyDecision override(StrategyDecision candidate, String diagnosis, double score, RecoveryAction action, String reason) {
        List<String> reasons = new ArrayList<>(candidate.reasons());
        reasons.add(reason);
        reasons.add("Deterministic threshold engine qualified this recommendation");
        return new StrategyDecision(diagnosis, score, action, 0, List.copyOf(reasons), candidate.source());
    }
}
