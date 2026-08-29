package com.recoverai.razorpay;
import com.recoverai.domain.RecoveryCase;
import java.util.Optional;
public interface PaymentLinkClient { PaymentLinkResult create(RecoveryCase recoveryCase, String referenceId); Optional<PaymentLinkResult> findByReferenceId(String referenceId); }
