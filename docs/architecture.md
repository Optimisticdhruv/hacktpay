# RecoverAI Architecture

```text
React merchant console
  -> Spring Boot API
     -> deterministic threshold rules
     -> policy gate
     -> Razorpay Test Mode Payment Link
     -> Razorpay signed webhook
     -> Firestore recovery case + append-only audit
     -> frontend status polling / refresh
```

The strategy engine does not use a live LLM. It is deterministic and reads thresholds from `backend/.env`.

## Autonomous thresholds

- Payment Link: successful-payment history and score must meet configured minimums.
- High amount: amounts above `RECOVERAI_MAX_RECOVERY_AMOUNT_PAISE` escalate to a human.
- Contact restricted, already paid, or attempt limit reached: blocked or escalated.
- All money is stored in paise.

Razorpay is the payment authority. RecoverAI never collects card details, CVV, or OTP.
