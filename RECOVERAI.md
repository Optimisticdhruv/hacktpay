# RECOVERAI
## Master Build Document
### Autonomous AI Revenue Recovery Agent for Razorpay

**Track:** Track 03 — AI Revenue Recovery  
**Project Name:** RecoverAI  
**Build Window:** 5 Days  
**Primary Payment Platform:** Razorpay Test Mode  
**Primary Backend:** Java + Spring Boot  
**Primary Frontend:** React  
**Database:** MySQL  
**AI:** Structured LLM reasoning with deterministic safety controls  
**ML Training:** Not required for MVP  
**Evaluation:** Synthetic merchant revenue dataset + real Razorpay Test Mode execution for selected cases

---

# 0. INSTRUCTIONS TO THE CODING AGENT

You are responsible for building this project end-to-end.

Do not treat this document as a vague idea-generation document. Treat it as the source of truth for product behavior, architecture, implementation priorities, safety constraints, APIs, data model, UI behavior and evaluation.

Your responsibility is to:

1. Understand the complete product before coding.
2. Build working functionality rather than placeholder interfaces.
3. Prefer simple reliable engineering over unnecessary complexity.
4. Use Razorpay Test Mode for all payment operations.
5. Never use live payment credentials.
6. Never hardcode secrets.
7. Keep AI recommendations bounded by deterministic policy checks.
8. Make important operations idempotent.
9. Maintain an append-only audit trail of recovery activity.
10. Design for graceful failure.
11. Do not introduce unnecessary microservices.
12. Do not introduce Kafka, Kubernetes or distributed infrastructure unless absolutely required.
13. Do not train an ML model during the MVP phase.
14. Build the complete closed-loop recovery flow before adding optional features.
15. Ensure the application can still demonstrate core functionality if the LLM service fails.
16. Keep all monetary calculations integer-based using currency subunits.
17. Separate synthetic evaluation results from actual Razorpay Test Mode payment results.
18. Never claim synthetic recovered revenue is real merchant revenue.
19. Use proper validation, error responses, logging and testing.
20. Keep code production-oriented and understandable.

When a detail is not explicitly specified here, make the simplest technically sound decision that preserves the architecture and product intent.

Do not repeatedly request clarification for minor implementation decisions.

---

# 1. EXECUTIVE SUMMARY

RecoverAI is an autonomous revenue-recovery system for merchants.

Businesses continuously lose revenue because customers:

- experience payment failures,
- abandon checkout,
- fail subscription renewals,
- leave invoices unpaid,
- encounter temporary payment problems,
- forget to retry payments.

Traditional payment dashboards generally surface the failed transaction but do not close the recovery loop.

RecoverAI does.

The system:

```text
Detects revenue at risk
        ↓
Diagnoses what happened
        ↓
Estimates whether recovery makes sense
        ↓
Chooses the next-best recovery action
        ↓
Explains the decision
        ↓
Checks deterministic safety policies
        ↓
Executes an approved recovery action
        ↓
Observes Razorpay payment state
        ↓
Retries / escalates / stops
        ↓
Measures revenue recovered
        ↓
Stores the entire audit trail
```

The core design philosophy is:

```text
AI proposes.
Policy engine approves.
Payment infrastructure executes.
Webhook confirms.
State machine decides what happens next.
Audit trail records everything.
```

The AI must NEVER have unrestricted authority over money-related actions.

---

# 2. PROBLEM STATEMENT

Revenue loss rarely happens at one isolated point.

A customer may:

- reach checkout and abandon,
- attempt payment and fail,
- fail multiple times,
- have a subscription renewal fail,
- leave an invoice overdue,
- receive a payment link but never complete payment,
- successfully pay while a recovery flow is still active.

Merchants therefore face fragmented recovery processes.

Existing systems often tell merchants:

```text
Payment failed.
Invoice overdue.
Checkout abandoned.
Subscription renewal failed.
```

But detection alone does not recover revenue.

Someone still needs to determine:

- Is this revenue actually recoverable?
- Why did the transaction fail?
- Should we contact the customer now?
- Should we wait?
- Should we generate another payment route?
- Has the customer already paid?
- How many times have we contacted them?
- Should we stop?
- Should a human handle this case?
- Did the intervention actually recover revenue?
- Was the intervention economically worthwhile?

RecoverAI closes that loop.

---

# 3. PRODUCT VISION

RecoverAI should behave like an intelligent revenue-operations employee who operates under strict company policies.

The merchant should be able to see:

```text
Revenue at risk
₹284,500

Recovery attempted
₹176,000

Revenue recovered
₹93,400

Cases recovered
31

Cases still active
12

Cases escalated
7

Policy violations
0
```

The most important number is:

# REVENUE RECOVERED

Not:

```text
AI tokens used
Model responses
Number of predictions
Chatbot messages
```

The product must remain business-outcome oriented.

---

# 4. HACKATHON OBJECTIVE

The product must demonstrate:

- detection of revenue at risk,
- diagnosis,
- selection of recovery intervention,
- bounded execution,
- stopping rules,
- compliant escalation,
- money recovered across a batch,
- auditability,
- graceful failure handling.

The final system should look like a credible production prototype rather than an AI demo.

---

# 5. CORE DIFFERENTIATOR

Do NOT build:

```text
payment failed
     ↓
send reminder
```

Build:

```text
Revenue Event
     ↓
Risk Detection
     ↓
Context Collection
     ↓
Diagnosis
     ↓
Recoverability Assessment
     ↓
Strategy Selection
     ↓
Explanation
     ↓
Policy Gate
     ↓
Execution
     ↓
Outcome Observation
     ↓
Retry / Escalate / Stop
     ↓
Revenue Measurement
     ↓
Audit
```

That closed loop is the product.

---

# 6. PRIMARY USE CASES

The MVP should support three revenue-risk categories well.

## 6.1 Payment Failure

Example:

```text
Customer attempted ₹4,999 UPI payment.
Payment failed.
Customer has previously paid successfully.
No successful payment exists for this order.
```

RecoverAI:

```text
Detect failure
→ classify as transient/recoverable
→ generate recovery strategy
→ policy approves
→ create Razorpay Payment Link
→ customer pays
→ webhook received
→ recovery marked successful
→ agent stops
```

This is the primary live demo flow.

---

## 6.2 Checkout Abandonment

Example:

```text
Customer created checkout/order
but no successful payment appears within threshold.
```

Possible action:

```text
WAIT
SEND_REMINDER
CREATE_PAYMENT_LINK
NO_ACTION
```

The system should consider:

- checkout age,
- cart amount,
- customer history,
- previous interventions,
- active payment state.

---

## 6.3 Overdue Receivable

Example:

```text
Invoice amount: ₹18,000
Due date exceeded by 5 days
Customer has not paid
No active dispute
Contact permitted
```

Potential flow:

```text
Detect overdue
→ analyze payment history
→ choose reminder/payment-link/escalation
→ execute bounded action
→ track outcome
```

