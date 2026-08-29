package com.recoverai.evaluation;

import com.recoverai.domain.*;
import com.recoverai.recovery.FallbackStrategyEngine;
import com.recoverai.recovery.PolicyEngine;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Deterministic synthetic evaluation. It never creates Razorpay links or changes
 * merchant data; its results must always be presented as simulated outcomes.
 */
@Service
public class EvaluationService {
    private final FallbackStrategyEngine strategy;
    private final PolicyEngine policy;

    public EvaluationService(FallbackStrategyEngine strategy, PolicyEngine policy) {
        this.strategy = strategy;
        this.policy = policy;
    }

    public EvaluationResult run(int requestedSize, long seed) {
        int size = Math.max(20, Math.min(requestedSize, 1_000));
        Random random = new Random(seed);
        long atRisk = 0, attempted = 0, recovered = 0;
        int approved = 0, blocked = 0;
        Map<RiskType, StrategyMetric> metrics = new EnumMap<>(RiskType.class);
        for (RiskType type : RiskType.values()) metrics.put(type, new StrategyMetric(0, 0, 0));

        for (int i = 0; i < size; i++) {
            RiskType type = RiskType.values()[i % RiskType.values().length];
            long amount = (500 + random.nextInt(19_501)) * 100L;
            boolean contactAllowed = random.nextDouble() > 0.12;
            int previousSuccesses = random.nextInt(9);
            int previousFailures = random.nextInt(3);
            RecoveryCase synthetic = new RecoveryCase("simulation-" + i, "SIM-" + (1000 + i), "Synthetic customer", null,
                    contactAllowed, type, amount, "INR", "SIMULATED", "synthetic_event", TransactionStatus.FAILED,
                    previousSuccesses, previousFailures, 0, false, RecoveryStatus.DETECTED, null, null, null,
                    List.of(), 0, Instant.EPOCH, Instant.EPOCH, null);
            StrategyDecision decision = strategy.decide(synthetic);
            PolicyResult gate = policy.evaluate(synthetic, decision.recommendedAction());
            StrategyMetric current = metrics.get(type);
            atRisk += amount;
            if (!gate.approved()) {
                blocked++;
                metrics.put(type, current.add(amount, 0, 0));
                continue;
            }
            approved++;
            boolean actionable = decision.recommendedAction() == RecoveryAction.CREATE_PAYMENT_LINK
                    || decision.recommendedAction() == RecoveryAction.SEND_REMINDER;
            long recoveredForCase = actionable && random.nextDouble() < decision.recoverabilityScore() * 0.62 ? amount : 0;
            if (actionable) attempted += amount;
            recovered += recoveredForCase;
            metrics.put(type, current.add(amount, actionable ? amount : 0, recoveredForCase));
        }
        return new EvaluationResult("SIMULATED", size, seed, atRisk, attempted, recovered,
                attempted == 0 ? 0 : ((double) recovered / attempted), approved, blocked, metrics);
    }

    public record EvaluationResult(String dataClassification, int datasetSize, long seed, long totalAtRisk,
                                   long totalAttempted, long totalRecovered, double recoveryRate,
                                   int policyApproved, int policyBlocked, Map<RiskType, StrategyMetric> byRiskType) {}
    public record StrategyMetric(long atRisk, long attempted, long recovered) {
        StrategyMetric add(long newAtRisk, long newAttempted, long newRecovered) {
            return new StrategyMetric(atRisk + newAtRisk, attempted + newAttempted, recovered + newRecovered);
        }
    }
}
