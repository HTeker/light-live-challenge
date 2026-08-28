# Codebase Structure

**Analysis Date:** 2026-08-28

## Directory Layout

```
light-live-challenge/                          # Monorepo root
├── backend/                                   # Kotlin/Dropwizard backend
│   ├── build.gradle.kts                       # Gradle build config
│   ├── src/
│   │   ├── main/kotlin/org/light/challenge/
│   │   │   ├── App.kt                         # Dropwizard entry point
│   │   │   ├── model/                         # Domain objects
│   │   │   │   ├── Payment.kt                 # Payment, PaymentStatus, Department enums
│   │   │   │   ├── ApprovalRequest.kt         # ApprovalRequest, ApprovalStatus
│   │   │   │   └── ApprovalRule.kt            # ApprovalRule
│   │   │   ├── repository/                    # In-memory data access
│   │   │   │   ├── PaymentRepository.kt       # Payment CRUD + seed
│   │   │   │   ├── ApprovalRuleRepository.kt  # Rule set (with seed)
│   │   │   │   └── ApprovalRequestRepository.kt # Approval CRUD
│   │   │   ├── service/                       # Business logic
│   │   │   │   ├── ApprovalService.kt         # Workflow, rule matching
│   │   │   │   ├── CategorizationService.kt   # LLM pipeline (vendor + dept)
│   │   │   │   └── NotificationService.kt     # Stub for notifications
│   │   │   ├── llm/                           # LLM abstraction
│   │   │   │   ├── LlmClient.kt               # Interface
│   │   │   │   └── OllamaClient.kt            # Ollama implementation
│   │   │   └── rest/                          # HTTP endpoints
│   │   │       ├── PaymentResource.kt         # GET/POST /payments
│   │   │       ├── ApprovalResource.kt        # GET/POST /approvals
│   │   │       └── CategorizationResource.kt  # POST /categorize
│   │   └── test/kotlin/org/light/challenge/   # Tests (JUnit 5)
│   │       └── service/                       # Service layer tests
│   ├── gradle/                                # Gradle wrapper
│   └── buildSrc/                              # Gradle plugin config
│
├── frontend/                                  # Next.js React frontend
│   ├── package.json                           # npm dependencies
│   ├── tsconfig.json                          # TypeScript config
│   ├── next.config.js                         # Next.js rewrites (/api -> :8080)
│   ├── src/
│   │   ├── pages/                             # Next.js pages (auto-routed)
│   │   │   ├── _app.tsx                       # App wrapper
│   │   │   ├── _document.tsx                  # Document wrapper
│   │   │   ├── index.tsx                      # /payments (list + submit form)
│   │   │   ├── approvals.tsx                  # /approvals (pending approvals)
│   │   │   └── payments/
│   │   │       └── [id].tsx                   # /payments/:id (detail + approvals)
│   │   ├── components/                        # Reusable components
│   │   │   ├── Layout.tsx                     # Navigation wrapper
│   │   │   └── StatusBadge.tsx                # Payment status renderer
│   │   └── styles/
│   │       └── globals.css                    # Tailwind + global styles
│   └── public/                                # Static assets (if any)
│
├── .git/                                      # Git repository
├── .planning/                                 # Documentation (generated)
│   └── codebase/                              # This file + ARCHITECTURE.md
├── README.md                                  # Project overview
├── docker-compose.yml                         # Ollama service definition
└── setup.sh                                   # Script to start Ollama
```

## Directory Purposes

**backend/:**
- Core business logic: payment workflow, approval rules, LLM integration
- Framework: Dropwizard (microframework, embedded Jetty, Jackson)
- Runtime: JVM (Java 11+), Kotlin 1.7.10
- Port: 8080

**backend/src/main/kotlin/org/light/challenge/:**
- Root package for all backend code
- Entry point: App.kt (bootstraps Dropwizard, wires dependencies)

**backend/src/main/kotlin/org/light/challenge/model/:**
- Domain objects: Payment, ApprovalRequest, ApprovalRule
- Enums: PaymentStatus (PENDING_APPROVAL, APPROVED, REJECTED), ApprovalStatus (PENDING, APPROVED, REJECTED), Department (ENGINEERING, MARKETING, FINANCE, etc.)
- Request DTOs: CreatePaymentRequest, ApprovalDecisionRequest
- All immutable data classes

