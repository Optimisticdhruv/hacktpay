package com.recoverai.recovery;

import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FallbackStrategyEngineTest {
    private final FallbackStrategyEngine rules = new FallbackStrategyEngine(new RecoveryProperties("memory", false, null, null,
            new RecoveryProperties.Recovery(3, 24, false), new RecoveryProperties.Thresholds(1, .70, .60, .60, 5_000_000), "http://localhost:5173"));
    @Test void createsPaymentLinkOnlyWhenHistoryAndScoreMeetThreshold() { assertEquals(RecoveryAction.CREATE_PAYMENT_LINK, rules.decide(caseOf(7, 0, 499900)).recommendedAction()); }
    @Test void escalatesCasesAboveAutonomousAmountThreshold() { assertEquals(RecoveryAction.ESCALATE_TO_HUMAN, rules.decide(caseOf(7, 0, 5_000_001)).recommendedAction()); }
    private RecoveryCase caseOf(int successes, int failures, long amount) { Instant now=Instant.now(); return new RecoveryCase("case", "RCV-1", "Demo", null, true, RiskType.PAYMENT_FAILURE, amount, "INR", "UPI", "timeout", TransactionStatus.FAILED, successes, failures, 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), 0, now, now, null); }
}
