# Coding Conventions

**Analysis Date:** 2026-08-28

## Language-Specific Guidelines

### Kotlin Backend

**File & Class Naming:**
- Package names: lowercase with dots, e.g., `org.light.challenge.service`
- Class names: PascalCase (data classes, regular classes, services)
- File names: Match class name exactly, e.g., `ApprovalService.kt`
- Repository files: `[Entity]Repository.kt` (e.g., `PaymentRepository.kt`)
- Service files: `[Feature]Service.kt` (e.g., `ApprovalService.kt`)
- REST resources: `[Entity]Resource.kt` (e.g., `ApprovalResource.kt`)

**Function & Variable Naming:**
- Functions: camelCase, descriptive action verbs
  - Examples: `submitPayment()`, `findMatchingRules()`, `processDecision()`, `getPendingApprovals()`
- Variables: camelCase, prefer clarity over brevity
  - Examples: `approvalRequest`, `paymentRepository`, `matchingRules`
- Private helper functions: camelCase, often at the end of class
  - Example in `CategorizationService`: `inferVendor()`, `classifyDepartment()`
- Companion object properties: UPPER_SNAKE_CASE for constants
  - Example: `VENDOR_SYSTEM_PROMPT`, `DEPARTMENT_SYSTEM_PROMPT`

**Data Classes & Models:**
- Located in `src/main/kotlin/org/light/challenge/model/`
- Use data classes for value objects: `data class Payment(...)` (see `Payment.kt`)
- Enums in UPPER_SNAKE_CASE: `PaymentStatus`, `Department`, `ApprovalStatus`
- Request/response DTOs: PascalCase with Request/Response suffix
  - Examples: `CreatePaymentRequest`, `ApprovalDecisionRequest`
- Properties use default values for IDs and timestamps:
  ```kotlin
  val id: String = UUID.randomUUID().toString()
  val createdAt: Instant = Instant.now()
  var status: PaymentStatus = PaymentStatus.PENDING_APPROVAL
  ```

**Imports:**
- Organized by origin: Kotlin stdlib → third-party libraries → project imports
- Use wildcard imports sparingly; prefer explicit imports for clarity
- Example from `ApprovalService.kt`:
  ```kotlin
  import mu.KotlinLogging
  import org.light.challenge.model.*
  import org.light.challenge.repository.*
  ```

### TypeScript/React Frontend

**File & Component Naming:**
- React components: PascalCase in `src/components/` directory
  - Examples: `Layout.tsx`, `StatusBadge.tsx`
- Page files: lowercase with brackets for dynamic routes
  - Examples: `pages/index.tsx`, `pages/approvals.tsx`, `pages/payments/[id].tsx`
- Component files: match component export name
- Type definitions: inline with `interface` keyword (see `index.tsx`)

**Function & Variable Naming:**
- React components: PascalCase function names
- Event handlers: `handle[Action]` prefix, e.g., `handleSubmit`, `handleCategorize`
- State setters: destructured from `useState`, follow pattern `const [state, setState] = useState()`
- Helper functions: camelCase, e.g., `resetForm()`, `showToast()`, `formatDepartment()`
- Internal constants: camelCase arrays and objects
  - Example: `const departments = [...]` in `index.tsx`

**Styling:**
- Tailwind CSS classes only; no CSS-in-JS or local styles
- Multi-class strings on single lines or broken with template literals
- Conditional classes use template literals:
  ```typescript
  className={`${baseClass} ${condition ? 'conditional-class' : 'alternative'}`}
  ```

**Type Definitions:**
- Use `interface` for object shapes (e.g., `interface Payment { ... }`)
- Inline types for component props:
  ```typescript
  export default function StatusBadge({ status }: { status: Status }) { ... }
  ```
- Use `type` for unions: `type Status = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'PENDING'`

## Logging

### Kotlin Logging

**Framework:** Kotlin-logging (`mu.KotlinLogging`)

**Setup:**
- Private logger per file at module level:
  ```kotlin
  private val logger = KotlinLogging.logger {}
  ```

**Patterns:**
- Use lambda-based logging for deferred evaluation:
  ```kotlin
  logger.info { "Payment ${payment.id} auto-approved (no matching rules)" }
  logger.error(e) { "Failed to parse department from LLM response: $response" }
  ```
- Log at INFO level for business events (approvals, rejections, routing)
- Log at ERROR level for exceptions with context
- Avoid excessive DEBUG logging; use selectively for investigative debugging

### Frontend Logging

**Pattern:** Use browser `console` directly

- Minimal logging in production code
- Use for debugging during development
- No structured logging framework installed

## Error Handling

### Kotlin Error Handling

**Strategy:** Explicit exception throwing with typed exceptions

**Patterns:**
- Use `IllegalArgumentException` for invalid inputs that can't be processed
  ```kotlin
  throw IllegalArgumentException("Approval request not found: $approvalRequestId")
  ```
- Use `IllegalStateException` for invalid state transitions
  ```kotlin
  throw IllegalStateException("Approval request has already been decided")
  throw IllegalStateException("Payment not found for approval request")
  ```
- In REST resources, catch specific exceptions and map to HTTP status codes:
  ```kotlin
  return try {
      val result = approvalService.processDecision(approvalRequestId, decision)
      Response.ok(result).build()
  } catch (e: IllegalArgumentException) {
      Response.status(404).entity(mapOf("error" to e.message)).build()
  } catch (e: IllegalStateException) {
      Response.status(400).entity(mapOf("error" to e.message)).build()
  }
  ```

### Frontend Error Handling

**Patterns:**
- Wrap fetch calls in try-catch-finally blocks
- Store error state in component state: `const [error, setError] = useState<string | null>(null)`
- Display errors to users via toast notifications or error containers:
  ```typescript
  if (!res.ok) {
      const data = await res.json()
      throw new Error(data.error || 'Failed to submit payment')
  }
  ```
