# Codebase Concerns

**Analysis Date:** 2026-08-28

## Data Persistence & Storage

**In-memory storage with data loss on restart:**
- Issue: All payment and approval data is stored in mutable maps in `PaymentRepository`, `ApprovalRequestRepository`, and `ApprovalRuleRepository`. Data resets to seed values on every backend restart.
- Files: `backend/src/main/kotlin/org/light/challenge/repository/*.kt`
- Impact: Development/demo only. Cannot be used for production. Users lose all submissions and approval history on any deployment or crash.
- Fix approach: Migrate to persistent storage (database). Replace in-memory repositories with JPA/exposed ORM backed by PostgreSQL or similar. Keep seed data as initial migration.

## Authentication & Authorization

**No authentication or authorization checks:**
- Issue: Any user can approve/reject any payment without verification. The `/approvals/{id}/decide` endpoint doesn't validate who the approver is or if they have permission.
- Files: `backend/src/main/kotlin/org/light/challenge/rest/ApprovalResource.kt`
- Impact: Critical security risk. Approvals can be spoofed by anyone with network access. No audit trail of who actually made decisions.
- Recommendations:
  - Add user authentication (JWT, OAuth, etc.) to all endpoints
  - Add role-based authorization (only assigned approvers can approve)
  - Log all approval decisions with authenticated user identity

**Hardcoded submitter in frontend:**
- Issue: Frontend always submits payments as `'user@light.inc'` instead of using actual user identity.
- Files: `frontend/src/pages/index.tsx:124`
- Impact: Cannot track who actually submitted payments. Notification system can't reach real submitters.
- Fix approach: Implement user context (from auth provider) and use actual user email for submissions.

## LLM Integration Fragility

**Ollama service dependency without graceful degradation:**
- Issue: The categorization feature requires Ollama to be running on `localhost:11434`. If Ollama is down or slow, requests hang or timeout.
- Files: `backend/src/main/kotlin/org/light/challenge/llm/OllamaClient.kt`
- Impact: Users cannot submit payments while LLM is unavailable. CategorizationResource catches exceptions and returns 503, which is correct, but underlying service failures are not clearly distinguishable.
- Fix approach:
  - Add health check endpoint for Ollama service
  - Log specific failure reasons (connection refused vs. timeout)
  - Consider circuit breaker pattern for repeated failures

**LLM response parsing silently fails:**
- Issue: In `CategorizationService.classifyDepartment`, if the LLM response JSON is malformed, parsing fails with generic Exception catch and returns null. No indication to user of what went wrong.
- Files: `backend/src/main/kotlin/org/light/challenge/service/CategorizationService.kt:127-133`
- Impact: Silent failures mask real issues (bad LLM output, API changes). Users see "no suggestions available" but can't debug.
- Fix approach:
  - Log the raw LLM response when parsing fails
  - Add metrics to track parse failure rate
  - Consider adding validation of LLM output against schema before parsing

**Vendor inference can return "none":**
- Issue: In `CategorizationService.inferVendor`, if the LLM responds with "none"/"null"/"unknown", the field is set to null. However, the frontend requires a vendor, so forms are left incomplete.
- Files: `backend/src/main/kotlin/org/light/challenge/service/CategorizationService.kt:105-113` and `frontend/src/pages/index.tsx:264`
- Impact: AI suggestions can partially fail, leaving vendor field blank. Users must fill it in manually anyway, reducing value of the feature.
- Fix approach: Return explicit "Unknown Vendor" or prompt user to provide vendor when LLM can't infer it.

## Approval Workflow Issues

**Race condition in approval decision processing:**
- Issue: In `ApprovalService.processDecision`, there's a time window between checking `approvalRequest.status != ApprovalStatus.PENDING` and updating it. If two requests arrive simultaneously, both could pass the check.
- Files: `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt:86-96`
- Impact: Rare but possible race condition where an approval could be double-decided. In-memory storage masks this, but it would be critical with concurrent access.
- Fix approach: Use database-level constraints and optimistic locking (version field) or pessimistic locking on approval requests.

**Approval status enum mismatch between backend and frontend:**
- Issue: Backend uses `PaymentStatus` and `ApprovalStatus` enums, but there's no validation that incoming JSON matches these exactly. Jackson will throw on unknown values.
- Files: `backend/src/main/kotlin/org/light/challenge/model/*.kt`
- Impact: If frontend sends wrong status values, API returns 400 without clear error message about which values are valid.
- Fix approach: Add validation error details listing accepted enum values in error response.

## Input Validation

**Missing validation on payment submission:**
- Issue: `CreatePaymentRequest` and payment endpoints don't validate:
  - Amount must be positive and non-zero
  - Description and vendor must be non-empty
  - Department must be a valid enum value
  - Currency format
  - Email format for `submittedBy`
- Files: `backend/src/main/kotlin/org/light/challenge/model/Payment.kt` and `backend/src/main/kotlin/org/light/challenge/rest/PaymentResource.kt`
- Impact: Invalid data can enter the system (negative amounts, empty descriptions). Frontend has some validation but backend doesn't enforce it.
- Fix approach: Add JSR-380 Bean Validation annotations to request models and enable validation with @Valid on endpoints.