---

# 7. OPTIONAL FOURTH USE CASE

Subscription renewal failure may be included after the first three scenarios are complete.

Do not allow it to delay the MVP.

---

# 8. NON-GOALS

The MVP is NOT:

- a full accounting platform,
- CRM replacement,
- WhatsApp automation platform,
- voice-call center,
- collections platform,
- fraud-detection platform,
- lending platform,
- payment gateway replacement,
- large-scale ML research project,
- multi-agent swarm,
- Kubernetes deployment exercise.

Avoid unnecessary scope.

---

# 9. USERS

Primary persona:

## Merchant / Revenue Operations Manager

They need to answer:

- How much revenue is currently at risk?
- Why?
- What is RecoverAI doing?
- Why did RecoverAI choose that action?
- Has the customer already paid?
- How much money has RecoverAI recovered?
- Which strategies work best?
- Which cases require humans?
- Has RecoverAI violated any policies?

---

# 10. HIGH-LEVEL USER EXPERIENCE

Merchant logs in.

Dashboard:

```text
Revenue At Risk
₹2,84,500

Revenue Recovered
₹93,400

Recovery Rate
32.83%

Active Recoveries
18

Escalations
5
```

Below:

```text
Revenue Recovery Trend

Recovery by Risk Type

Recovery Strategy Performance

Recent Recoveries
```

Merchant opens:

```text
Recovery Cases
```

Example row:

```text
RCV-1048
Customer: Aarav Shah
₹7,499
Payment Failure
Recoverability: 84%
WAITING_CUSTOMER
Payment Link
```

Merchant opens case.

They see:

```text
Revenue at risk: ₹7,499

Diagnosis:
Transient payment failure

Recoverability:
84%

Recommended intervention:
Payment Link

Why:
- Customer completed checkout
- First payment failure
- Strong payment history
- No successful transaction exists
```

Then:

```text
Policy checks

✓ payment not already captured
✓ attempts below limit
✓ contact allowed
✓ no duplicate active link
✓ recovery economically valid
```

Timeline:

```text
10:02 Checkout created

10:04 Payment failed

10:04 Revenue risk detected

10:05 Agent analysis completed

10:05 Payment Link recommended

10:05 Policy gate approved

10:06 Razorpay Payment Link created

10:10 Payment captured

10:10 Recovery successful

10:10 Recovery stopped
```

This screen is a major demo component.

---

# 11. SYSTEM ARCHITECTURE

Use a modular monolithic architecture.

Do NOT prematurely introduce multiple backend services.

```text
                        ┌──────────────────────┐
                        │      React UI        │
                        └──────────┬───────────┘
                                   │
                              REST / JSON
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │ Spring Boot Backend  │
                        └──────────┬───────────┘
                                   │
     ┌─────────────────────────────┼─────────────────────────────┐
     │                             │                             │
     ▼                             ▼                             ▼

Transaction Module          Recovery Module                 Audit Module
     │                             │
     │                             ├── Risk Detector
     │                             ├── Context Builder
     │                             ├── AI Strategy Engine
     │                             ├── Policy Engine
     │                             ├── Recovery Executor
     │                             └── State Machine
     │
     └─────────────────────────────┐
                                   │
                                   ▼
                         Razorpay Integration
                                   │
                   ┌───────────────┴───────────────┐
                   │                               │
               Orders API                   Payment Links API
                   │                               │
                   └───────────────┬───────────────┘
                                   │
                              Razorpay
                                   │
                              Test Payment
                                   │
                                   ▼
                              Webhooks
                                   │
                                   ▼
                         Webhook Processor
                                   │
                                   ▼
                         Recovery State Engine
                                   │
                                   ▼
                               MySQL
```

---

# 12. TECHNOLOGY STACK

## Frontend

```text
React
Vite
Tailwind CSS
Axios
React Router
Recharts
Lucide React
```

Use TypeScript if convenient.

Preferred:

```text
React + TypeScript
```

---

## Backend

```text
Java 21
Spring Boot 3.x
Spring Web
Spring Data JPA
Spring Validation
Spring Security
MySQL Driver
Jackson
Lombok optional
```

Use Maven.

---

## Database

```text
MySQL 8+
```

Use migrations if possible.

Preferred:

```text
Flyway
```

---

## Payment Platform

```text
Razorpay Test Mode
```

Razorpay Test Mode uses test credentials and simulated transactions; Test Mode API keys can be generated without adding a live website.

Use:

```text
Orders API
Payment Links API
Payment Webhooks
```

Razorpay exposes Orders APIs for creating and retrieving payment-related orders.

Payment Links can be created, fetched, updated and cancelled programmatically.

---

# 13. PROJECT DIRECTORY

Recommended monorepo:

```text
recoverai/
│
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── layouts/
│   │   ├── pages/
│   │   ├── types/
│   │   ├── utils/
│   │   └── App.tsx
│   ├── package.json
│   └── vite.config.ts
│
├── backend/
│   ├── src/main/java/com/recoverai/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── recovery/
│   │   │   ├── detector/
│   │   │   ├── strategy/
│   │   │   ├── policy/
│   │   │   ├── executor/
│   │   │   └── state/
│   │   ├── razorpay/
│   │   ├── webhook/
│   │   ├── audit/
│   │   ├── exception/
│   │   └── RecoverAiApplication.java
│   │
│   ├── src/main/resources/
│   │   ├── db/migration/
│   │   ├── application.yml
│   │   └── application-local.yml.example
│   │
│   └── pom.xml
│
├── data/
│   ├── synthetic_revenue_events.csv
│   ├── customers.csv
│   └── scenarios.json
│
├── scripts/
│   └── generate_dataset.py
│
├── docs/
│   ├── architecture.md
│   ├── api.md
│   ├── database.md
│   └── demo-script.md
│
├── .gitignore
├── .env.example
└── README.md
```

---

# 14. CORE DOMAIN ENTITIES

Create these logical entities.

## Merchant

```text
id
name
email
currency
timezone
created_at
updated_at
```

MVP may seed one merchant.

---

# 15. CUSTOMER ENTITY

```text
id
merchant_id
external_customer_id
name
email
phone
segment
contact_allowed
created_at
updated_at
```

Customer segment:

```text
NEW
REGULAR
HIGH_VALUE
AT_RISK
```

Do not use these segments for discriminatory decisions.

They exist only for merchant relationship/recovery prioritization.

---

# 16. TRANSACTION ENTITY

```text
id
merchant_id
customer_id
external_transaction_id

razorpay_order_id
razorpay_payment_id

amount
currency

payment_method

status

failure_code
failure_reason

created_at
updated_at
paid_at
failed_at
```

Transaction status:

```text
CREATED
ATTEMPTED
AUTHORIZED
CAPTURED
FAILED
CANCELLED
```

Money must be stored as integer subunits.

Example:

```text
₹4,999 = 499900 paise
```

Never use floating-point arithmetic for money.

