# RecoverAI

RecoverAI is a bounded revenue-recovery agent for Razorpay. It detects revenue at risk, recommends a recovery action, applies deterministic safety policies, executes approved payment-link recovery, and keeps an append-only audit trail.

## Current build

The first working vertical slice is in `backend/`:

- Firebase Firestore storage is the production backend. `RECOVERAI_STORAGE_MODE=memory` is only a local developer fixture mode; it never simulates or reports a recovery.
- Seeded payment-failure, checkout-abandonment, and overdue-receivable cases.
- Deterministic fallback strategy engine (safe even without an LLM).
- Policy gate that blocks already-paid, contact-forbidden, duplicate-link, stopped, and max-attempt cases.
- Razorpay Test Mode Payment Link client. It refuses to send an action unless valid Test Mode credentials are configured.

## Firebase migration

MySQL/Flyway/JPA from the original brief is intentionally replaced with Firebase Firestore. Firebase Authentication will be added when the merchant login flow is built. Set `RECOVERAI_STORAGE_MODE=firestore`, `FIREBASE_PROJECT_ID`, and `FIREBASE_SERVICE_ACCOUNT_PATH` for server-side Firestore access; credentials are never committed.

## Run backend

```powershell
Copy-Item .env.example .env
cd backend
mvn spring-boot:run
```

Open `http://localhost:8080/api/recovery-cases`. Analyze a case with `POST /api/recovery-cases/demo-payment-failure/analyze`, then execute it with `POST /api/recovery-cases/demo-payment-failure/execute`.

All monetary values use paise. The seeded `RCV-1048` case is ₹4,999 (`499900` paise).

## Current MVP capabilities

- Firestore-backed recovery cases, audit events, payment-link records, and webhook idempotency records.
- A deterministic, policy-bounded recovery strategy engine. This is the safe fallback while no live LLM provider is configured.
- Razorpay Test Mode Payment Links, paid-link reconciliation, and signed Razorpay webhook processing.
- A merchant console with live recovery KPIs, case details, policy-controlled actions, and audit timeline.
- A synthetic evaluation endpoint. It never calls Razorpay and every response is labelled `SIMULATED`.

## Run the full project

Terminal 1 (backend):

```powershell
cd C:\Users\Dhruv\Downloads\hactkpay\backend
$env:RECOVERAI_STORAGE_MODE="firestore"
$env:FIREBASE_PROJECT_ID="your-firebase-project-id"
$env:FIREBASE_SERVICE_ACCOUNT_PATH="C:\Users\Dhruv\Downloads\hactkpay\backend\firebase-service-account.json"
mvn spring-boot:run
```

Terminal 2 (frontend):

```powershell
cd C:\Users\Dhruv\Downloads\hactkpay\frontend
npm install
.\node_modules\.bin\vite.cmd
```

Open `http://localhost:5173`.

## Safe verification commands

```powershell
Invoke-RestMethod http://localhost:8080/api/dashboard/summary
Invoke-RestMethod -Method Post http://localhost:8080/api/evaluation/run -ContentType "application/json" -Body '{"datasetSize":240,"seed":42}'
```

The evaluation response is synthetic; do not present its recovered value as Razorpay or merchant revenue.

## Razorpay webhook test

For a fresh Razorpay Test Mode payment, subscribe to `payment_link.paid`, `payment.captured`, `payment.failed`, and `order.paid` in the Razorpay Dashboard. Keep `RECOVERAI_PUBLIC_WEBHOOK_ONLY=true` when Cloudflare Tunnel is open, and expose only:

```text
POST /api/webhooks/razorpay
```

After the test payment, verify the timeline contains `PAYMENT_CAPTURED` and `RECOVERY_COMPLETED`.
