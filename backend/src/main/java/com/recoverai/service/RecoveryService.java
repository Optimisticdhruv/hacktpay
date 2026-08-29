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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class RecoveryService {
    private final RecoveryCaseRepository cases; private final AuditRepository audits; private final RecoveryStrategyService strategy; private final PolicyEngine policy; private final PaymentLinkClient paymentLinks; private final PaymentLinkRepository paymentLinkRecords; private final RecoveryProperties properties;
    public RecoveryService(RecoveryCaseRepository cases, AuditRepository audits, RecoveryStrategyService strategy, PolicyEngine policy, PaymentLinkClient paymentLinks, PaymentLinkRepository paymentLinkRecords, RecoveryProperties properties) { this.cases = cases; this.audits = audits; this.strategy = strategy; this.policy = policy; this.paymentLinks = paymentLinks; this.paymentLinkRecords = paymentLinkRecords; this.properties = properties; }
    public List<RecoveryCase> list() { return cases.findAll(); }
    public RecoveryCase create(RecoveryCase recoveryCase) {
        RecoveryCase saved = cases.save(recoveryCase);
        log(saved.id(), "SYSTEM", "REVENUE_RISK_DETECTED", "Revenue risk case created", Map.of("amountAtRisk", saved.amountAtRisk(), "riskType", saved.riskType().name()));
        return saved;
    }
    public RecoveryCase get(String id) { return cases.findById(id).orElseThrow(() -> new NoSuchElementException("Recovery case not found: " + id)); }
    public List<AuditEvent> audit(String id) { get(id); return audits.findByRecoveryCaseId(id); }
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
        PolicyResult result = policy.evaluate(current, action); log(id, "POLICY_ENGINE", result.approved() ? "POLICY_CHECK_APPROVED" : "POLICY_CHECK_BLOCKED", result.approved() ? "Recovery action approved" : "Blocked by " + result.blockedBy(), Map.of("action", action));
        if (!result.approved()) { RecoveryStatus status = current.paymentCaptured() ? RecoveryStatus.RECOVERED : RecoveryStatus.ACTION_BLOCKED; RecoveryCase blocked = copy(current, status, null, null, null, null, current.attemptCount(), current.activePaymentLink(), current.paymentCaptured() ? current.amountAtRisk() : current.amountRecovered(), current.paymentCaptured() ? Instant.now() : null); return new ExecutionResult(cases.save(blocked), result, null); }
        if (action != RecoveryAction.CREATE_PAYMENT_LINK) { RecoveryStatus next = action == RecoveryAction.ESCALATE_TO_HUMAN ? RecoveryStatus.ESCALATED : RecoveryStatus.ACTION_EXECUTED; return new ExecutionResult(cases.save(copy(current, next, null, null, null, null, current.attemptCount() + 1, current.activePaymentLink(), current.amountRecovered(), null)), result, null); }
        for (int attempt=1; attempt<=properties.recovery().maxAttempts(); attempt++) { String reference="REC-"+current.caseReference()+"-A"+attempt; Optional<PaymentLinkResult> existing=paymentLinks.findByReferenceId(reference); if(existing.isEmpty()) return createLink(current,result,reference,attempt); PaymentLinkResult link=existing.get(); if("paid".equalsIgnoreCase(link.status())) { RecoveryCase recovered=copy(current,RecoveryStatus.RECOVERED,null,null,null,null,Math.max(current.attemptCount(),attempt),false,current.amountAtRisk(),Instant.now()); cases.save(recovered); paymentLinkRecords.save(new PaymentLinkRecord(link.externalId(),id,reference,"PAID",Instant.now(),Instant.now())); log(id,"RAZORPAY","RECOVERY_RECONCILED","Existing paid Payment Link reconciled",Map.of("referenceId",reference,"paymentLinkId",link.externalId())); log(id,"SYSTEM","RECOVERY_COMPLETED","Recovery stopped after payment success",Map.of("amountRecovered",current.amountAtRisk())); return new ExecutionResult(recovered,result,link); } if("created".equalsIgnoreCase(link.status())||"partially_paid".equalsIgnoreCase(link.status())) { RecoveryCase waiting=copy(current,RecoveryStatus.WAITING_CUSTOMER,null,null,null,null,Math.max(current.attemptCount(),attempt),true,current.amountRecovered(),null); cases.save(waiting); paymentLinkRecords.save(new PaymentLinkRecord(link.externalId(),id,reference,"ACTIVE",Instant.now(),null)); log(id,"RAZORPAY","PAYMENT_LINK_REUSED","Existing Payment Link reused",Map.of("referenceId",reference,"paymentLinkId",link.externalId())); return new ExecutionResult(waiting,result,link); } }
        throw new IllegalStateException("Maximum recovery attempts reached");
    }
    private ExecutionResult createLink(RecoveryCase current, PolicyResult result, String reference, int attempt) { PaymentLinkResult link; try { link=paymentLinks.create(current,reference); } catch(IllegalStateException e) { if(!e.getMessage().contains("HTTP 400")) throw e; Optional<PaymentLinkResult> existing=paymentLinks.findByReferenceId(reference); if(existing.isPresent()) { PaymentLinkResult found=existing.get(); if("paid".equalsIgnoreCase(found.status())) { RecoveryCase recovered=copy(current,RecoveryStatus.RECOVERED,null,null,null,null,attempt,false,current.amountAtRisk(),Instant.now()); cases.save(recovered); return new ExecutionResult(recovered,result,found); } RecoveryCase waiting=copy(current,RecoveryStatus.WAITING_CUSTOMER,null,null,null,null,attempt,true,current.amountRecovered(),null); cases.save(waiting); return new ExecutionResult(waiting,result,found); } throw e; } RecoveryCase updated=copy(current,RecoveryStatus.WAITING_CUSTOMER,null,null,null,null,attempt,true,current.amountRecovered(),null); cases.save(updated); paymentLinkRecords.save(new PaymentLinkRecord(link.externalId(),current.id(),reference,"ACTIVE",Instant.now(),null)); log(current.id(),"SYSTEM","PAYMENT_LINK_CREATED","Payment link created",Map.of("referenceId",reference,"externalId",link.externalId())); return new ExecutionResult(updated,result,link); }
    @Transactional public RecoveryCase stop(String id) { RecoveryCase current = get(id); RecoveryCase stopped = copy(current, RecoveryStatus.STOPPED, null, null, null, null, current.attemptCount(), false, current.amountRecovered(), Instant.now()); cases.save(stopped); log(id, "MERCHANT", "RECOVERY_STOPPED", "Merchant manually stopped recovery", Map.of()); return stopped; }
    @Transactional public void markPaymentCaptured(String razorpayPaymentLinkId, String razorpayPaymentId) {
        paymentLinkRecords.findByRazorpayPaymentLinkId(razorpayPaymentLinkId).ifPresent(link -> { RecoveryCase current=get(link.recoveryCaseId()); if (current.status()==RecoveryStatus.RECOVERED) return; RecoveryCase recovered=copy(current, RecoveryStatus.RECOVERED, null, null, null, null, current.attemptCount(), false, current.amountAtRisk(), Instant.now()); cases.save(recovered); paymentLinkRecords.save(new PaymentLinkRecord(link.razorpayPaymentLinkId(), link.recoveryCaseId(), link.referenceId(), "PAID", link.createdAt(), Instant.now())); log(current.id(), "RAZORPAY", "PAYMENT_CAPTURED", "Razorpay payment captured; recovery stopped", Map.of("paymentLinkId", razorpayPaymentLinkId, "paymentId", razorpayPaymentId == null ? "" : razorpayPaymentId)); log(current.id(), "SYSTEM", "RECOVERY_COMPLETED", "Revenue recovered through Razorpay Test Mode", Map.of("amountRecovered", current.amountAtRisk())); });
    }
    @Transactional public void markPaymentCapturedForCase(String recoveryCaseId, String razorpayPaymentId) {
        if (recoveryCaseId == null || recoveryCaseId.isBlank()) return;
        RecoveryCase current = cases.findById(recoveryCaseId).orElse(null);
        if (current == null || current.status() == RecoveryStatus.RECOVERED) return;
        RecoveryCase recovered = copy(current, RecoveryStatus.RECOVERED, null, null, null, null, current.attemptCount(), false, current.amountAtRisk(), Instant.now());
        cases.save(recovered);
        log(current.id(), "RAZORPAY", "PAYMENT_CAPTURED", "Razorpay payment captured; recovery stopped", Map.of("paymentId", razorpayPaymentId == null ? "" : razorpayPaymentId));
        log(current.id(), "SYSTEM", "RECOVERY_COMPLETED", "Revenue recovered through Razorpay Test Mode", Map.of("amountRecovered", current.amountAtRisk()));
    }
    @Transactional public void recordPaymentFailure(String recoveryCaseId) {
        if (recoveryCaseId == null || recoveryCaseId.isBlank() || cases.findById(recoveryCaseId).isEmpty()) return;
        log(recoveryCaseId, "RAZORPAY", "PAYMENT_FAILED", "Razorpay reported a failed payment; recovery remains active", Map.of());
    }
    private void log(String id, String actor, String type, String msg, Map<String,Object> metadata) { audits.save(new AuditEvent(UUID.randomUUID().toString(), id, actor, type, msg, metadata, Instant.now())); }
    private RecoveryCase copy(RecoveryCase c, RecoveryStatus status, String diagnosis, Double score, RecoveryAction action, List<String> reasons, int attempts, boolean activeLink, long recovered, Instant resolved) { return copy(c, status, diagnosis, score, action, reasons, null, attempts, activeLink, recovered, resolved); }
    private RecoveryCase copy(RecoveryCase c, RecoveryStatus status, String diagnosis, Double score, RecoveryAction action, List<String> reasons, StrategySource source, int attempts, boolean activeLink, long recovered, Instant resolved) { return new RecoveryCase(c.id(), c.caseReference(), c.customerName(), c.customerEmail(), c.contactAllowed(), c.riskType(), c.amountAtRisk(), c.currency(), c.paymentMethod(), c.failureReason(), c.transactionStatus(), c.previousSuccessfulPayments(), c.previousFailedPayments(), attempts, activeLink, status, diagnosis == null ? c.diagnosis() : diagnosis, score == null ? c.recoverabilityScore() : score, action == null ? c.recommendedAction() : action, reasons == null ? c.reasons() : reasons, source == null ? c.strategySource() : source, recovered, c.createdAt(), Instant.now(), resolved == null ? c.resolvedAt() : resolved); }
    public record ExecutionResult(RecoveryCase recoveryCase, PolicyResult policy, PaymentLinkResult paymentLink) {}
}