---

# 17. REVENUE EVENT

Represents an event indicating possible revenue loss.

```text
id

merchant_id
customer_id
transaction_id

event_type

amount_at_risk

detected_at

source

metadata_json

processed
created_at
```

Types:

```text
PAYMENT_FAILURE
CHECKOUT_ABANDONMENT
OVERDUE_RECEIVABLE
SUBSCRIPTION_FAILURE
```

---

# 18. RECOVERY CASE

Central entity.

```text
id

case_reference

merchant_id
customer_id
transaction_id
revenue_event_id

risk_type

amount_at_risk

recoverability_score

diagnosis

recommended_action

status

attempt_count

amount_recovered

started_at
resolved_at
created_at
updated_at
```

Statuses:

```text
DETECTED

ANALYZING

ACTION_PENDING

ACTION_APPROVED

ACTION_BLOCKED

ACTION_EXECUTED

WAITING_CUSTOMER

RECOVERED

ESCALATED

STOPPED

UNRECOVERABLE
```

---

# 19. AGENT DECISION

Store every recommendation.

```text
id
recovery_case_id

diagnosis
recoverability_score
recommended_action
delay_minutes

reasoning_summary_json

model_provider
model_name

raw_response_hash

fallback_used

created_at
```

Do NOT store hidden chain-of-thought.

Store concise decision rationale suitable for audit.

Example:

```json
{
  "reasons": [
    "first payment failure",
    "checkout completed",
    "strong prior payment history",
    "no successful payment exists"
  ]
}
```

---

# 20. RECOVERY ATTEMPT

```text
id

recovery_case_id

attempt_number

strategy

status

external_reference

started_at
completed_at

failure_code
failure_message

created_at
```

Status:

```text
PENDING
APPROVED
BLOCKED
EXECUTING
SUCCESS
FAILED
EXPIRED
CANCELLED
```

---

# 21. PAYMENT LINK RECORD

```text
id

recovery_case_id

razorpay_payment_link_id

reference_id

short_url

amount

currency

status

expires_at

created_at
paid_at
cancelled_at
```

Razorpay's Payment Link creation API is:

```text
POST /v1/payment_links
```

and supports fields including amount, currency, reference ID, customer information, expiration, description and callback configuration.

Use a unique `reference_id`.

Example:

```text
REC-RCV1048-A1
```

---

# 22. WEBHOOK EVENT TABLE

Critical for idempotency.

```text
id

external_event_id

event_type

payload_hash

signature_valid

processing_status

received_at
processed_at

error_message
```

Create a unique constraint on:

```text
external_event_id
```

If the same event arrives twice:

```text
already processed
→ return HTTP 200
→ perform no duplicate action
```

---

# 23. AUDIT LOG

Append-only.

Never overwrite audit history.

```text
id

recovery_case_id

actor_type
actor_reference

event_type

event_message

metadata_json

created_at
```

Actor type:

```text
SYSTEM
AI_AGENT
POLICY_ENGINE
MERCHANT
RAZORPAY
CUSTOMER
```

Events might include:

```text
REVENUE_RISK_DETECTED

AI_ANALYSIS_STARTED

AI_DECISION_CREATED

POLICY_CHECK_APPROVED

POLICY_CHECK_BLOCKED

PAYMENT_LINK_REQUESTED

PAYMENT_LINK_CREATED

PAYMENT_LINK_CREATION_FAILED

PAYMENT_CAPTURED

RECOVERY_COMPLETED

MAX_ATTEMPTS_REACHED

RECOVERY_ESCALATED

RECOVERY_STOPPED

WEBHOOK_DUPLICATE_IGNORED
```

---

# 24. CORE RECOVERY ACTIONS

Initial supported actions:

```text
NO_ACTION

WAIT_AND_RETRY

SEND_REMINDER

CREATE_PAYMENT_LINK

ESCALATE_TO_HUMAN
```

Do not implement twenty strategies.

These five are sufficient.

---

# 25. DETECTION ENGINE

Detection should be deterministic.

AI should NOT determine whether a Razorpay payment is captured or failed.

Examples:

```text
transaction.status == FAILED
→ PAYMENT_FAILURE
```

Checkout abandonment:

```text
order created
AND
no successful payment
AND
checkout age > configured threshold
→ CHECKOUT_ABANDONMENT
```

Overdue:

```text
invoice due date < today
AND
invoice unpaid
→ OVERDUE_RECEIVABLE
```

---

# 26. RECOVERY CONTEXT BUILDER

Before asking AI for a recommendation, gather structured context.

Example:

```json
{
  "caseId": "RCV-1048",
  "riskType": "PAYMENT_FAILURE",
  "amountAtRisk": 749900,
  "currency": "INR",

  "paymentMethod": "UPI",

  "failureReason": "payment_timeout",

  "customer": {
    "segment": "HIGH_VALUE",
    "previousSuccessfulPayments": 8,
    "previousFailedPayments": 1,
    "contactAllowed": true
  },

  "currentState": {
    "successfulPaymentExists": false,
    "activeRecoveryLinkExists": false,
    "recoveryAttempts": 0
  }
}
```

Never send unnecessary private customer information to the LLM.

---

# 27. AI STRATEGY ENGINE

The LLM should produce structured JSON only.

Do not allow free-form autonomous actions.

Required schema:

```json
{
  "diagnosis": "TRANSIENT_PAYMENT_FAILURE",
  "recoverabilityScore": 0.84,
  "recommendedAction": "CREATE_PAYMENT_LINK",
  "delayMinutes": 0,
  "reasons": [
    "customer completed checkout",
    "first payment failure",
    "previous payment history is strong"
  ]
}
```

Allowed action enum:

```text
NO_ACTION
WAIT_AND_RETRY
SEND_REMINDER
CREATE_PAYMENT_LINK
ESCALATE_TO_HUMAN
```

Score:

```text
0.00 - 1.00
```

Validate the LLM output.

If:

- malformed JSON,
- missing fields,
- invalid enum,
- score outside range,
- timeout,
- API failure,

then invoke fallback strategy engine.

---

# 28. SYSTEM PROMPT FOR AI ENGINE

Recommended logical prompt:

```text
You are the recovery-strategy component of a merchant revenue recovery system.

Your role is advisory.

You MUST NOT execute payments or contact customers.

Analyze the structured revenue-risk context.

Return only valid JSON matching the required schema.

Choose exactly one allowed action.

Optimize for legitimate revenue recovery while minimizing unnecessary customer contact.

Prefer stopping or escalation when information is uncertain.

Never recommend another recovery action if payment has already succeeded.

Never recommend contacting a customer who has contact_allowed=false.

Do not exceed the system recovery-attempt policy.

Your recommendation is not final. A deterministic policy engine will validate it.
```

Append structured case context.

---

# 29. FALLBACK STRATEGY ENGINE

The application MUST work without the LLM.

Implement deterministic fallback logic.

Example:

