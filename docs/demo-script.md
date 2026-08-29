# Three-minute demo

1. Open the RecoverAI dashboard and click **Start live recovery**.
2. Enter the customer name and amount. The created case displays its case reference and risk amount.
3. Open the case, show diagnosis, score, threshold-based action, and policy controls.
4. Create the secure Razorpay Test Mode link and complete the test payment on Razorpay's hosted page.
5. Return to RecoverAI. The case automatically changes from `WAITING_CUSTOMER` to `RECOVERED` after the signed webhook is processed.
6. Show `PAYMENT_CAPTURED` and `RECOVERY_COMPLETED` in the audit timeline.

Use the Simulation lab only to explain synthetic evaluation; never present simulated recovered value as real revenue.
