package com.recoverai.recovery;

import com.recoverai.config.RecoveryProperties;
import com.recoverai.domain.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyEngine {
    private final RecoveryProperties properties;
    public PolicyEngine(RecoveryProperties properties) { this.properties = properties; }

    public PolicyResult evaluate(RecoveryCase c, RecoveryAction action) {
        List<PolicyCheck> checks = new ArrayList<>();
        checks.add(new PolicyCheck("PAYMENT_NOT_ALREADY_CAPTURED", !c.paymentCaptured(), "Payment must not already be captured"));
        checks.add(new PolicyCheck("MAX_RECOVERY_ATTEMPTS", c.attemptCount() < properties.recovery().maxAttempts(), "Maximum is " + properties.recovery().maxAttempts()));
        boolean contactRequired = action == RecoveryAction.CREATE_PAYMENT_LINK || action == RecoveryAction.SEND_REMINDER;
        checks.add(new PolicyCheck("CONTACT_ALLOWED", !contactRequired || c.contactAllowed(), "Customer contact permission is required"));
        checks.add(new PolicyCheck("NO_ACTIVE_DUPLICATE_LINK", action != RecoveryAction.CREATE_PAYMENT_LINK || !c.activePaymentLink(), "An equivalent payment link must not be active"));
        checks.add(new PolicyCheck("CASE_IS_ACTIVE", c.status() != RecoveryStatus.STOPPED && c.status() != RecoveryStatus.RECOVERED, "Stopped or recovered cases cannot be acted on"));
        checks.add(new PolicyCheck("VALID_AMOUNT", c.amountAtRisk() > 0, "Amount at risk must be positive"));
        checks.add(new PolicyCheck("SUPPORTED_ACTION", action != null, "Action must be recognised"));
        PolicyCheck failed = checks.stream().filter(check -> !check.passed()).findFirst().orElse(null);
        return new PolicyResult(failed == null, failed == null ? null : failed.name(), checks);
    }
}