```text
IF payment already successful
→ NO_ACTION

ELSE IF contact not allowed
→ ESCALATE_TO_HUMAN / NO_ACTION

ELSE IF attempts >= maximum
→ ESCALATE_TO_HUMAN

ELSE IF PAYMENT_FAILURE
AND attempts == 0
→ CREATE_PAYMENT_LINK

ELSE IF CHECKOUT_ABANDONMENT
AND checkout age < minimum intervention delay
→ WAIT_AND_RETRY

ELSE IF OVERDUE_RECEIVABLE
→ SEND_REMINDER

ELSE
→ ESCALATE_TO_HUMAN
```

Set:

```text
fallback_used = true
```

in the agent decision.

This ensures demo resilience.

---

# 30. POLICY ENGINE

This is one of the most important components.

No financial/recovery action executes without policy approval.

Core policy checks:

```text
P1: payment has NOT already succeeded

P2: recovery attempts < configured maximum

P3: customer contact is allowed when contact is required

P4: no equivalent active Payment Link already exists

P5: case is not STOPPED

P6: case is not RECOVERED

P7: amount at risk > 0

P8: amount and currency match transaction context

P9: intervention is supported

P10: recovery remains economically sensible
```

Return:

```json
{
  "approved": true,
  "checks": [
    {
      "rule": "PAYMENT_NOT_ALREADY_CAPTURED",
      "passed": true
    },
    {
      "rule": "MAX_ATTEMPTS",
      "passed": true
    }
  ]
}
```

If blocked:

```json
{
  "approved": false,
  "blockedBy": "PAYMENT_ALREADY_CAPTURED"
}
```

Record this in the audit trail.

---

# 31. MAXIMUM ATTEMPTS

Default:

```text
MAX_RECOVERY_ATTEMPTS = 3
```

Make configurable.

When reached:

```text
STOP or ESCALATE
```

Never endlessly contact customers.

---

# 32. STOPPING RULES

Immediately stop recovery when:

```text
payment succeeded

customer opted out

max attempts reached

invoice cancelled

merchant manually stopped case

active dispute requires human review

revenue already recovered through another transaction

policy engine determines unsafe state
```

This is central to the hackathon solution.

---

# 33. ECONOMIC STOP RULE

Optional but recommended.

Example:

```text
Expected recovery value =
recoverability_probability × amount_at_risk
```

If expected value is below estimated intervention cost, choose:

```text
NO_ACTION
```

Do not over-engineer intervention-cost models during MVP.

A small configuration-based cost model is sufficient.

---

# 34. RAZORPAY AUTHENTICATION

Use environment variables:

```text
RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
RAZORPAY_WEBHOOK_SECRET
```

Never commit secrets.

Razorpay uses a key ID and key secret to authenticate API access.

---

# 35. RAZORPAY PAYMENT LINKS

Core recovery execution:

```text
POST /v1/payment_links
```

Conceptual request:

```json
{
  "amount": 499900,
  "currency": "INR",
  "reference_id": "REC-RCV1048-A1",
  "description": "Recovery payment for order ORD-1048",
  "customer": {
    "name": "Demo Customer"
  },
  "expire_by": 1780000000,
  "callback_url": "http://localhost:5173/recovery/success",
  "callback_method": "get",
  "reminder_enable": false
}
```

Do not blindly copy timestamps.

Generate expiration dynamically.

Razorpay documents that Payment Link amounts are represented in currency subunits.

Razorpay currently documents a maximum of 30 Payment Links per business in Test Mode unless the limit is increased by support.

Therefore:

**Do not create Payment Links for the entire synthetic evaluation batch.**

Only real-execute selected demo/evaluation cases.

---

# 36. ORDERS API

Optional checkout demonstration can use:

```text
POST /v1/orders
```

Razorpay describes Orders as entities that can be linked to payments and exposes creation and retrieval APIs.

Use Orders if needed to demonstrate:

```text
checkout
→ failed/unfinished payment
→ recovery
```

Do not let checkout construction delay the recovery engine.

---

# 37. WEBHOOK ENDPOINT

Backend endpoint:

```text
POST /api/webhooks/razorpay
```

Important events may include payment success/failure states.

Razorpay provides webhook events for payment state changes, including captured-payment flows.

Webhook processing:

```text
Receive payload
        ↓
Read event identifier
        ↓
Check duplicate
        ↓
Verify signature
        ↓
Persist webhook receipt
        ↓
Route event
        ↓
Update transaction
        ↓
Update recovery case
        ↓
Append audit event
        ↓
Return HTTP 200
```

---

# 38. PAYMENT SUCCESS HANDLING

On successful payment:

```text
Find related transaction/recovery case

mark transaction CAPTURED

mark recovery attempt SUCCESS

calculate amount recovered

mark recovery case RECOVERED

set resolved_at

append audit log

cancel/disable future recovery workflow

STOP
```

Never execute another recovery action after success.

---

# 39. DUPLICATE WEBHOOK HANDLING

Required.

Algorithm:

```text
IF event_id already exists
    audit duplicate ignored
    return 200
ELSE
    continue processing
```

Add DB uniqueness.

Do not depend only on in-memory cache.

---

# 40. OUT-OF-ORDER WEBHOOKS

Webhook arrival order may not always align perfectly with state transitions.

Do not blindly downgrade transaction status.

For example:

```text
current = CAPTURED
incoming older event = AUTHORIZED
```

Do NOT revert:

```text
CAPTURED → AUTHORIZED
```

Use monotonic state rules where appropriate.

---

# 41. RAZORPAY API FAILURE

If Payment Link creation fails:

```text
attempt status = FAILED

audit API failure

case remains recoverable

retry according to bounded retry policy
```

Do not create duplicate links on ambiguous failures.

Before retrying, verify whether an existing link/reference was already created.

Use unique recovery attempt/reference identifiers.

---

# 42. RETRY POLICY

For infrastructure/API failures only:

```text
maximum technical retries: 2-3

exponential backoff
```

Do NOT conflate:

```text
technical API retries
```

with:

```text
customer recovery attempts
```

They are different concepts.

---

# 43. MAIN BACKEND API

Implement approximately:

```text
GET    /api/dashboard/summary

GET    /api/dashboard/recovery-by-type

GET    /api/dashboard/recovery-trend

GET    /api/recovery-cases

GET    /api/recovery-cases/{id}

POST   /api/recovery-cases/{id}/analyze

POST   /api/recovery-cases/{id}/execute

POST   /api/recovery-cases/{id}/stop

GET    /api/recovery-cases/{id}/audit

GET    /api/revenue-events

POST   /api/demo/revenue-events

POST   /api/payment-links

POST   /api/webhooks/razorpay

POST   /api/evaluation/run

GET    /api/evaluation/latest
```

Use appropriate DTOs.

Do not expose JPA entities directly.

---

# 44. DASHBOARD SUMMARY RESPONSE

Example:

```json
{
  "totalRevenueAtRisk": 28450000,
  "recoveryAttempted": 17600000,
  "revenueRecovered": 9340000,
  "recoveryRate": 32.83,
  "activeCases": 18,
  "recoveredCases": 31,
  "escalatedCases": 5,
  "policyViolations": 0
}
```

Frontend formats subunits as INR.

---

# 45. RECOVERY CASE DETAIL RESPONSE

Example:

```json
{
  "caseReference": "RCV-1048",
  "riskType": "PAYMENT_FAILURE",
  "amountAtRisk": 749900,
  "status": "WAITING_CUSTOMER",

  "diagnosis": "TRANSIENT_PAYMENT_FAILURE",

  "recoverabilityScore": 0.84,

  "recommendedAction": "CREATE_PAYMENT_LINK",

  "reasons": [
    "customer completed checkout",
    "first failed attempt",
    "strong previous successful payment history"
  ],

  "policy": {
    "approved": true,
    "checks": [
      {
        "name": "PAYMENT_NOT_ALREADY_CAPTURED",
        "passed": true
      }
    ]
  },

  "attempts": [],

  "audit": []
}
```

---

# 46. FRONTEND PAGES

Create:

```text
/login              optional/simple demo auth

/dashboard

/recovery-cases

/recovery-cases/:id

/revenue-events

/evaluation

/settings

/demo
```

---

# 47. DASHBOARD PAGE

Must contain:

## Top cards

```text
Revenue At Risk

Revenue Recovered

Recovery Rate

Active Recoveries
```

Charts:

```text
Revenue Recovery Over Time

Recovery by Risk Type

Recovery Strategy Performance
```

Tables:

```text
Recent Recoveries

Cases Requiring Attention
```

UI should be professional fintech SaaS.

Avoid flashy hackathon styling.

---

# 48. RECOVERY CASES PAGE

Filters:

```text
risk type

status

strategy

recoverability range
```

Columns:

```text
Case
Customer
Risk Type
Amount
Recoverability
Strategy
Status
Updated
```

---

# 49. RECOVERY CASE DETAIL PAGE

This is one of the strongest demo pages.

Sections:

```text
Case Overview

AI Diagnosis

Recovery Recommendation

Policy Gate

Current Recovery Action

Customer/Transaction Context

Audit Timeline
```

For each policy:

```text
✓ Payment not captured
✓ Attempt limit
✓ Contact allowed
✓ No active duplicate recovery
```

If blocked:

```text
✕ PAYMENT_ALREADY_CAPTURED
```

---

# 50. DEMO CONTROL PAGE

Create a special page for judges/demo operator.

Buttons:

```text
Create Payment Failure

Create Checkout Abandonment

Create Overdue Invoice

Run Recovery Analysis

Execute Approved Recovery

Open Payment Link

Simulate LLM Failure

Simulate Duplicate Webhook

Simulate API Failure
```

Do not fake Razorpay payment success.

Actual successful payment demonstration should come through Razorpay Test Mode.

Simulation is acceptable only for internal non-Razorpay scenarios and must be clearly labelled.

---

# 51. DATASET REQUIREMENT

No external ML dataset is required.

Generate synthetic merchant revenue-event data.

Target:

```text
200-500 records
```

Example mix:

```text
60% healthy transactions

15% payment failures

10% checkout abandonment

8% overdue receivables

7% subscription failures
```

Exact distribution may vary.

---

# 52. SYNTHETIC DATA FIELDS

Generate fields such as:

```text
event_id

customer_id

transaction_id

amount

currency

event_type

payment_method

failure_reason

previous_success_count

previous_failure_count

customer_segment

checkout_age_minutes

invoice_days_overdue

recovery_attempts

contact_allowed

active_payment_link

already_paid

historical_recovery_probability

ground_truth_outcome
```

---

# 53. GROUND TRUTH OUTCOME

To evaluate recovery strategies in synthetic batch mode, give scenarios deterministic/probabilistic ground-truth outcomes.

Example:

```text
TRANSIENT_UPI_FAILURE
recovery likelihood high

INVALID_PAYMENT_DETAILS
recovery likelihood medium

CONTACT_NOT_ALLOWED
no-contact policy

ALREADY_PAID
must stop

MAX_ATTEMPTS_REACHED
must stop
```

Use seeded randomness for reproducibility.

Example:

```text
seed = 42
```

This allows evaluation runs to be repeated.

---

# 54. IMPORTANT METRIC DISTINCTION

Maintain TWO categories of metrics.

## A. Synthetic Batch Evaluation

Label clearly:

```text
SIMULATED BATCH RECOVERY
```

Example:

```text
300 cases evaluated

₹620,000 synthetic revenue at risk

₹201,000 simulated revenue recovered
```

These demonstrate strategy effectiveness.

---

## B. Razorpay Test Execution

Label clearly:

```text
RAZORPAY TEST MODE EXECUTION
```

Example:

```text
8 recovery actions executed

5 successful test payments

₹14,500 test-mode recovered
```

Do not present these as real settled merchant revenue.

This distinction maintains credibility.

---

# 55. EVALUATION METRICS

Track:

```text
Total revenue events

Revenue at risk

Recoverable cases

Recovery actions attempted

Cases recovered

Amount recovered

Recovery rate

Attempt success rate

Average attempts per recovered case

Escalation rate

Stop-rule correctness

Duplicate intervention count

Policy violation count
```

Ideal:

```text
duplicate interventions = 0

policy violations = 0
```

---

# 56. STRATEGY PERFORMANCE

Display:

```text
CREATE_PAYMENT_LINK

cases attempted
cases recovered
revenue recovered
success rate
```

Likewise:

```text
WAIT_AND_RETRY
SEND_REMINDER
ESCALATE_TO_HUMAN
```

---

# 57. OPTIONAL RECOVERABILITY ML MODEL

Do NOT build until complete MVP works.

Optional Day 4/5 enhancement:

```text
XGBoost / LightGBM
```

Input:

```text
amount

payment method

failure category

customer payment history

checkout age

attempt count

time of day

customer segment
```

Output:

```text
P(recovery)
```

However:

MVP does NOT require training.

A rule/LLM-based recoverability score is sufficient.

---

# 58. SECURITY REQUIREMENTS

Mandatory:

```text
Secrets from environment variables

Never commit API secrets

Validate request inputs

Webhook signature verification

Prevent duplicate webhook processing

Use server-side Razorpay credentials only

Never expose key secret to frontend

Never use live credentials

Sanitize logs

Avoid storing unnecessary customer-sensitive data

Use HTTPS in deployed environment
```

Frontend may receive only safe identifiers/configuration required for checkout.

---

# 59. AUTHENTICATION

For hackathon MVP, simple merchant authentication is enough.

Possible:

```text
Spring Security + JWT
```

But do not let authentication delay core recovery.

If necessary:

```text
one seeded demo merchant
```

with simple login.

---

# 60. ERROR HANDLING

Standard backend format:

