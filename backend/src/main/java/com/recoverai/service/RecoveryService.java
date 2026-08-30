package com.recoverai.service;

import com.recoverai.domain.*;
import com.recoverai.config.RecoveryProperties;
import com.recoverai.razorpay.PaymentLinkClient;
import com.recoverai.razorpay.PaymentLinkResult;
import com.recoverai.recovery.RecoveryStrategyService;
import com.recoverai.recovery.PolicyEngine;
import com.recoverai.repository.AuditRepository;
import com.recoverai.repository.RecoveryCaseRepository;
import com.recoverai.repository.PaymentLinkRepository;
import com.recoverai.repository.RecoveryTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class RecoveryService {
    private final RecoveryCaseRepository cases; private final AuditRepository audits; private final RecoveryStrategyService strategy; private final PolicyEngine policy; private final PaymentLinkClient paymentLinks; private final PaymentLinkRepository paymentLinkRecords; private final RecoveryTaskRepository tasks; private final RecoveryProperties properties;
    public RecoveryService(RecoveryCaseRepository cases, AuditRepository audits, RecoveryStrategyService strategy, PolicyEngine policy, PaymentLinkClient paymentLinks, PaymentLinkRepository paymentLinkRecords, RecoveryTaskRepository tasks, RecoveryProperties properties) { this.cases = cases; this.audits = audits; this.strategy = strategy; this.policy = policy; this.paymentLinks = paymentLinks; this.paymentLinkRecords = paymentLinkRecords; this.tasks = tasks; this.properties = properties; }
    public List<RecoveryCase> list() { return cases.findAll(); }
    public RecoveryCase create(RecoveryCase recoveryCase) {
        RecoveryCase saved = cases.save(recoveryCase);
        log(saved.id(), "SYSTEM", "REVENUE_RISK_DETECTED", "Revenue risk case created", Map.of("amountAtRisk", saved.amountAtRisk(), "riskType", saved.riskType().name()));
        return saved;
    }
    @Transactional public void detectExternalPaymentFailure(String razorpayPaymentId, long amountPaise, String currency, String paymentMethod, String failureReason, boolean contactAllowed) {
        if (razorpayPaymentId == null || razorpayPaymentId.isBlank() || cases.findById("razorpay-failure-" + razorpayPaymentId).isPresent()) return;
        Instant now = Instant.now();
        RecoveryCase detected = new RecoveryCase("razorpay-failure-" + razorpayPaymentId, "RZP-" + razorpayPaymentId.substring(Math.max(0, razorpayPaymentId.length() - 8)).toUpperCase(), "Razorpay detected payment", null, contactAllowed, RiskType.PAYMENT_FAILURE, Math.max(0, amountPaise), currency == null || currency.isBlank() ? "INR" : currency, paymentMethod == null || paymentMethod.isBlank() ? "UNKNOWN" : paymentMethod, failureReason == null || failureReason.isBlank() ? "razorpay_payment_failed" : failureReason, TransactionStatus.FAILED, 0, 1, 0, false, RecoveryStatus.DETECTED, null, null, null, List.of(), null, 0, now, now, null);
        cases.save(detected);
        log(detected.id(), "RAZORPAY", "RAZORPAY_FAILURE_DETECTED", "Signed Razorpay failure event created a human-review recovery case", Map.of("paymentId", razorpayPaymentId, "amountAtRisk", amountPaise, "contactAllowed", contactAllowed));
    }
    public RecoveryCase get(String id) { return cases.findById(id).orElseThrow(() -> new NoSuchElementException("Recovery case not found: " + id)); }
    public List<AuditEvent> audit(String id) { get(id); return audits.findByRecoveryCaseId(id); }
    public List<RecoveryTask> tasks(String id) { get(id); return tasks.findByRecoveryCaseId(id); }
    @Transactional public RecoveryCase reviewDetectedCase(String id, String customerName, String customerEmail, boolean contactAllowed) {
        RecoveryCase current = get(id);
        if (current.status() == RecoveryStatus.RECOVERED || current.status() == RecoveryStatus.STOPPED) throw new IllegalStateException("Resolved recovery cases cannot be reviewed");
        RecoveryCase reviewed = new RecoveryCase(current.id(), current.caseReference(), customerName.trim(), customerEmail == null || customerEmail.isBlank() ? null : customerEmail.trim(), contactAllowed, current.riskType(), current.amountAtRisk(), current.currency(), current.paymentMethod(), current.failureReason(), current.transactionStatus(), current.previousSuccessfulPayments(), current.previousFailedPayments(), current.attemptCount(), current.activePaymentLink(), current.status(), current.diagnosis(), current.recoverabilityScore(), current.recommendedAction(), current.reasons(), current.strategySource(), current.amountRecovered(), current.createdAt(), Instant.now(), current.resolvedAt());
        cases.save(reviewed);
        log(id, "MERCHANT", "HUMAN_REVIEW_COMPLETED", contactAllowed ? "Merchant approved recovery contact after review" : "Merchant reviewed case without contact approval", Map.of("contactAllowed", contactAllowed));
        return reviewed;
    }
    @Transactional public RecoveryCase analyze(String id) {
        RecoveryCase current = get(id); log(id, "AI_AGENT", "AI_ANALYSIS_STARTED", "Recovery analysis started", Map.of());
        RecoveryStrategyService.StrategyResolution resolution = strategy.decide(current);
        StrategyDecision d = resolution.decision();
        RecoveryCase updated = copy(current, RecoveryStatus.ACTION_PENDING, d.diagnosis(), d.recoverabilityScore(), d.recommendedAction(), d.reasons(), d.source(), current.attemptCount(), current.activePaymentLink(), current.amountRecovered(), null);
        cases.save(updated);
        if (d.source() == StrategySource.OLLAMA) {
            log(id, "AI_AGENT", "AI_STRATEGY_GENERATED", "Ollama generated a contextual recovery recommendation", Map.of("provider", "OLLAMA", "model", properties.ollama().model(), "diagnosis", d.diagnosis(), "action", d.recommendedAction(), "score", d.recoverabilityScore()));
        } else {
            log(id, "AI_AGENT", "AI_FALLBACK_USED", "Deterministic strategy recommendation created", Map.of("reason", resolution.fallbackReason(), "action", d.recommendedAction(), "score", d.recoverabilityScore()));
        }
        return updated;
    }
    @Transactional public synchronized ExecutionResult execute(String id) {
        RecoveryCase current = get(id); RecoveryAction action = Optional.ofNullable(current.recommendedAction()).orElseThrow(() -> new IllegalStateException("Analyze this case before execution"));
        if (current.status() == RecoveryStatus.ESCALATED || current.status() == RecoveryStatus.UNRECOVERABLE || current.status() == RecoveryStatus.STOPPED || current.status() == RecoveryStatus.RECOVERED) throw new IllegalStateException("This recovery case is not available for autonomous execution");
        if (current.status() == RecoveryStatus.ACTION_EXECUTED && action != RecoveryAction.CREATE_PAYMENT_LINK) throw new IllegalStateException("Complete the scheduled recovery task before requesting another action");
        PolicyResult result = policy.evaluate(current, action); log(id, "POLICY_ENGINE", result.approved() ? "POLICY_CHECK_APPROVED" : "POLICY_CHECK_BLOCKED", result.approved() ? "Recovery action approved" : "Blocked by " + result.blockedBy(), Map.of("action", action));
        if (!result.approved()) { RecoveryStatus status = current.paymentCaptured() ? RecoveryStatus.RECOVERED : RecoveryStatus.ACTION_BLOCKED; RecoveryCase blocked = copy(current, status, null, null, null, null, current.attemptCount(), current.activePaymentLink(), current.paymentCaptured() ? current.amountAtRisk() : current.amountRecovered(), current.paymentCaptured() ? Instant.now() : null); return new ExecutionResult(cases.save(blocked), result, null); }
        if (action == RecoveryAction.NO_ACTION) { RecoveryCase blocked = cases.save(copy(current, RecoveryStatus.ACTION_BLOCKED, null, null, null, null, current.attemptCount(), false, current.amountRecovered(), null)); log(id, "SYSTEM", "NO_AUTONOMOUS_ACTION", "No autonomous recovery action was approved for this case", Map.of()); return new ExecutionResult(blocked, result, null); }
        if (action != RecoveryAction.CREATE_PAYMENT_LINK) {
            RecoveryStatus next = action == RecoveryAction.ESCALATE_TO_HUMAN ? RecoveryStatus.ESCALATED : RecoveryStatus.ACTION_EXECUTED;
            RecoveryCase updated = cases.save(copy(current, next, null, null, null, null, current.attemptCount() + 1, false, current.amountRecovered(), null));
            if (next == RecoveryStatus.ESCALATED) {
                log(id, "SYSTEM", "HUMAN_ESCALATION_CREATED", "Autonomous recovery paused; merchant human follow-up is required", Map.of("action", action));
            } else {
                RecoveryTask task = scheduleTask(updated, action);
                log(id, "SYSTEM", "RECOVERY_TASK_SCHEDULED", "Approved recovery task scheduled", Map.of("action", action, "taskId", task.id(), "dueAt", task.dueAt().toString()));
            }
            return new ExecutionResult(updated, result, null);
        }
        for (int attempt=1; attempt<=properties.recovery().maxAttempts(); attempt++) { String reference="REC-"+current.caseReference()+"-A"+attempt; Optional<PaymentLinkResult> existing=paymentLinks.findByReferenceId(reference); if(existing.isEmpty()) return createLink(current,result,reference,attempt); PaymentLinkResult link=existing.get(); if("paid".equalsIgnoreCase(link.status())) { RecoveryCase recovered=copy(current,RecoveryStatus.RECOVERED,null,null,null,null,Math.max(current.attemptCount(),attempt),false,current.amountAtRisk(),Instant.now()); cases.save(recovered); paymentLinkRecords.save(new PaymentLinkRecord(link.externalId(),id,reference,"PAID",Instant.now(),Instant.now())); log(id,"RAZORPAY","RECOVERY_RECONCILED","Existing paid Payment Link reconciled",Map.of("referenceId",reference,"paymentLinkId",link.externalId())); log(id,"SYSTEM","RECOVERY_COMPLETED","Recovery stopped after payment success",Map.of("amountRecovered",current.amountAtRisk())); return new ExecutionResult(recovered,result,link); } if("created".equalsIgnoreCase(link.status())||"partially_paid".equalsIgnoreCase(link.status())) { RecoveryCase waiting=copy(current,RecoveryStatus.WAITING_CUSTOMER,null,null,null,null,Math.max(current.attemptCount(),attempt),true,current.amountRecovered(),null); cases.save(waiting); paymentLinkRecords.save(new PaymentLinkRecord(link.externalId(),id,reference,"ACTIVE",Instant.now(),null)); log(id,"RAZORPAY","PAYMENT_LINK_REUSED","Existing Payment Link reused",Map.of("referenceId",reference,"paymentLinkId",link.externalId())); return new ExecutionResult(waiting,result,link); } }
        throw new IllegalStateException("Maximum recovery attempts reached");
    }
    private ExecutionResult createLink(RecoveryCase current, PolicyResult result, String reference, int attempt) { PaymentLinkResult link; try { link=paymentLinks.create(current,reference); } catch(IllegalStateException e) { if(!e.getMessage().contains("HTTP 400")) throw e; Optional<PaymentLinkResult> existing=paymentLinks.findByReferenceId(reference); if(existing.isPresent()) { PaymentLinkResult found=existing.get(); if("paid".equalsIgnoreCase(found.status())) { RecoveryCase recovered=copy(current,RecoveryStatus.RECOVERED,null,null,null,null,attempt,false,current.amountAtRisk(),Instant.now()); cases.save(recovered); return new ExecutionResult(recovered,result,found); } RecoveryCase waiting=copy(current,RecoveryStatus.WAITING_CUSTOMER,null,null,null,null,attempt,true,current.amountRecovered(),null); cases.save(waiting); return new ExecutionResult(waiting,result,found); } throw e; } RecoveryCase updated=copy(current,RecoveryStatus.WAITING_CUSTOMER,null,null,null,null,attempt,true,current.amountRecovered(),null); cases.save(updated); paymentLinkRecords.save(new PaymentLinkRecord(link.externalId(),current.id(),reference,"ACTIVE",Instant.now(),null)); log(current.id(),"SYSTEM","PAYMENT_LINK_CREATED","Payment link created",Map.of("referenceId",reference,"externalId",link.externalId())); return new ExecutionResult(updated,result,link); }
    @Transactional public RecoveryCase stop(String id) { RecoveryCase current = get(id); if (current.status() == RecoveryStatus.RECOVERED || current.status() == RecoveryStatus.UNRECOVERABLE || current.status() == RecoveryStatus.STOPPED) throw new IllegalStateException("Resolved recovery cases cannot be stopped"); RecoveryCase stopped = copy(current, RecoveryStatus.STOPPED, null, null, null, null, current.attemptCount(), false, current.amountRecovered(), Instant.now()); cases.save(stopped); cancelScheduledTasks(id); log(id, "MERCHANT", "RECOVERY_STOPPED", "Merchant manually stopped recovery", Map.of()); return stopped; }
    @Transactional public int stopOpenCases() {
        List<RecoveryCase> openCases = cases.findAll().stream().filter(recoveryCase -> recoveryCase.status() != RecoveryStatus.RECOVERED && recoveryCase.status() != RecoveryStatus.STOPPED && recoveryCase.status() != RecoveryStatus.UNRECOVERABLE).toList();
        openCases.forEach(recoveryCase -> stop(recoveryCase.id()));
        return openCases.size();
    }
    @Transactional public void markPaymentCaptured(String razorpayPaymentLinkId, String razorpayPaymentId) {
        paymentLinkRecords.findByRazorpayPaymentLinkId(razorpayPaymentLinkId).ifPresent(link -> { RecoveryCase current=get(link.recoveryCaseId()); if (current.status()==RecoveryStatus.RECOVERED) return; RecoveryCase recovered=copy(current, RecoveryStatus.RECOVERED, null, null, null, null, current.attemptCount(), false, current.amountAtRisk(), Instant.now()); cases.save(recovered); cancelScheduledTasks(current.id()); paymentLinkRecords.save(new PaymentLinkRecord(link.razorpayPaymentLinkId(), link.recoveryCaseId(), link.referenceId(), "PAID", link.createdAt(), Instant.now())); log(current.id(), "RAZORPAY", "PAYMENT_CAPTURED", "Razorpay payment captured; recovery stopped", Map.of("paymentLinkId", razorpayPaymentLinkId, "paymentId", razorpayPaymentId == null ? "" : razorpayPaymentId)); log(current.id(), "SYSTEM", "RECOVERY_COMPLETED", "Revenue recovered through Razorpay Test Mode", Map.of("amountRecovered", current.amountAtRisk())); });
    }
    @Transactional public void markPaymentCapturedForCase(String recoveryCaseId, String razorpayPaymentId) {
        if (recoveryCaseId == null || recoveryCaseId.isBlank()) return;
        RecoveryCase current = cases.findById(recoveryCaseId).orElse(null);
        if (current == null || current.status() == RecoveryStatus.RECOVERED) return;
        RecoveryCase recovered = copy(current, RecoveryStatus.RECOVERED, null, null, null, null, current.attemptCount(), false, current.amountAtRisk(), Instant.now());
        cases.save(recovered); cancelScheduledTasks(current.id());
        log(current.id(), "RAZORPAY", "PAYMENT_CAPTURED", "Razorpay payment captured; recovery stopped", Map.of("paymentId", razorpayPaymentId == null ? "" : razorpayPaymentId));
        log(current.id(), "SYSTEM", "RECOVERY_COMPLETED", "Revenue recovered through Razorpay Test Mode", Map.of("amountRecovered", current.amountAtRisk()));
    }
    @Transactional public void recordPaymentFailure(String recoveryCaseId) {
        if (recoveryCaseId == null || recoveryCaseId.isBlank() || cases.findById(recoveryCaseId).isEmpty()) return;
        log(recoveryCaseId, "RAZORPAY", "PAYMENT_FAILED", "Razorpay reported a failed payment; recovery remains active", Map.of());
    }
    @Transactional public RecoveryTask completeTask(String recoveryCaseId, String taskId) {
        RecoveryTask task = tasks.findById(taskId).orElseThrow(() -> new NoSuchElementException("Recovery task not found: " + taskId));
        if (!task.recoveryCaseId().equals(recoveryCaseId)) throw new IllegalStateException("Recovery task does not belong to this case");
        if (task.status() != RecoveryTaskStatus.SCHEDULED) throw new IllegalStateException("Only scheduled recovery tasks can be completed");
        RecoveryCase current = get(recoveryCaseId);
        if (current.status() == RecoveryStatus.STOPPED || current.status() == RecoveryStatus.RECOVERED || current.status() == RecoveryStatus.UNRECOVERABLE) throw new IllegalStateException("Resolved recovery cases cannot complete tasks");
        RecoveryTask completed = tasks.save(new RecoveryTask(task.id(), task.recoveryCaseId(), task.type(), RecoveryTaskStatus.COMPLETED, task.dueAt(), task.createdAt(), Instant.now()));
        cases.save(clearRecommendation(current));
        log(recoveryCaseId, "MERCHANT", "RECOVERY_TASK_COMPLETED", "Merchant completed the scheduled recovery task; case returned for review", Map.of("taskType", task.type()));
        return completed;
    }
    @Transactional public RecoveryCase resolveHumanHandoff(String id, HumanHandoffOutcome outcome, long amountRecovered) {
        RecoveryCase current = get(id);
        if (current.status() != RecoveryStatus.ESCALATED) throw new IllegalStateException("Only escalated cases can receive a human handoff outcome");
        if (outcome == HumanHandoffOutcome.RECOVERED_EXTERNALLY) {
            long recoveredAmount = Math.max(1, Math.min(amountRecovered, current.amountAtRisk()));
            RecoveryCase recovered = copy(current, RecoveryStatus.RECOVERED, null, null, null, null, current.attemptCount(), false, recoveredAmount, Instant.now());
            cases.save(recovered); cancelScheduledTasks(id); log(id, "MERCHANT", "HUMAN_OUTCOME_RECORDED", "Human follow-up recovered revenue outside Razorpay", Map.of("outcome", outcome, "amountRecovered", recoveredAmount)); return recovered;
        }
        if (outcome == HumanHandoffOutcome.NOT_RECOVERABLE) {
            RecoveryCase unrecoverable = copy(current, RecoveryStatus.UNRECOVERABLE, null, null, null, null, current.attemptCount(), false, current.amountRecovered(), Instant.now());
            cases.save(unrecoverable); cancelScheduledTasks(id); log(id, "MERCHANT", "HUMAN_OUTCOME_RECORDED", "Human follow-up marked this case not recoverable", Map.of("outcome", outcome)); return unrecoverable;
        }
        RecoveryCase promised = copy(current, RecoveryStatus.ACTION_EXECUTED, null, null, null, null, current.attemptCount(), false, current.amountRecovered(), null);
        cases.save(promised); log(id, "MERCHANT", "HUMAN_OUTCOME_RECORDED", "Human follow-up recorded a promise to pay", Map.of("outcome", outcome)); return promised;
    }
    private RecoveryTask scheduleTask(RecoveryCase recoveryCase, RecoveryAction action) {
        RecoveryTaskType type = action == RecoveryAction.SEND_REMINDER ? RecoveryTaskType.CUSTOMER_REMINDER : RecoveryTaskType.PAYMENT_RETRY_REVIEW;
        Instant now = Instant.now(); Instant dueAt = now.plusSeconds(action == RecoveryAction.SEND_REMINDER ? 4 * 3600L : 24 * 3600L);
        return tasks.save(new RecoveryTask(UUID.randomUUID().toString(), recoveryCase.id(), type, RecoveryTaskStatus.SCHEDULED, dueAt, now, null));
    }
    private void cancelScheduledTasks(String recoveryCaseId) { tasks.findByRecoveryCaseId(recoveryCaseId).stream().filter(task -> task.status() == RecoveryTaskStatus.SCHEDULED).forEach(task -> tasks.save(new RecoveryTask(task.id(), task.recoveryCaseId(), task.type(), RecoveryTaskStatus.CANCELLED, task.dueAt(), task.createdAt(), null))); }
    private RecoveryCase clearRecommendation(RecoveryCase c) { return new RecoveryCase(c.id(), c.caseReference(), c.customerName(), c.customerEmail(), c.contactAllowed(), c.riskType(), c.amountAtRisk(), c.currency(), c.paymentMethod(), c.failureReason(), c.transactionStatus(), c.previousSuccessfulPayments(), c.previousFailedPayments(), c.attemptCount(), false, RecoveryStatus.DETECTED, null, null, null, List.of(), c.strategySource(), c.amountRecovered(), c.createdAt(), Instant.now(), c.resolvedAt()); }
    private void log(String id, String actor, String type, String msg, Map<String,Object> metadata) { audits.save(new AuditEvent(UUID.randomUUID().toString(), id, actor, type, msg, metadata, Instant.now())); }
    private RecoveryCase copy(RecoveryCase c, RecoveryStatus status, String diagnosis, Double score, RecoveryAction action, List<String> reasons, int attempts, boolean activeLink, long recovered, Instant resolved) { return copy(c, status, diagnosis, score, action, reasons, null, attempts, activeLink, recovered, resolved); }
    private RecoveryCase copy(RecoveryCase c, RecoveryStatus status, String diagnosis, Double score, RecoveryAction action, List<String> reasons, StrategySource source, int attempts, boolean activeLink, long recovered, Instant resolved) { return new RecoveryCase(c.id(), c.caseReference(), c.customerName(), c.customerEmail(), c.contactAllowed(), c.riskType(), c.amountAtRisk(), c.currency(), c.paymentMethod(), c.failureReason(), c.transactionStatus(), c.previousSuccessfulPayments(), c.previousFailedPayments(), attempts, activeLink, status, diagnosis == null ? c.diagnosis() : diagnosis, score == null ? c.recoverabilityScore() : score, action == null ? c.recommendedAction() : action, reasons == null ? c.reasons() : reasons, source == null ? c.strategySource() : source, recovered, c.createdAt(), Instant.now(), resolved == null ? c.resolvedAt() : resolved); }
    public record ExecutionResult(RecoveryCase recoveryCase, PolicyResult policy, PaymentLinkResult paymentLink) {}
}