**backend/src/main/kotlin/org/light/challenge/repository/:**
- Data persistence layer (in-memory maps in this project)
- No ORM, no database; all data resets on restart
- Repositories initialized with seed data in init blocks
- CRUD methods: findAll(), findById(), save(), findByStatus(), findByPaymentId()

**backend/src/main/kotlin/org/light/challenge/service/:**
- Business logic tier
- ApprovalService: payment submission workflow, rule matching, approval state transitions
- CategorizationService: two-stage LLM pipeline (vendor inference + department classification)
- NotificationService: stub for approval/payment notifications

**backend/src/main/kotlin/org/light/challenge/llm/:**
- LLM abstraction layer
- LlmClient: interface defining complete(systemPrompt, userMessage, temperature, responseFormat)
- OllamaClient: implementation calling Ollama's OpenAI-compatible /v1/chat/completions endpoint

**backend/src/main/kotlin/org/light/challenge/rest/:**
- HTTP endpoint handlers (JAX-RS)
- PaymentResource: @Path("/payments") — GET all, GET by id, POST new
- ApprovalResource: @Path("/approvals") — GET pending, POST decision
- CategorizationResource: @Path("/categorize") — POST for vendor/dept suggestion

**backend/src/test/kotlin/:**
- Unit and integration tests
- Uses JUnit 5 + MockK for mocking