**Frontend validation is incomplete:**
- Issue: Frontend requires fields but doesn't validate data types, ranges, or formats before sending.
- Files: `frontend/src/pages/index.tsx:109-160`
- Impact: Malformed data from network proxy or MITM could reach backend without validation.
- Fix approach: Add client-side validation for amount (positive, max decimal places), email format validation on submitter field.

## Error Handling & UX

**Poor rejection workflow in approvals:**
- Issue: Frontend uses browser `prompt()` dialog for rejection reason, which is poor UX and doesn't persist if user cancels.
- Files: `frontend/src/pages/approvals.tsx:47-49` and `frontend/src/pages/payments/[id].tsx:63`
- Impact: Clunky user experience. Users may accidentally cancel. No clear guidance on what reason to provide.
- Fix approach: Replace `prompt()` with a modal form component that validates and provides guidance.

**Generic error messages don't aid debugging:**
- Issue: Error responses are generic ("Failed to submit payment", "Categorization service unavailable"). No actionable details.
- Files: Multiple REST endpoints return generic error maps
- Impact: Users can't self-diagnose issues. Support/debugging is harder.
- Fix approach: Include more specific error details (validation failures list values, LLM timeout vs. network error, etc.).

## Test Coverage

**Limited test coverage:**
- Issue: Only `ApprovalService` has tests. No tests for:
  - REST endpoints (PaymentResource, ApprovalResource, CategorizationResource)
  - CategorizationService
  - NotificationService
  - LLM integration
  - Frontend components and page logic
- Files: `backend/src/test/kotlin/org/light/challenge/service/ApprovalServiceTest.kt` (only file with tests)
- Impact: Regressions in service layer, API contracts, and UI logic are undetected. High risk for breaking changes during refactoring.
- Priority: High - This blocks safe refactoring and feature additions.
- Fix approach:
  - Add integration tests for REST endpoints with MockMvc or REST-Assured
  - Add unit tests for CategorizationService with mocked LlmClient
  - Add contract tests for JSON serialization/deserialization
  - Add frontend component tests with React Testing Library

## Configuration & Environment

**Hardcoded service URLs and ports:**
- Issue: Ollama URL is hardcoded as `http://localhost:11434`. Backend port is 8080, frontend dev server is 3000. No environment-based configuration.
- Files: `backend/src/main/kotlin/org/light/challenge/llm/OllamaClient.kt:11-12` and `frontend/next.config.js:8`
- Impact: Cannot easily run in different environments (staging, production, Docker). Requires code changes for deployment.
- Fix approach: Move URLs to environment variables or config files. Use ConfigMap/Secrets for containerized deployments.

**No CORS configuration for production:**
- Issue: Frontend rewrites API calls in dev mode via Next.js `rewrites()`, which masks CORS issues. Production deployment would face cross-origin errors.
- Files: `frontend/next.config.js:4-10`
- Impact: Frontend will fail when deployed separately from backend.
- Fix approach: Add CORS headers to backend (Dropwizard CORSBundle or JAX-RS filters) with appropriate origin whitelist.

## Scalability & Performance Concerns

**No caching or pagination on payment lists:**
- Issue: `/payments` endpoint returns all payments without limit. As data grows, response size and memory usage will grow unbounded.
- Files: `backend/src/main/kotlin/org/light/challenge/rest/PaymentResource.kt:15-28`
- Impact: Performance degrades as more payments accumulate. Frontend loads entire list into memory.
- Fix approach: Add pagination (limit/offset or cursor), add client-side virtualization for large lists, add caching headers.

**LLM inference adds latency to payment submission:**
- Issue: Categorization is optional and returns quickly via catch handler, but if enabled, each suggestion call blocks for up to 30 seconds.
- Files: `backend/src/main/kotlin/org/light/challenge/llm/OllamaClient.kt:20`
- Impact: Users see delays during categorization. Multiple concurrent calls could saturate the local model.
- Fix approach: Consider async/queue-based categorization or background enrichment after submission.

## Missing Features for Production

**No audit trail:**
- Issue: Payments and approvals lack audit history. No way to see who changed what, when, or why.
- Files: All repositories
- Impact: Cannot debug issues, track compliance, or investigate suspicious activity.
- Fix approach: Add CreatedBy, UpdatedBy, UpdatedAt fields to Payment and ApprovalRequest. Consider event sourcing for full history.

**Notifications are only logged:**
- Issue: `NotificationService` logs notifications but doesn't send them anywhere (email, Slack, etc.).
- Files: `backend/src/main/kotlin/org/light/challenge/service/NotificationService.kt`
- Impact: Users don't actually get notified about pending approvals or decisions.
- Fix approach: Integrate with email service or Slack API. Add notification preferences/channels.

**No bulk operations or filtering:**
- Issue: Frontend can't filter payments by status, date range, vendor, etc. No bulk approve/reject.
- Files: Frontend pages and backend endpoints
- Impact: Hard to manage many payments. UI becomes unusable as data grows.
- Fix approach: Add filter parameters to API, implement filtering UI, consider bulk operation endpoints.

## Frontend Dependencies & Outdated Packages

**Slightly older Next.js and React versions:**
- Issue: Next.js 13.3.0, React 18.2.0. Not latest but not old enough to be critical.
- Files: `frontend/package.json`
- Impact: Minor. May miss security patches and new features.
- Fix approach: Regular dependency updates, monitor for security advisories.

---

*Concerns audit: 2026-08-28*
