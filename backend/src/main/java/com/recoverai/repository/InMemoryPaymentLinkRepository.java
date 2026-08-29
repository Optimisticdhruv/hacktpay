package com.recoverai.repository;
import com.recoverai.domain.PaymentLinkRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
@Repository @ConditionalOnProperty(prefix="recoverai", name="storage-mode", havingValue="memory", matchIfMissing=true)
public class InMemoryPaymentLinkRepository implements PaymentLinkRepository {
    private final ConcurrentMap<String, PaymentLinkRecord> links = new ConcurrentHashMap<>();
    public PaymentLinkRecord save(PaymentLinkRecord link) { links.put(link.razorpayPaymentLinkId(), link); return link; }
    public Optional<PaymentLinkRecord> findByRazorpayPaymentLinkId(String id) { return Optional.ofNullable(links.get(id)); }
}