```json
{
  "timestamp": "2026-08-29T12:00:00Z",
  "status": 400,
  "code": "INVALID_RECOVERY_ACTION",
  "message": "The requested recovery action is not allowed.",
  "path": "/api/recovery-cases/123/execute"
}
```

Use centralized:

```text
@RestControllerAdvice
```

---

# 61. LOGGING

Log:

```text
request correlation id

case_reference

transaction reference

recovery attempt

external Razorpay identifier

state transition

error category
```

Never log:

```text
RAZORPAY_KEY_SECRET

LLM API key

full payment credentials
```

---

# 62. OBSERVABILITY

At minimum expose/log:

```text
recovery cases created

recovery actions executed

Razorpay API failures

LLM failures

webhook events processed

duplicate events ignored

policy blocks

recoveries completed
```

Optional:

```text
Spring Boot Actuator
```

---

# 63. STATE MACHINE

Centralize state transitions.

Example:

```text
DETECTED
   ↓
ANALYZING
   ↓
ACTION_PENDING
   ↓
ACTION_APPROVED
   ↓
ACTION_EXECUTED
   ↓
WAITING_CUSTOMER
   ↓
RECOVERED
```

Alternative:

```text
ACTION_PENDING
   ↓
ACTION_BLOCKED
   ↓
STOPPED
```

Or:

```text
WAITING_CUSTOMER
   ↓
EXPIRED
   ↓
ANALYZING
   ↓
another bounded attempt
```

Do not let random services directly set arbitrary states.

---

# 64. FAILURE SCENARIOS TO IMPLEMENT

Mandatory:

## Failure 1 — LLM unavailable

```text
LLM request fails
→ fallback strategy engine
→ audit FALLBACK_USED
→ recovery continues safely
```

---

## Failure 2 — Duplicate webhook

```text
same webhook delivered twice
→ first processed
→ second ignored
→ no duplicate recovery
```

---

## Failure 3 — Razorpay API timeout

```text
API call fails
→ record technical failure
→ bounded retry
→ avoid duplicate Payment Link
```

---

## Failure 4 — Customer already paid

AI may recommend action based on stale data.

Before execution:

```text
Policy Engine detects payment success
→ BLOCK
→ mark recovered/stop
```

Excellent judge-demo scenario.

---

## Failure 5 — Maximum attempts reached

```text
attempt count == configured max
→ policy rejects additional recovery
→ escalate or stop
```

---

# 65. CONFIGURATION

Example:

```yaml
recoverai:
  recovery:
    max-attempts: 3
    abandonment-threshold-minutes: 10
    payment-link-expiry-hours: 24
    ai-enabled: true

  evaluation:
    seed: 42
```

Secrets via environment.

---

# 66. ENVIRONMENT VARIABLES

Example `.env.example`:

```text
DB_URL=jdbc:mysql://localhost:3306/recoverai
DB_USERNAME=root
DB_PASSWORD=

RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=

LLM_API_KEY=
LLM_MODEL=

FRONTEND_URL=http://localhost:5173
```

`.gitignore`:

```text
.env
application-local.yml
*.log
node_modules/
target/
.idea/
```

---

# 67. DATABASE INDEXES

Add indexes for:

```text
transaction.external_transaction_id

transaction.razorpay_payment_id

recovery_case.case_reference

recovery_case.status

recovery_case.risk_type

webhook_event.external_event_id UNIQUE

payment_link.razorpay_payment_link_id

audit_log.recovery_case_id

recovery_attempt.recovery_case_id
```

---

# 68. CONCURRENCY PROTECTION

Potential problem:

Two requests attempt recovery simultaneously.

Protect execution.

Options:

```text
optimistic locking using @Version
```

recommended.

Before executing action:

```text
reload case
verify state
verify policy
commit transition
```

Prevent:

```text
two Payment Links
```

for one action.

---

# 69. IDEMPOTENCY

Critical actions should use internal idempotency keys.

Example:

```text
REC-RCV1048-A1
```

Recovery action creation:

```text
case + attempt number
```

should map uniquely to an attempt.

Before making external call:

```text
does attempt already have successful external_reference?
```

If yes:

```text
return existing result
```

---

# 70. TESTING STRATEGY

Backend unit tests:

```text
RiskDetectorTest

PolicyEngineTest

FallbackStrategyTest

RecoveryStateMachineTest

WebhookIdempotencyTest

RecoveryServiceTest
```

Integration tests:

```text
RecoveryCaseControllerTest

WebhookControllerTest

Repository tests
```

Frontend:

At minimum verify critical page rendering and API integration manually.

---

# 71. MOST IMPORTANT POLICY TESTS

Test:

```text
already paid → block

attempt limit reached → block

contact forbidden + contact action → block

duplicate active link → block

valid payment failure → approve

amount <= 0 → block

stopped case → block
```

---

# 72. WEBHOOK TESTS

Test:

```text
valid event

invalid signature

duplicate event

unknown event type

payment captured

payment failed

repeated captured event

out-of-order event
```

---

# 73. DATASET GENERATOR

Create:

```text
scripts/generate_dataset.py
```

Use seeded generator.

Generate:

```text
customers.csv

synthetic_revenue_events.csv
```

Ensure repeatability.

Do not require internet.

---

# 74. EVALUATION ENGINE

The evaluation endpoint:

```text
POST /api/evaluation/run
```

should:

```text
load synthetic events

create/recreate evaluation run

process each case

invoke strategy engine

apply policy

apply simulation outcome model

record result

aggregate metrics
```

Do NOT create real Razorpay Payment Links for all batch cases.

---

# 75. EVALUATION RUN ENTITY

Recommended:

```text
id

run_reference

dataset_size

seed

started_at
completed_at

total_at_risk

total_attempted

total_recovered

recovery_rate

policy_violations

duplicate_interventions

status
```

---

# 76. DEMO WORKFLOW

The main judge demo should follow this exact narrative.

### Step 1

Show dashboard.

```text
Revenue At Risk
₹X

Revenue Recovered
₹Y
```

Explain:

RecoverAI continuously looks for revenue-loss events.

---

### Step 2

Generate/show a payment failure:

```text
Customer: Aarav
Amount: ₹4,999
Method: UPI
State: FAILED
```

---

### Step 3

RecoverAI automatically creates:

```text
Recovery Case RCV-XXXX
```

---

### Step 4

Show analysis:

```text
Diagnosis:
Transient payment failure

Recoverability:
87%

Recommended:
CREATE_PAYMENT_LINK
```

Reasons.

---

### Step 5

Show policy gate:

```text
✓ payment not already captured
✓ attempts below maximum
✓ contact permitted
✓ no duplicate active recovery
```

---

### Step 6

Execute.

Spring backend calls real:

```text
Razorpay Test Mode Payment Link API
```

and displays generated Payment Link.

---

### Step 7

Open Razorpay-hosted Test Mode link.

Complete test payment.

---

### Step 8

