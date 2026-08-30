package com.recoverai.controller;

import com.recoverai.domain.RecoveryCase;
import com.recoverai.domain.RecoveryStatus;
import com.recoverai.service.RecoveryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "${recoverai.frontend-url}")
public class DashboardController {
    private final RecoveryService service;
    public DashboardController(RecoveryService service) { this.service = service; }
    @GetMapping("/summary") public Summary summary() {
        List<RecoveryCase> cases = service.list();
        List<RecoveryCase> tracked = cases.stream().filter(c -> c.status() != RecoveryStatus.STOPPED && c.status() != RecoveryStatus.UNRECOVERABLE).toList();
        long risk = tracked.stream().filter(c -> c.status() != RecoveryStatus.RECOVERED).mapToLong(RecoveryCase::amountAtRisk).sum(); long recovered = tracked.stream().mapToLong(RecoveryCase::amountRecovered).sum(); long trackedValue = risk + recovered; long attempted = tracked.stream().filter(c -> c.attemptCount() > 0).mapToLong(RecoveryCase::amountAtRisk).sum(); long recoveredCases = cases.stream().filter(c -> c.status() == RecoveryStatus.RECOVERED).count();
        long openCases = cases.stream().filter(c -> c.status() != RecoveryStatus.RECOVERED && c.status() != RecoveryStatus.STOPPED && c.status() != RecoveryStatus.UNRECOVERABLE).count();
        return new Summary(risk, attempted, recovered, trackedValue == 0 ? 0 : Math.round((recovered * 10000.0 / trackedValue)) / 100.0, openCases, recoveredCases, cases.stream().filter(c -> c.status() == RecoveryStatus.ESCALATED).count(), 0);
    }
    public record Summary(long totalRevenueAtRisk, long recoveryAttempted, long revenueRecovered, double recoveryRate, long activeCases, long recoveredCases, long escalatedCases, long policyViolations) {}
}
