package com.recoverai.repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.recoverai.domain.PaymentLinkRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
@Repository @ConditionalOnProperty(prefix="recoverai", name="storage-mode", havingValue="firestore")
public class FirestorePaymentLinkRepository implements PaymentLinkRepository {
    private final Firestore db; private final ObjectMapper json;
    public FirestorePaymentLinkRepository(Firestore db, ObjectMapper json) { this.db=db; this.json=json; }
    public PaymentLinkRecord save(PaymentLinkRecord link) { try { db.collection("paymentLinks").document(link.razorpayPaymentLinkId()).set(Map.of("payload",json.writeValueAsString(link),"recoveryCaseId",link.recoveryCaseId())).get(); return link; } catch(Exception e) { throw new IllegalStateException("Firestore could not save payment link",e); } }
    public Optional<PaymentLinkRecord> findByRazorpayPaymentLinkId(String id) { try { var d=db.collection("paymentLinks").document(id).get().get(); return d.exists()?Optional.of(json.readValue(d.getString("payload"),PaymentLinkRecord.class)):Optional.empty(); } catch(Exception e) { throw new IllegalStateException("Firestore could not read payment link",e); } }
}
