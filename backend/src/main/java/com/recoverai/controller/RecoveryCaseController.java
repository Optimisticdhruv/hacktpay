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
    @PostMapping("/{id}/analyze") public RecoveryCase analyze(@PathVariable String id) { return service.analyze(id); }
    @PostMapping("/{id}/execute") public RecoveryService.ExecutionResult execute(@PathVariable String id) { return service.execute(id); }
    @PostMapping("/{id}/stop") public RecoveryCase stop(@PathVariable String id) { return service.stop(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public RecoveryCase create(@RequestBody @Valid CreateRecoveryCase request) {
        Instant now = Instant.now();
        RecoveryCase recoveryCase = new RecoveryCase(UUID.randomUUID().toString(), "RCV-" + (1000 + new Random().nextInt(9000)), request.customerName(), request.customerEmail(), request.contactAllowed(), request.riskType(), request.amountAtRisk(), "INR", request.paymentMethod(), request.failureReason(), request.transactionStatus(), request.previousSuccessfulPayments(), request.previousFailedPayments(), 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), null, 0, now, now, null);
        return service.create(recoveryCase);
    }
    public record CreateRecoveryCase(@NotBlank String customerName, String customerEmail, boolean contactAllowed, @NotNull RiskType riskType, long amountAtRisk, String paymentMethod, String failureReason, TransactionStatus transactionStatus, int previousSuccessfulPayments, int previousFailedPayments) {}
}
