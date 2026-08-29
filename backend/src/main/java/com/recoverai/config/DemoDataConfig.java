package com.recoverai.config;

import com.recoverai.domain.*;
import com.recoverai.repository.AuditRepository;
import com.recoverai.repository.RecoveryCaseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
public class DemoDataConfig {
    @Bean CommandLineRunner seedDemoData(RecoveryCaseRepository cases, AuditRepository audits) { return args -> {
        if (!cases.findAll().isEmpty()) return; Instant now = Instant.now();
        RecoveryCase primary = new RecoveryCase("demo-payment-failure", "RCV-1048", "Aarav Shah", "aarav@example.test", true, RiskType.PAYMENT_FAILURE, 499900, "INR", "UPI", "payment_timeout", TransactionStatus.FAILED, 7, 0, 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), 0, now, now, null);
        RecoveryCase abandoned = new RecoveryCase("demo-abandonment", "RCV-1049", "Meera Iyer", "meera@example.test", true, RiskType.CHECKOUT_ABANDONMENT, 249900, "INR", "CARD", "checkout_expired", TransactionStatus.CREATED, 2, 0, 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), 0, now, now, null);
        RecoveryCase restricted = new RecoveryCase("demo-contact-restricted", "RCV-1050", "Rohan Das", "rohan@example.test", false, RiskType.OVERDUE_RECEIVABLE, 1800000, "INR", "BANK_TRANSFER", "invoice_overdue", TransactionStatus.FAILED, 4, 1, 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), 0, now, now, null);
        for (RecoveryCase c : List.of(primary, abandoned, restricted)) { cases.save(c); audits.save(new AuditEvent("seed-" + c.id(), c.id(), "SYSTEM", "REVENUE_RISK_DETECTED", "Synthetic revenue risk case seeded", Map.of("amountAtRisk", c.amountAtRisk()), now)); }
    }; }
}
