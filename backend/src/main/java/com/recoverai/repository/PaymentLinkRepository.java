package com.recoverai.repository;
import com.recoverai.domain.PaymentLinkRecord;
import java.util.Optional;
public interface PaymentLinkRepository { PaymentLinkRecord save(PaymentLinkRecord link); Optional<PaymentLinkRecord> findByRazorpayPaymentLinkId(String id); }