Webhook arrives.

RecoverAI updates:

```text
WAITING_CUSTOMER
      ↓
RECOVERED
```

---

### Step 9

Dashboard updates.

```text
Revenue Recovered
+ ₹4,999
```

---

### Step 10

Open audit timeline.

Show every event.

---

### Step 11

Show a failure case.

Recommended:

```text
AI service unavailable
```

RecoverAI:

```text
LLM failed
→ deterministic fallback activated
→ policy approved
→ system continues
```

Alternatively demonstrate duplicate webhook prevention.

---

# 77. WINNING DEMO MOMENT

The strongest moment should be:

```text
AT RISK
₹4,999

      ↓

RECOVERED
₹4,999
```

with actual Razorpay Test Mode confirmation.

Then:

```text
Agent stopped automatically:
PAYMENT_SUCCESS
```

---

# 78. AUDIT TIMELINE EXAMPLE

```text
10:02:11
CHECKOUT_CREATED

10:04:23
PAYMENT_FAILED

10:04:23
REVENUE_RISK_DETECTED
₹4,999

10:04:24
AI_ANALYSIS_STARTED

10:04:25
AI_DECISION_CREATED
CREATE_PAYMENT_LINK
recoverability=0.87

10:04:25
POLICY_CHECK_APPROVED

10:04:26
PAYMENT_LINK_CREATED

10:08:14
PAYMENT_CAPTURED

10:08:14
RECOVERY_COMPLETED
₹4,999

10:08:14
RECOVERY_STOPPED
PAYMENT_SUCCESS
```

---

# 79. UI DESIGN PRINCIPLES

Style:

```text
Professional
Fintech
Clean
High trust
Minimal
Data-driven
```

Avoid:

```text
neon hacker UI
excessive gradients
chatbot-first interface
animations everywhere
huge amounts of text
```

Use status colors carefully.

Important UI information hierarchy:

```text
₹ Money

Status

Why

Action

Result
```

---

# 80. MOBILE RESPONSIVENESS

Desktop dashboard is priority.

Still support sensible responsive behavior for:

```text
tablet
mobile
```

Tables may convert to cards.

---

# 81. ACCESSIBILITY

Include:

```text
proper labels

keyboard-accessible controls

sufficient contrast

status icons + text, not color alone
```

---

# 82. FIVE-DAY IMPLEMENTATION PLAN

## DAY 1 — PAYMENT HEARTBEAT

Objective:

Get Razorpay interaction working.

Build:

```text
Spring Boot project

MySQL connection

basic entities

Razorpay config

Payment Link service

Payment Link endpoint

basic React project

basic demo page
```

Success criterion:

```text
Backend creates real Razorpay Test Mode Payment Link.
```

Then:

```text
Test payment can be completed.
```

Then start webhook.

---

# 83. DAY 2 — RECOVERY DOMAIN

Build:

```text
RevenueEvent

RecoveryCase

RecoveryAttempt

AuditLog

RiskDetector

RecoveryStateMachine

PolicyEngine

Recovery APIs

Case list UI

Case detail UI
```

Success:

```text
synthetic payment failure
→ Recovery Case
→ deterministic recommendation
→ policy
→ Payment Link
```

---

# 84. DAY 3 — AI + WEBHOOK CLOSED LOOP

Build:

```text
AI Strategy Engine

structured JSON parser

fallback engine

webhook verification

webhook idempotency

payment success handling

automatic stop

audit timeline
```

Success:

```text
Payment Failure
→ AI
→ Policy
→ Razorpay Link
→ Test Payment
→ Webhook
→ RECOVERED
```

This is the most important milestone.

---

# 85. DAY 4 — BATCH EVALUATION + DASHBOARD

Build:

```text
dataset generator

evaluation engine

dashboard KPIs

charts

recovery strategy metrics

failure scenarios
```

Success:

```text
200+ synthetic cases evaluated
```

with transparent:

```text
SIMULATED
```

labels.

---

# 86. DAY 5 — HARDENING + DEMO

Focus entirely on:

```text
bugs

error handling

UI polish

loading states

empty states

failure handling

tests

README

architecture diagram

demo data

demo rehearsal

presentation
```

Do NOT add major new features.

---

# 87. DEVELOPMENT PRIORITY

Priority order:

```text
P0 Razorpay Payment Link works

P0 webhook works

P0 payment success closes recovery

P0 policy engine prevents bad actions

P0 audit trail

P1 AI recommendation

P1 fallback engine

P1 dashboard

P1 batch evaluation

P2 optional ML

P2 advanced channels
```

Never work on P2 while P0 is incomplete.

---

# 88. DEFINITION OF MVP COMPLETE

MVP is complete only when:

```text
✓ Merchant can view revenue-risk case

✓ Risk case has diagnosis

✓ Recovery action is selected

✓ Policy gate validates action

✓ Razorpay Test Payment Link can be created

✓ Test payment can be completed

✓ webhook reaches backend

✓ transaction becomes successful

✓ recovery case becomes RECOVERED

✓ subsequent recovery is stopped

✓ audit timeline shows entire process

✓ duplicate webhook does not duplicate actions

✓ LLM failure falls back safely

✓ dashboard reports recovered amount

✓ synthetic evaluation batch works
```

---

# 89. DEFINITION OF WINNING VERSION

Winning version adds:

```text
polished fintech dashboard

clear explainability

recovery strategy comparison

economic stopping rules

excellent failure demonstration

transparent synthetic vs test-mode metrics

strong architecture documentation

smooth 2-4 minute demo

no broken flows
```

---

# 90. DO NOT DO THESE

Do not:

```text
train a large neural network

add Kafka

add Kubernetes

add five microservices

build native mobile apps

build a Chrome extension

build voice calling

build WhatsApp integration before MVP

build twenty recovery actions

use AI for deterministic payment status

allow LLM to call Razorpay directly

expose API secrets

hardcode payment success

fake Razorpay payments

present synthetic metrics as real merchant revenue
```

---

# 91. README MUST INCLUDE

Create a complete README containing:

```text
Project overview

Problem statement

Solution

Architecture

Technology stack

Feature list

Setup requirements

Environment variables

Database setup

Backend run instructions

Frontend run instructions

Razorpay Test Mode setup

Webhook setup

Synthetic dataset generation

Evaluation instructions

Demo workflow

Known limitations

Security considerations
```

---

# 92. LOCAL RUN WORKFLOW

Expected:

Terminal 1:

```text
MySQL
```

Terminal 2:

```text
cd backend
mvn spring-boot:run
```

Terminal 3:

```text
cd frontend
npm install
npm run dev
```

Expose backend webhook endpoint using an appropriate tunnel during local Razorpay testing.

The exact tunneling provider may be chosen by the developer.

---

# 93. INITIAL DEVELOPMENT DATA

Seed:

```text
1 merchant

20 customers

30 transactions

5 recovery cases

multiple audit events
```

This gives frontend developers immediate data.