- Use `any` type casting sparingly when casting API response arrays:
  ```typescript
  const approvers: string[] = (data.approvals || []).map((a: { approverName: string }) => a.approverName)
  ```

## Code Style & Formatting

### Kotlin Code Style

**Indentation & Line Length:**
- 4-space indentation (Kotlin standard)
- No explicit line-length limit enforced; stay readable (80-120 characters preferred)

**Null Handling:**
- Use Elvis operator `?:` for defaults:
  ```kotlin
  .filter { it.maxAmount == null || it.maxAmount > payment.amount }
  ```
- Use safe navigation for optional chaining: `.mapNotNull { ... }`
- Prefer explicit null checks to nullable types where appropriate

**Lambdas & Collections:**
- Prefer collection functions over loops:
  ```kotlin
  matchingRules.filter { it.department == null || it.department == payment.department }
  ```
- Use `forEach` for side effects; functional transforms otherwise
- One-liners for simple operations:
  ```kotlin
  fun findAll(): List<Payment> = payments.values.toList().sortedByDescending { it.createdAt }
  ```

### TypeScript Code Style

**Indentation & Line Length:**
- 2-space indentation (Next.js/React standard)
- ESLint enforces `next/core-web-vitals` rules

**Template Literals:**
- Used for conditional styling and string interpolation:
  ```typescript
  className={`block w-full border rounded-md shadow-sm py-2 px-3 text-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 transition-colors duration-500 ${highlighted.has('vendor') ? 'border-blue-400 bg-blue-50' : 'border-gray-300'}`}
  ```

**Optional Chaining & Nullish Coalescing:**
- Use optional chaining for safe property access: `data?.error`
- Use nullish coalescing for defaults: `data.error || 'Failed to submit payment'`

## Comments & Documentation

### Kotlin Documentation

**JSDoc-style Comments:**
- Use for public classes and complex business logic:
  ```kotlin
  /**
   * A pending approval request enriched with summary details of the payment it belongs to,
   * so the approvals queue can show context (amount, vendor) without a second lookup.
   */
  data class PendingApprovalView(...)
  ```

- Multi-stage algorithms document each step:
  ```kotlin
  /**
   * Categorizes a free-text payment description into a vendor and a department.
   *
   * This runs in two deterministic LLM stages rather than one combined call:
   *   1. Vendor inference  - identify (or infer from a product/brand) the company being paid.
   *   2. Department classification - classify into one of the fixed departments...
   */
  ```

**Inline Comments:**
- Explain why, not what (code shows what)
- Use sparingly; prefer self-documenting code
- Example: `// $12,000 matches Large (Finance Manager) AND Engineering Vendor (Engineering Director).`

### TypeScript/React Comments

- Minimal JSDoc; rely on type annotations and clear naming
- Inline comments explain business logic, not code structure:
  ```typescript
  // don't overwrite the user's description
  if (filled.length > 0) { ... }
  ```
- HTML comments for section markers in long files:
  ```typescript
  {/* Outcome of the last submission */}
  {outcome && ( ... )}
  ```

## Module Design

### Kotlin Services & Repositories

**Repository Pattern:**
- In-memory storage with map: `private val payments = mutableMapOf<String, Payment>()`
- Methods follow database naming: `findAll()`, `findById()`, `save()`, `findByStatus()`
- Seed data in `init` block for prototyping
- No transaction management (in-memory; would need in production)

**Service Pattern:**
- Orchestrate business logic across repositories and external services
- Constructor injection of dependencies:
  ```kotlin
  class ApprovalService(
      private val paymentRepository: PaymentRepository,
      private val approvalRuleRepository: ApprovalRuleRepository,
      ...
  )
  ```
- Public methods expose the business domain (e.g., `submitPayment()`, `processDecision()`)
- Private methods break down complex logic (e.g., `findMatchingRules()`)

### Kotlin REST Resources (Jersey)

**Annotation Pattern:**
- Class-level `@Path`, `@Produces`, `@Consumes`
- Method-level `@GET`, `@POST`, `@Path` with path parameters
- Dependency injection in constructor:
  ```kotlin
  class ApprovalResource(private val approvalService: ApprovalService)
  ```

**Response Building:**
- Return `Response` objects from all endpoints
- Use `Response.ok(data).build()` for success
- Use `Response.status(code).entity(error).build()` for errors

### TypeScript/React Page Structure

**Pattern:**
- State at top of component: form fields, loading, error, UI state
- Fetch functions for API calls
- Event handlers (form submit, button clicks)
- Conditional rendering based on state
- JSX markup at bottom

Example pattern from `pages/index.tsx`:
1. Interface definitions
2. Component function
3. State declarations (useState)
4. Effect hooks
5. Event handlers (handleSubmit, handleCategorize)
6. Render logic with conditional branches

**Barrel Files:**
- Not heavily used; imports specify exact files
- Example: `import Layout from '@/components/Layout'`

## Import Path Aliases

**Frontend (`tsconfig.json`):**
- `@/*` maps to `./src/*`
- Used to avoid relative paths: `import Layout from '@/components/Layout'`

**Backend:**
- Fully qualified package names: `org.light.challenge.service.*`
- No aliases; rely on IDE resolution

## Testing Conventions (Applied in TESTING.md)

See dedicated TESTING.md for:
- Test naming patterns (`*Test.kt`, `*Test.tsx` patterns)
- Assertion patterns (JUnit Jupiter vs. Jest/Vitest)
- Setup/teardown patterns (@BeforeEach, beforeEach)
- Mocking patterns (MockK in Kotlin, manual for TypeScript)

---

*Convention analysis: 2026-08-28*
