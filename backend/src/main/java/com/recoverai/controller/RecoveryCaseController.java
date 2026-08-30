package com.recoverai.controller;

import com.recoverai.domain.*;
import com.recoverai.service.RecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/recovery-cases")
@CrossOrigin(origins = "${recoverai.frontend-url}")
public class RecoveryCaseController {
    private final RecoveryService service;
    public RecoveryCaseController(RecoveryService service) { this.service = service; }
    @GetMapping public List<RecoveryCase> list() { return service.list(); }
    @GetMapping("/{id}") public RecoveryCase get(@PathVariable String id) { return service.get(id); }
    @GetMapping("/{id}/audit") public List<AuditEvent> audit(@PathVariable String id) { return service.audit(id); }
    @GetMapping("/{id}/tasks") public List<RecoveryTask> tasks(@PathVariable String id) { return service.tasks(id); }
    @PostMapping("/{id}/review") public RecoveryCase review(@PathVariable String id, @RequestBody @Valid ReviewRecoveryCase request) { return service.reviewDetectedCase(id, request.customerName(), request.customerEmail(), request.contactAllowed()); }
    @PostMapping("/{id}/analyze") public RecoveryCase analyze(@PathVariable String id) { return service.analyze(id); }
    @PostMapping("/{id}/execute") public RecoveryService.ExecutionResult execute(@PathVariable String id) { return service.execute(id); }
    @PostMapping("/{id}/stop") public RecoveryCase stop(@PathVariable String id) { return service.stop(id); }
    @PostMapping("/{id}/tasks/{taskId}/complete") public RecoveryTask completeTask(@PathVariable String id, @PathVariable String taskId) { return service.completeTask(id, taskId); }
    @PostMapping("/{id}/handoff-outcome") public RecoveryCase handoffOutcome(@PathVariable String id, @RequestBody @Valid HandoffOutcomeRequest request) { return service.resolveHumanHandoff(id, request.outcome(), request.amountRecovered() == null ? 0 : request.amountRecovered()); }
    @PostMapping("/stop-open") public StopOpenCasesResponse stopOpenCases() { return new StopOpenCasesResponse(service.stopOpenCases()); }
    @PostMapping("/demo-pack") public List<RecoveryCase> demoPack() {
        Instant now = Instant.now();
        List<RecoveryCase> pack = List.of(
                demoCase("Demo checkout", RiskType.CHECKOUT_ABANDONMENT, 249900, "CARD", "checkout_expired", TransactionStatus.CREATED, 2, 0, now),
                demoCase("Demo subscription", RiskType.SUBSCRIPTION_FAILURE, 799900, "CARD", "renewal_payment_failed", TransactionStatus.FAILED, 4, 1, now),
                demoCase("Demo invoice", RiskType.OVERDUE_RECEIVABLE, 1200000, "BANK_TRANSFER", "invoice_overdue", TransactionStatus.FAILED, 5, 0, now));
        return pack.stream().map(service::create).toList();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public RecoveryCase create(@RequestBody @Valid CreateRecoveryCase request) {
        Instant now = Instant.now();
        RecoveryCase recoveryCase = new RecoveryCase(UUID.randomUUID().toString(), "RCV-" + (1000 + new Random().nextInt(9000)), request.customerName(), request.customerEmail(), request.contactAllowed(), request.riskType(), request.amountAtRisk(), "INR", request.paymentMethod(), request.failureReason(), request.transactionStatus(), request.previousSuccessfulPayments(), request.previousFailedPayments(), 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), null, 0, now, now, null);
        return service.create(recoveryCase);
    }
    public record CreateRecoveryCase(@NotBlank String customerName, String customerEmail, boolean contactAllowed, @NotNull RiskType riskType, long amountAtRisk, String paymentMethod, String failureReason, TransactionStatus transactionStatus, int previousSuccessfulPayments, int previousFailedPayments) {}
    public record ReviewRecoveryCase(@NotBlank String customerName, String customerEmail, boolean contactAllowed) {}
    public record StopOpenCasesResponse(int stoppedCount) {}
    public record HandoffOutcomeRequest(@NotNull HumanHandoffOutcome outcome, Long amountRecovered) {}
    private RecoveryCase demoCase(String customerName, RiskType riskType, long amountAtRisk, String paymentMethod, String failureReason, TransactionStatus transactionStatus, int previousSuccessfulPayments, int previousFailedPayments, Instant now) {
        return new RecoveryCase(UUID.randomUUID().toString(), "DEMO-" + (1000 + new Random().nextInt(9000)), customerName, null, true, riskType, amountAtRisk, "INR", paymentMethod, failureReason, transactionStatus, previousSuccessfulPayments, previousFailedPayments, 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), null, 0, now, now, null);
    }
}