Later replace/demo against evaluation dataset.

---

# 94. DEMO MERCHANT

Use a fictional merchant.

Example:

```text
NovaCart
```

Do not imply association with a real merchant.

Sample products/transactions can look like generic e-commerce purchases.

---

# 95. RECOVERY CASE EXAMPLE

```json
{
  "caseReference": "RCV-1048",

  "customer": "Aarav Shah",

  "amountAtRisk": 499900,

  "currency": "INR",

  "riskType": "PAYMENT_FAILURE",

  "paymentMethod": "UPI",

  "failureReason": "payment_timeout",

  "previousSuccesses": 7,

  "previousFailures": 0,

  "contactAllowed": true,

  "attemptCount": 0,

  "alreadyPaid": false
}
```

Expected AI:

```json
{
  "diagnosis": "TRANSIENT_PAYMENT_FAILURE",

  "recoverabilityScore": 0.87,

  "recommendedAction": "CREATE_PAYMENT_LINK",

  "delayMinutes": 0,

  "reasons": [
    "payment failure appears transient",
    "customer completed checkout",
    "customer has strong successful payment history"
  ]
}
```

Policy:

```text
APPROVED
```

Outcome:

```text
Razorpay Payment Link created
→ customer test payment
→ webhook
→ RECOVERED
```

---

# 96. SAFETY TEST CASE

Input:

```text
transaction already CAPTURED
```

Suppose stale AI output says:

```text
CREATE_PAYMENT_LINK
```

Policy response MUST be:

```json
{
  "approved": false,
  "blockedBy": "PAYMENT_ALREADY_CAPTURED"
}
```

Then:

```text
STOP RECOVERY
```

This demonstrates why the system is safe.

---

# 97. CONTACT POLICY TEST

Input:

```text
contact_allowed = false
```

AI recommends:

```text
SEND_REMINDER
```

Policy:

```text
BLOCK
```

Possible resulting action:

```text
ESCALATE_TO_HUMAN
```

---

# 98. DUPLICATE LINK TEST

Current state:

```text
active payment link exists
```

AI recommends:

```text
CREATE_PAYMENT_LINK
```

Policy:

```text
BLOCK
ACTIVE_RECOVERY_ALREADY_EXISTS
```

---

# 99. LLM FAILURE TEST

Force:

```text
timeout
```

Result:

```text
LLM_ERROR
↓
FallbackStrategyEngine
↓
deterministic recommendation
↓
PolicyEngine
↓
execution
```

Audit:

```text
AI_PROVIDER_FAILURE

FALLBACK_STRATEGY_USED
```

---

# 100. IMPLEMENTATION PHILOSOPHY

When deciding between:

```text
complex clever architecture
```

and:

```text
simple reliable architecture
```

choose:

# SIMPLE + RELIABLE

The project needs to work during judging.

---

# 101. PRODUCT ONE-LINER

Use:

**RecoverAI is an autonomous revenue-recovery agent that detects revenue leakage, diagnoses why money is at risk, chooses and executes a bounded recovery strategy through Razorpay, and proves every recovered rupee with a complete audit trail.**

---

# 102. SHORT PITCH

Merchants lose money across failed payments, abandoned checkouts and overdue receivables.

Existing systems usually surface the failure but leave recovery to humans.

RecoverAI closes the loop.

It detects revenue at risk, diagnoses the cause, recommends the next-best intervention, passes every action through deterministic safety policies, executes approved recovery through Razorpay Test Mode, observes the result through webhooks, automatically stops when payment succeeds, and records every decision.

The merchant can therefore see not only what went wrong, but:

```text
what RecoverAI did,
why it did it,
whether it was allowed,
and exactly how much revenue was recovered.
```

---

# 103. FINAL ARCHITECTURE PRINCIPLE

Never forget:

```text
                AI
                 │
             recommends
                 │
                 ▼
          POLICY ENGINE
                 │
              approves
                 │
                 ▼
        RECOVERY EXECUTOR
                 │
                 ▼
             RAZORPAY
                 │
                 ▼
             WEBHOOK
                 │
                 ▼
           STATE ENGINE
                 │
                 ▼
        RECOVERED / RETRY
        ESCALATE / STOP
                 │
                 ▼
            AUDIT LOG
```

AI is intelligent.

Policy is authoritative.

Razorpay is transactional.

Webhook is evidence.

Audit is permanent.

---

# 104. FIRST IMPLEMENTATION TASK FOR THE CODING AGENT

Start here.

Do not begin with AI.

Do not begin with dashboard polish.

Do not begin with dataset training.

Build this first:

```text
Spring Boot
      ↓
Razorpay Test Credentials
      ↓
POST /v1/payment_links
      ↓
Create ₹10 test Payment Link
      ↓
Persist Payment Link
      ↓
Open hosted Razorpay page
      ↓
Complete test payment
      ↓
Razorpay webhook
      ↓
Verify webhook
      ↓
Persist payment
      ↓
Audit event
```

When that works, implement:

```text
RecoveryCase
↓
PolicyEngine
↓
RecoveryExecutor
```

Then:

```text
AI Strategy Engine
```

Then:

```text
Dashboard
```

Then:

```text
Evaluation batch
```

This development order is intentional.

---

# 105. CODING AGENT FINAL DIRECTIVE

Build the project incrementally.

After each major component:

```text
compile
test
run
verify
```

Do not produce hundreds of files before confirming basic integration.

Prioritize working vertical slices.

First vertical slice:

```text
API
→ Razorpay
→ payment
→ webhook
→ DB
```

Second:

```text
Revenue Event
→ Recovery Case
→ Policy
→ Recovery
```

Third:

```text
AI
→ Recommendation
→ Policy
→ Execution
```

Fourth:

```text
Batch
→ Metrics
→ Dashboard
```

Fifth:

```text
Failures
→ Tests
→ Polish
```

If optional functionality conflicts with core reliability, remove the optional functionality.

The final product must demonstrate one complete, legitimate, bounded, explainable revenue-recovery loop with real Razorpay Test Mode execution and a measurable batch evaluation.

---

# FINAL SUCCESS CONDITION

The project is successful when a judge can watch this happen:

```text
₹4,999 PAYMENT FAILED
          ↓
RecoverAI detected ₹4,999 at risk
          ↓
Diagnosis generated
          ↓
Recovery strategy selected
          ↓
Policy approved
          ↓
Razorpay Test Payment Link created
          ↓
Customer completes test payment
          ↓
Webhook received
          ↓
₹4,999 marked RECOVERED
          ↓
Agent automatically STOPPED
          ↓
Complete decision trail visible
```

and the dashboard can additionally demonstrate:

```text
hundreds of synthetic revenue-risk scenarios
          ↓
bounded automated decisions
          ↓
measured simulated recovery
          ↓
zero duplicate interventions
          ↓
zero policy violations
```

with synthetic and Razorpay Test Mode metrics clearly distinguished.

That is RecoverAI.