**frontend/:**
- User-facing web application
- Framework: Next.js 13.3, React 18.2, TypeScript 5.0, Tailwind CSS 3.3
- Port: 3000 (dev mode)
- API proxy: /api/* → http://localhost:8080/*

**frontend/src/pages/:**
- Next.js file-based routing
- index.tsx → /
- approvals.tsx → /approvals
- payments/[id].tsx → /payments/:id

**frontend/src/components/:**
- Reusable React components
- Layout: Navigation header, main container
- StatusBadge: Color-coded payment status display

**frontend/src/styles/:**
- Global CSS and Tailwind directives
- Per-page styles inline in component files (style jsx)

## Key File Locations

**Entry Points:**
- Backend: `backend/src/main/kotlin/org/light/challenge/App.kt` — Dropwizard application bootstrap
- Frontend: `frontend/src/pages/_app.tsx` — Next.js root, `frontend/src/pages/index.tsx` — initial page

**Configuration:**
- Backend: `backend/build.gradle.kts` — dependencies, build tasks, JVM target
- Frontend: `frontend/next.config.js` — API rewrites to backend, `frontend/tsconfig.json` — TypeScript config
- LLM: `setup.sh` — script to start Ollama, `docker-compose.yml` — service definition

**Core Logic:**
- Approval workflow: `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt`
- Rule matching: `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt:76`
- LLM pipeline: `backend/src/main/kotlin/org/light/challenge/service/CategorizationService.kt`
- Payment form: `frontend/src/pages/index.tsx:109`
- Approval UI: `frontend/src/pages/approvals.tsx:18`

**Testing:**
- Backend: `backend/src/test/kotlin/org/light/challenge/service/ApprovalServiceTest.kt`
- Frontend: No test files present; could add Jest + React Testing Library

## Naming Conventions

**Files:**
- Backend: PascalCase, one class per file
  - Classes: `PaymentResource.kt`, `ApprovalService.kt`
  - Examples: `Payment.kt`, `ApprovalRule.kt`
- Frontend: PascalCase for components, lowercase for pages/utilities
  - Components: `Layout.tsx`, `StatusBadge.tsx`
  - Pages: `index.tsx`, `approvals.tsx`, `[id].tsx`
  - Utilities: `globals.css`

**Directories:**
- Backend: lowercase, domain-oriented
  - `model/` (domain objects)
  - `repository/` (data access)
  - `service/` (business logic)
  - `rest/` (HTTP endpoints)
  - `llm/` (external integrations)
- Frontend: lowercase, feature-oriented
  - `pages/` (routable pages)
  - `components/` (reusable UI)
  - `styles/` (CSS)

**Functions/Methods:**
- Backend: camelCase, verb-noun pattern
  - `submitPayment()`, `findMatchingRules()`, `processDecision()`
  - `inferVendor()`, `classifyDepartment()`
- Frontend: camelCase, verb-noun pattern
  - `fetchPayments()`, `handleSubmit()`, `handleDecision()`
  - `formatDepartment()`, `showToast()`

**Variables/Constants:**
- Backend: camelCase for locals, UPPER_SNAKE_CASE for constants
  - `private val logger = KotlinLogging.logger {}`
  - `companion object { private val VENDOR_SYSTEM_PROMPT = "..." }`
- Frontend: camelCase for locals, uppercase for component names
  - `const [payments, setPayments] = useState<Payment[]>([])`
  - `const departments = ['ENGINEERING', 'MARKETING', ...]`

**API Endpoints:**
- RESTful: `/payments` (list/create), `/payments/{id}` (detail)
- Stateful actions: `/approvals/{id}/decide` (POST decision)
- Special: `/approvals/pending` (GET filtered), `/categorize` (POST for suggestion)

**Types/Enums:**
- PascalCase: `Payment`, `ApprovalRequest`, `PaymentStatus`
- Enum values: UPPER_SNAKE_CASE: `PENDING_APPROVAL`, `APPROVED`, `REJECTED`

## Where to Add New Code

**New Feature (e.g., payment search, approval history):**
- Backend API endpoint: `backend/src/main/kotlin/org/light/challenge/rest/PaymentResource.kt` (add @GET method) or new Resource class
- Business logic: `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt` (add method)
- Data access: `backend/src/main/kotlin/org/light/challenge/repository/PaymentRepository.kt` (add query method)
- Frontend page: `frontend/src/pages/search.tsx` or new route
- Frontend component: `frontend/src/components/SearchForm.tsx` if needed

**New Component/Module (e.g., notification integration, new LLM provider):**
- Implementation: Create directory `backend/src/main/kotlin/org/light/challenge/{new-feature}/`
- Interface: Define protocol/trait in module root or dedicated file
- Implementation: Create implementation class
- Wire in App.kt: Instantiate in run() method, inject into services
- Example: Email notification → `backend/src/main/kotlin/org/light/challenge/notification/EmailNotifier.kt`, implement NotificationProvider interface

**Utilities/Helpers:**
- Shared backend helpers: `backend/src/main/kotlin/org/light/challenge/util/` (create if needed)
- Shared frontend helpers: `frontend/src/lib/` or `frontend/src/utils/` (create if needed)
- Example: Date formatting → `frontend/src/lib/format.ts`

**Tests:**
- Backend unit tests: `backend/src/test/kotlin/org/light/challenge/{layer}/*Test.kt`
  - Mirror package structure: test for `service/ApprovalService.kt` lives in `test/kotlin/org/light/challenge/service/ApprovalServiceTest.kt`
- Frontend tests: `frontend/src/**/*.test.tsx` (co-located with component)
  - Example: `frontend/src/components/StatusBadge.test.tsx`

## Special Directories

**backend/gradle/:**
- Gradle wrapper files (gradle-wrapper.jar, gradle-wrapper.properties)
- Generated; committed to repo; used to ensure consistent Gradle version

**backend/buildSrc/:**
- Gradle plugin source code; build script configuration
- Defined in `buildSrc/src/main/kotlin/`; contains version constants and library definitions

**frontend/.next/:**
- Next.js build output; auto-generated during build
- Contents: compiled JavaScript, static assets, server routes
- Not committed to Git (should be in .gitignore)

**frontend/public/:**
- Static assets (if any exist; currently minimal)
- Served as-is by Next.js at root path

**.planning/codebase/:**
- Documentation generated by gsd-map-codebase
- Contains ARCHITECTURE.md, STRUCTURE.md, CONVENTIONS.md, TESTING.md, etc.
- Committed to Git; used by gsd-plan-phase and gsd-execute-phase

---

*Structure analysis: 2026-08-28*
