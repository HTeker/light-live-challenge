# External Integrations

**Analysis Date:** 2026-08-28

## APIs & External Services

**Large Language Models:**
- Ollama (Local) - AI-powered payment categorization using `qwen2.5:3b` model
  - SDK/Client: Native Java HTTP client (`backend/src/main/kotlin/org/light/challenge/llm/OllamaClient.kt`)
  - Endpoint: `http://localhost:11434/v1/chat/completions` (OpenAI-compatible API)
  - Port: 11434 (default)
  - Model: qwen2.5:3b (pulled via `setup.sh`)
  - Integration: Categorization service uses Ollama to suggest vendor and department from payment descriptions

**Frontend-to-Backend:**
- REST API via HTTP proxy
  - Proxying: Next.js rewrites all `/api/*` requests to backend at `http://localhost:8080` (`frontend/next.config.js`)
  - No authentication mechanism - requests flow directly from frontend to backend

## Data Storage

**Databases:**
- None - Application uses in-memory data storage
  - In-memory repositories: `PaymentRepository`, `ApprovalRuleRepository`, `ApprovalRequestRepository`
  - Location: `backend/src/main/kotlin/org/light/challenge/repository/`
  - Persistence: All data resets on backend restart
  - Seed data: Hard-coded sample payments and approval rules in `PaymentRepository.kt`

**File Storage:**
- Not used - No file upload or file system access

**Caching:**
- In-memory Java collections (HashMap) - Used for all data storage in repositories
- No external caching service (Redis, Memcached) deployed

## Authentication & Identity

**Auth Provider:**
- Custom/Hardcoded - No external auth provider
  - Implementation: Hardcoded user email (`user@light.inc` in frontend form submission)
  - Location: `frontend/src/pages/index.tsx` line 124 - sets `submittedBy: 'user@light.inc'` directly
  - No JWT, OAuth, or API key validation between frontend and backend
  - No session management

## Monitoring & Observability

**Error Tracking:**
- None - No external error tracking service (Sentry, Rollbar, etc.)

**Logs:**
- File/Console output only - Kotlin logging via `kotlin-logging` library
  - Framework: kotlin-logging 1.7.8 with SLF4J as backend
  - Location: Logs appear in terminal/console where application runs
  - No centralized logging service

**Health Checks:**
- Built-in Dropwizard health check endpoint
  - Location: `backend/src/main/kotlin/org/light/challenge/App.kt` lines 46-50
  - Endpoint: `/admin/health` (port 8081)
  - Status: Always returns healthy (no dynamic checks)

## CI/CD & Deployment

**Hosting:**
- Local development only - Not deployed to production infrastructure
- Expected environments:
  - Development: localhost with Gradle, npm, and Docker running locally
  - Ollama container: Docker containerized via `docker-compose.yml`

**CI Pipeline:**
- None detected - No GitHub Actions, GitLab CI, Jenkins, or other CI service configured

## Environment Configuration

**Required env vars:**
- None - Application does not read environment variables
- All configuration is hardcoded or YAML-based

**Configuration Files:**
- Backend server config: `backend/config.yml` - Specifies HTTP ports (8080, 8081)
- Frontend API proxy: `frontend/next.config.js` - Hardcoded proxy destination to `http://localhost:8080`
- Ollama defaults: Hardcoded in `OllamaClient.kt` - `baseUrl: "http://localhost:11434"`, `model: "qwen2.5:3b"`

**Secrets location:**
- No secrets management - No env files, credential stores, or secret managers detected
- No API keys, credentials, or sensitive data identified in codebase

## Webhooks & Callbacks

**Incoming:**
- None - Application does not receive webhooks from external services

**Outgoing:**
- None - Application does not send webhooks to external services

## Internal Service Communication

**REST API Endpoints:**

| Method | Path                    | Service        | Purpose |
|--------|-------------------------|----------------|---------|
| GET    | /payments               | PaymentResource | Fetch all payments |
| GET    | /payments/{id}          | PaymentResource | Get payment with approval details |
| POST   | /payments               | PaymentResource | Submit new payment (triggers approval matching) |
| GET    | /approvals/pending      | ApprovalResource | List pending approval requests |
| POST   | /approvals/{id}/decide  | ApprovalResource | Approve or reject approval request |
| POST   | /categorize             | CategorizationResource | AI-powered vendor/department suggestion |

**Backend Services:**
- `ApprovalService` - Core business logic for matching payments against approval rules
  - Location: `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt`
  - Repositories: Payment, ApprovalRule, ApprovalRequest
  - Dependencies: NotificationService
  
- `CategorizationService` - LLM-powered payment categorization
  - Location: `backend/src/main/kotlin/org/light/challenge/service/CategorizationService.kt`
  - LLM Client: OllamaClient (calls local Ollama endpoint)

- `NotificationService` - Notification/event emission (minimal implementation)
  - Location: `backend/src/main/kotlin/org/light/challenge/service/NotificationService.kt`

---

*Integration audit: 2026-08-28*
