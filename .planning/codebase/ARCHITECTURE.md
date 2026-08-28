# Architecture

**Analysis Date:** 2026-08-28

## System Overview

```text
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend (React/Next.js)                      │
│            `frontend/src/pages/` + `frontend/src/components/`    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Pages: Payments, Approvals, Payment Detail              │   │
│  │ Components: Layout, StatusBadge                          │   │
│  │ State: useEffect + useState (page-level)                │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       │ HTTP + JSON
                       │ (fetch, /api/*)
                       │ [Next.js rewrites to :8080]
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│                 Backend (Kotlin/Dropwizard)                      │
│            `backend/src/main/kotlin/org/light/challenge/`       │
│                                                                   │
│  ┌─ REST Layer ─────────────────────────────────────────────┐   │
│  │ PaymentResource, ApprovalResource, CategorizationResource│   │
│  │ `rest/`                                                   │   │
│  └──────────────────────────────────────────────────────────┘   │
│                       │                                           │
│  ┌─ Service Layer ───▼──────────────────────────────────────┐   │
│  │ ApprovalService, NotificationService, CategorizationService│  │
│  │ `service/`                                                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                       │                                           │
│  ┌─ Repository Layer ▼──────────────────────────────────────┐   │
│  │ PaymentRepository, ApprovalRuleRepository,               │   │
│  │ ApprovalRequestRepository                                 │   │
│  │ `repository/` (in-memory maps)                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                       │                                           │
│  ┌─ Model Layer ─────▼──────────────────────────────────────┐   │
│  │ Payment, ApprovalRequest, ApprovalRule, Enums            │   │
│  │ `model/`                                                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                       │                                           │
│  ┌─ LLM Integration ──▼──────────────────────────────────────┐   │
│  │ LlmClient interface, OllamaClient implementation          │   │
│  │ `llm/` → Ollama (OpenAI-compatible API)                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  Entry Point: `App.kt` (Dropwizard bootstrap)                   │
└─────────────────────────────────────────────────────────────────┘
         │
         │ (Seed data only, no persistent DB)
         │
         ▼
   (In-memory repositories reset on restart)
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| **PaymentResource** | REST endpoints for payment listing, creation, detail | `backend/src/main/kotlin/org/light/challenge/rest/PaymentResource.kt` |
| **ApprovalResource** | REST endpoints for pending approvals and decisions | `backend/src/main/kotlin/org/light/challenge/rest/ApprovalResource.kt` |
| **CategorizationResource** | REST endpoint for AI-powered vendor/department suggestion | `backend/src/main/kotlin/org/light/challenge/rest/CategorizationResource.kt` |
| **ApprovalService** | Core business logic: payment submission, rule matching, approval state transitions | `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt` |
| **CategorizationService** | Two-stage LLM pipeline for vendor inference + department classification | `backend/src/main/kotlin/org/light/challenge/service/CategorizationService.kt` |
| **NotificationService** | Stub for approval notifications (logs only, no actual email) | `backend/src/main/kotlin/org/light/challenge/service/NotificationService.kt` |
| **PaymentRepository** | In-memory CRUD for payments with seed data | `backend/src/main/kotlin/org/light/challenge/repository/PaymentRepository.kt` |
| **ApprovalRuleRepository** | In-memory rule set (amount/department-based approval thresholds) | `backend/src/main/kotlin/org/light/challenge/repository/ApprovalRuleRepository.kt` |
| **ApprovalRequestRepository** | In-memory CRUD for approval requests | `backend/src/main/kotlin/org/light/challenge/repository/ApprovalRequestRepository.kt` |
| **LlmClient** | Interface for LLM calls (abstraction over Ollama) | `backend/src/main/kotlin/org/light/challenge/llm/LlmClient.kt` |
| **OllamaClient** | Implementation of LlmClient using Ollama's OpenAI-compatible API | `backend/src/main/kotlin/org/light/challenge/llm/OllamaClient.kt` |
| **Payments Page** | Lists all payments, submit new payment, suggest vendor/dept | `frontend/src/pages/index.tsx` |
| **Approvals Page** | Shows pending approvals grouped by payment, approve/reject UI | `frontend/src/pages/approvals.tsx` |
| **Payment Detail Page** | Show single payment with its approval requests | `frontend/src/pages/payments/[id].tsx` |
| **Layout Component** | Navigation wrapper for all pages | `frontend/src/components/Layout.tsx` |
| **StatusBadge Component** | Renders payment status with color coding | `frontend/src/components/StatusBadge.tsx` |

## Pattern Overview

**Overall:** Layered (REST → Service → Repository → Model) backend with Next.js stateless frontend.

**Key Characteristics:**
- **Dependency injection via constructor**: Services receive repositories; resources receive services (App.kt wires everything)
- **In-memory repositories**: All data is stored in mutable maps, seeded with sample payments. Resets on restart.
- **No ORM/database**: Repositories directly manage maps; no query builder or migrations
- **Dropwizard configuration**: App extends Dropwizard Application, uses Jackson for JSON serialization
- **Next.js rewrites**: Frontend routes `/api/*` to backend at `http://localhost:8080`
- **Client-side state only**: Frontend uses React `useState` and `useEffect` for data fetching, no client-side caching
- **LLM abstraction**: LlmClient interface allows swapping implementations; OllamaClient calls Ollama's OpenAI-compatible endpoint

## Layers

**REST Layer:**
- Purpose: HTTP request/response handling, request validation, status codes
- Location: `backend/src/main/kotlin/org/light/challenge/rest/`
- Contains: JAX-RS annotated resource classes
- Depends on: Service layer (injected via constructor)
- Used by: Frontend via HTTP + Next.js rewrites

**Service Layer:**
- Purpose: Business logic, state management, workflow orchestration
- Location: `backend/src/main/kotlin/org/light/challenge/service/`
- Contains: ApprovalService (workflow), CategorizationService (LLM pipeline), NotificationService (stub)
- Depends on: Repository layer, LLM layer
- Used by: REST layer

**Repository Layer:**
- Purpose: Data persistence abstraction (in this case, in-memory)
- Location: `backend/src/main/kotlin/org/light/challenge/repository/`
- Contains: PaymentRepository, ApprovalRuleRepository, ApprovalRequestRepository
- Depends on: Model layer
- Used by: Service layer

**Model Layer:**
- Purpose: Domain objects and enums
- Location: `backend/src/main/kotlin/org/light/challenge/model/`
- Contains: Payment, ApprovalRequest, ApprovalRule, enums (PaymentStatus, ApprovalStatus, Department)
- Depends on: Java/Kotlin stdlib only
- Used by: All layers

**LLM Integration Layer:**
- Purpose: Abstraction over external LLM service
- Location: `backend/src/main/kotlin/org/light/challenge/llm/`
- Contains: LlmClient interface, OllamaClient implementation
- Depends on: HTTP client (via Dropwizard), Jackson for JSON
- Used by: CategorizationService

**Frontend (React/Next.js):**
- Purpose: UI rendering, form submission, approval workflow display
- Location: `frontend/src/`
- Contains: Pages, components, styles
- Depends on: Backend REST API
- Used by: Browser

## Data Flow

### Primary Request Path: Submit Payment

1. **User fills form** → Payments page (`frontend/src/pages/index.tsx`)
2. **Click "Submit Payment"** → `handleSubmit()` calls `fetch('/api/payments', POST)` with CreatePaymentRequest
3. **Next.js rewrite** → Rewrites to `http://localhost:8080/payments`
4. **PaymentResource.createPayment()** → `backend/src/main/kotlin/org/light/challenge/rest/PaymentResource.kt:40`
5. **ApprovalService.submitPayment()** → `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt:38`
   - Creates Payment, saves to PaymentRepository
   - Calls findMatchingRules() to check against ApprovalRuleRepository
   - If no rules match: auto-approve, set status to APPROVED
   - If rules match: create ApprovalRequest for each matching approver, log notification
6. **Response** → Returns Payment + list of ApprovalRequest objects
7. **Frontend** → Displays outcome modal (auto-approved or sent for approval), updates payments list

### Approval Decision Path

1. **User clicks "Approve"/"Reject"** on Approvals page (`frontend/src/pages/approvals.tsx:44`)
2. **Fetch to `/api/approvals/{id}/decide`** → POST with decision (APPROVED/REJECTED) + optional comment
3. **ApprovalResource.processDecision()** → Updates ApprovalRequest status
4. **ApprovalService.processDecision()** → `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt:82`
   - Marks request as decided
   - Checks all requests for this payment:
     - If ANY rejected → payment REJECTED, notify submitter
     - If ALL approved → payment APPROVED, notify submitter
5. **Response** → Updated ApprovalRequest
6. **Frontend** → Refetches pending approvals, removes completed payment from list

### Categorization Path (AI Vendor/Department Suggestion)

1. **User enters description** in payment form
2. **Click "Suggest"** → `handleCategorize()` calls `fetch('/api/categorize', POST)` with description
3. **CategorizationResource.categorize()** → Calls CategorizationService
4. **CategorizationService.categorize()** → `backend/src/main/kotlin/org/light/challenge/service/CategorizationService.kt:95`
   - Stage 1: inferVendor() — LLM call with system prompt, constrained to return company name
   - Stage 2: classifyDepartment() — LLM call with vendor context, JSON schema constrained to valid Department enum
5. **Response** → CategorizationResult with vendor, department (or null if inference failed)
6. **Frontend** → Populates vendor + department fields, highlights them for 2 seconds, shows toast

**State Management:**
- Backend: Mutable maps in repositories, saved by reference (no transactions)
- Frontend: React state (useState) per page; no global store (Zustand, Redux, etc.)
- Persistence: None; all data lost on backend restart

## Key Abstractions

**ApprovalRule:**
- Purpose: Represents a single approval threshold (amount range + optional department)
- File: `backend/src/main/kotlin/org/light/challenge/model/ApprovalRule.kt`
- Pattern: Data class with nullable department (null = apply to all), minAmount/maxAmount bounds, approver details
- Matching logic in ApprovalService.findMatchingRules() filters by department and amount range

**PendingApprovalView:**
- Purpose: Enriched approval request that includes payment context (amount, description, vendor)
- File: `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt:17`
- Pattern: View object combining ApprovalRequest + Payment data, used to avoid N+1 queries in frontend
- Assembled in ApprovalService.getPendingApprovalDetails()

**CategorizationResult:**
- Purpose: Output of two-stage LLM pipeline
- File: `backend/src/main/kotlin/org/light/challenge/service/CategorizationService.kt:10`
- Pattern: Data class with optional vendor, department, description fields (nulls if inference failed)

**LlmClient Interface:**
- Purpose: Abstraction over LLM provider
- File: `backend/src/main/kotlin/org/light/challenge/llm/LlmClient.kt`
- Pattern: Single method `complete()` with systemPrompt, userMessage, temperature, responseFormat parameters
- Allows swapping OllamaClient with other implementations (e.g., OpenAI, Anthropic)

## Entry Points

**Backend:**
- Location: `backend/src/main/kotlin/org/light/challenge/App.kt:63`
- Triggers: `./gradlew run` or `java -jar`
- Responsibilities:
  1. Extends Dropwizard Application<Config>
  2. In run() method: instantiates all repositories and services, wires them together
  3. Registers REST resources (PaymentResource, ApprovalResource, CategorizationResource)
  4. Configures Jackson ObjectMapper for Kotlin + Java Time serialization
  5. Registers health check
  6. Starts embedded Jetty server on port 8080

**Frontend:**
- Location: `frontend/src/pages/_app.tsx`
- Triggers: `npm run dev`
- Responsibilities:
  1. Next.js App wrapper for all pages
  2. Loads global CSS and font
  3. Renders page component with props

**Frontend Pages:**
- Payments page: `frontend/src/pages/index.tsx` — entry point, form, table
- Approvals page: `frontend/src/pages/approvals.tsx` — pending approval cards
- Payment detail page: `frontend/src/pages/payments/[id].tsx` — single payment + its approvals

## Architectural Constraints

- **Threading:** Dropwizard default: thread pool for HTTP requests, single-threaded Jetty per request
- **Global state:** In-memory repositories are module-level singletons (mutable maps) created once in App.kt
- **Circular imports:** None detected; layering is clear (REST → Service → Repository → Model)
- **In-memory storage:** All data resets on backend restart; no persistence mechanism
- **Frontend state:** Isolated per page; no shared application state, no service worker/offline support
- **API communication:** Synchronous HTTP only; no WebSockets, no polling for approval notifications
- **LLM calls:** Synchronous; blocking REST endpoint (single LLM call can stall request)

## Anti-Patterns

### Blocking LLM Calls

**What happens:** CategorizationResource.categorize() blocks HTTP request thread while waiting for Ollama response (typically 1-10 seconds).

**Why it's wrong:** If LLM is slow or hangs, the frontend request times out; under load, thread pool exhausts and subsequent requests queue indefinitely.

**Do this instead:** Async processing — return 202 Accepted, queue categorization job, have frontend poll `/api/categorize/{taskId}` or use Server-Sent Events for callback. See `backend/src/main/kotlin/org/light/challenge/rest/CategorizationResource.kt`.

### No Validation at Repository Boundary

**What happens:** ApprovalService.submitPayment() calls repository.save() without checking if the payment already exists or if ID is duplicated.

**Why it's wrong:** In a real system with concurrent requests, race conditions can silently overwrite payments.

**Do this instead:** Repositories should guard against overwrites (throw if ID exists), or use database constraints (UNIQUE indexes). See `backend/src/main/kotlin/org/light/challenge/repository/PaymentRepository.kt:97`.

### Notification Service is a Stub

**What happens:** NotificationService.notifyApprover() and notifyPaymentApproved() just log to console; no actual email sent.

**Why it's wrong:** Approvers never see their approval requests; submitters never learn outcomes. Not a concern for this interview exercise, but blocks real-world use.

**Do this instead:** Integrate with email provider (SendGrid, AWS SES) or webhook service; implement retry logic for delivery failures. See `backend/src/main/kotlin/org/light/challenge/service/NotificationService.kt`.

### No Input Validation on REST Endpoints

**What happens:** PaymentResource.createPayment() does not validate that amount > 0, description is non-empty, etc.

**Why it's wrong:** Invalid data can be persisted; approvals can match on nonsense amounts.

**Do this instead:** Add javax.validation annotations (@NotNull, @Min, @Size) to CreatePaymentRequest, enable validation in Dropwizard setup. See `backend/src/main/kotlin/org/light/challenge/model/Payment.kt:35`.

## Error Handling

**Strategy:** Checked exceptions in repository/service layers; catch-all 500 responses in REST endpoints.

**Patterns:**
- ApprovalService.processDecision() throws IllegalArgumentException (request not found) or IllegalStateException (already decided)
- PaymentResource wraps exceptions in Response.status(404/500) with error JSON
- CategorizationService catches parsing errors on LLM response and logs; returns null Department on failure
- Frontend catches fetch errors, displays error message in UI, allows user to retry

**Gap:** No centralized error handler (ExceptionMapper in Dropwizard); each endpoint handles its own exceptions.

## Cross-Cutting Concerns

**Logging:** 
- Backend: KotlinLogging (SLF4J) via `mu.KotlinLogging.logger {}` in all services; logs payment events (submitted, approved, rejected)
- Frontend: None; errors shown as toast notifications or console

**Validation:** 
- Backend: None (anti-pattern noted above); model classes use data classes, rely on type safety
- Frontend: Required fields enforced by HTML input attributes; no client-side schema validation

**Authentication:** 
- None; no user login, no access control
- Frontend submits hardcoded `submittedBy: 'user@light.inc'`
- Approvals page assumes anyone can approve any payment

---

*Architecture analysis: 2026-08-28*
