# Testing Patterns

**Analysis Date:** 2026-08-28

## Test Framework Overview

### Backend (Kotlin)

**Test Runner:**
- JUnit Jupiter (JUnit 5) 5.8.1
- Gradle test task configured to use JUnitPlatform
- Config: `build.gradle.kts` contains `tasks.withType<Test> { useJUnitPlatform() }`

**Mocking Framework:**
- MockK 1.9.3 (defined in `backend/buildSrc/src/main/kotlin/Libraries.kt`)
- Used for mocking service dependencies

**Assertion Library:**
- JUnit Jupiter built-in assertions (`org.junit.jupiter.api.Assertions.*`)

**Run Commands:**
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ApprovalServiceTest

# Run with output
./gradlew test --info
```

### Frontend (TypeScript/React)

**Test Framework:**
- NOT CONFIGURED - No testing framework installed
- No test files found in `frontend/src/`
- No test dependencies in `frontend/package.json`

**Note:** Frontend tests would need Jest or Vitest setup. Currently, the project has no frontend unit tests.

## Test File Organization

### Kotlin Backend

**Location Pattern:**
- Source code: `backend/src/main/kotlin/`
- Tests: `backend/src/test/kotlin/`
- Mirror package structure: test class mirrors source package path

**Example:**
- Source: `backend/src/main/kotlin/org/light/challenge/service/ApprovalService.kt`
- Test: `backend/src/test/kotlin/org/light/challenge/service/ApprovalServiceTest.kt`

**File Naming:**
- Test files: `[ClassName]Test.kt`
- All test classes live in `src/test/` directory
- Current state: Only `ApprovalServiceTest.kt` exists (single test file in repo)

## Test Structure & Patterns

### Kotlin Test Suite Structure

**Typical Pattern** (from `ApprovalServiceTest.kt`):

```kotlin
/**
 * Tests for how a submitted payment is routed to approvers.
 *
 * These double as documentation of the rule-matching behaviour: rules stack, so a single
 * payment can require several approvers, and it is only approved once they all sign off.
 */
class ApprovalServiceTest {

    private lateinit var service: ApprovalService

    @BeforeEach
    fun setUp() {
        service = ApprovalService(
            PaymentRepository(),
            ApprovalRuleRepository(),
            ApprovalRequestRepository(),
            NotificationService()
        )
    }

    @Test
    fun `payments under 1000 are auto-approved with no approvers`() {
        val payment = service.submitPayment(request("500.00", Department.OPERATIONS))
        assertEquals(PaymentStatus.APPROVED, payment.status)
        assertTrue(service.getApprovalsForPayment(payment.id).isEmpty())
    }
}
```

**Key Characteristics:**
- Class-level documentation explaining test focus
- Private mutable service instance: `private lateinit var service: ApprovalService`
- `@BeforeEach` setup method constructing dependencies
- Backtick-quoted test names describing behavior in plain language
- Assertions at end of test method
- No teardown (in-memory data structures clean automatically)

### Assertions Pattern

**JUnit Jupiter Assertions:**
```kotlin
import org.junit.jupiter.api.Assertions.*

assertEquals(PaymentStatus.APPROVED, payment.status)
assertTrue(service.getApprovalsForPayment(payment.id).isEmpty())
assertEquals(setOf("Team Lead"), approversFor("5000.00", Department.FINANCE))
```

**Pattern:**
- Static import of assertion functions for brevity
- Actual value first, expected value second: `assertEquals(actual, expected)`
- Grouped assertions in single test method (one scenario, multiple assertions on result)

## Test Data & Fixtures

### Kotlin Test Data

**Pattern: Helper Functions**

From `ApprovalServiceTest.kt`:
```kotlin
private fun request(amount: String, department: Department) = CreatePaymentRequest(
    amount = BigDecimal(amount),
    currency = "USD",
    department = department,
    vendor = "Test Vendor",
    description = "Test payment",
    submittedBy = "tester@light.inc"
)

private fun approversFor(amount: String, department: Department): Set<String> {
    val payment = service.submitPayment(request(amount, department))
    return service.getApprovalsForPayment(payment.id).map { it.approverName }.toSet()
}
```

**Characteristics:**
- Private helper functions for creating test data
- Extracting common data construction (CreatePaymentRequest factory)
- Higher-level abstractions for domain logic (approversFor extracts what approvers match)
- No separate fixtures file; inline in test class

**Seed Data:**
- In-memory repositories use `init` block with seed data
- Example: `PaymentRepository.kt` contains 7 seed Payment objects
- Used for manual testing and integration; tests create fresh instances per test

### Repository Initialization Pattern

**In-Memory Storage** (`PaymentRepository.kt`):
```kotlin
class PaymentRepository {
    private val payments = mutableMapOf<String, Payment>()

    init {
        val seed = listOf(
            Payment(id = "pay-001", amount = BigDecimal("450.00"), ...),
            Payment(id = "pay-002", amount = BigDecimal("8500.00"), ...),
            // ... more seed data
        )
        seed.forEach { payments[it.id] = it }
    }

    fun save(payment: Payment): Payment {
        payments[payment.id] = payment
        return payment
    }
}
```

**Test Implication:**
- Each test receives fresh repository instances (created in `@BeforeEach`)
- No test pollution from other tests
- Seed data ignored by most tests (tests create new payments via service)

## Mocking

### MockK Usage

**Framework:** MockK 1.9.3

**Current State:**
- NOT USED in existing tests
- Available as dependency but `ApprovalServiceTest` creates real repository instances instead
- Allows `mock()` and `verify()` for mocking service dependencies if needed

**Expected Pattern** (if implemented):
```kotlin
// Example pattern (not in current code):
private val mockRepository = mockk<PaymentRepository>()

@BeforeEach
fun setUp() {
    every { mockRepository.findById(any()) } returns testPayment
    service = ApprovalService(mockRepository, /* ... */)
}
```

### What to Mock vs. What NOT to Mock

**Current Testing Philosophy:**
- Use real in-memory repositories (not mocked)
  - Rationale: Tests verify end-to-end flow through service → repository
  - In-memory storage is fast and deterministic
- Dependencies that could be mocked: `NotificationService` (sends notifications)
  - Current approach: Use real instance; no side effects in tests

**What NOT to Mock:**
- Business logic services (test them with real dependencies)
- Repository implementations (use real in-memory stores)
- Models and data classes (use real instances)

**What COULD Be Mocked:**
- External services (LlmClient in `CategorizationService`)
- Notification services (if they had external side effects)
- Time-dependent operations (currently use `Instant.now()` directly in tests)

## Test Coverage

### Requirements

**Coverage Target:** Not enforced

**Current State:**
- No coverage reports configured
- Only `ApprovalServiceTest` exists; covers core approval routing logic
- Frontend: No tests, 0% coverage

### Gaps

**Major Untested Areas:**
1. **REST Resources** (`ApprovalResource.kt`, `PaymentResource.kt`, `CategorizationResource.kt`)
   - Error handling in REST layer
   - Response building and serialization
   - Path parameter parsing and validation

2. **Categorization Service** (`CategorizationService.kt`)
   - LLM client integration
   - Vendor inference logic
   - Department classification
   - Error handling in LLM parsing

3. **Frontend UI** (all pages and components)
   - User interactions (form submission, button clicks)
   - State management (form state, loading, error states)
   - Fetch error handling
   - Toast notifications

4. **Repository Implementations**
   - Filtering and querying logic
   - State mutations

## Test Types & Scopes

### Unit Tests

**Scope:** Kotlin backend only

**Current Implementation:** `ApprovalServiceTest.kt`

**What They Test:**
- Service layer business logic (payment routing, approval workflows)
- Rule matching against payment attributes (amount, department)
- Multi-approver scenarios (both approval and rejection flows)
- State transitions (PENDING → APPROVED, PENDING → REJECTED)

**What They DON'T Test:**
- Individual repository methods in isolation (tests use service layer end-to-end)
- Serialization/deserialization
- Database persistence

**Characteristics:**
- Fast (in-memory, no external dependencies)
- Deterministic (no randomness, no time-dependent logic)
- Focused on business rules and workflows

### Integration Tests

**Current State:** Not present

**What Would Be Tested:**
- REST endpoint behavior (request → response)
- Serialization via Jackson (JSON → Kotlin objects)
- Error response codes and bodies
- Service → Repository → Serialization pipeline

### End-to-End Tests

**Current State:** Not present

**Would Require:**
- Running full application (Docker)
- HTTP client to make real requests
- Browser automation (Selenium/Playwright) for frontend testing

## Test Naming Convention

### Kotlin Test Names

**Pattern:** Backtick-quoted descriptive sentences

```kotlin
@Test
fun `payments under 1000 are auto-approved with no approvers`() { ... }

@Test
fun `standard payments route to the team lead`() { ... }

@Test
fun `an engineering payment stacks the global and department rules`() { ... }

@Test
fun `a payment with multiple approvers is approved only once all sign off`() { ... }

@Test
fun `a single rejection rejects the whole payment`() { ... }
```

**Characteristics:**
- Describe behavior in plain English, not technical implementation
- Start with "when" or "that" to read like specification
- State the expected outcome clearly
- Backticks allow spaces and special characters in method names
- Serve as documentation of business rules

### Assertion Message Convention

**Pattern:** No explicit messages; rely on assertion type and test name

```kotlin
assertEquals(PaymentStatus.APPROVED, payment.status)
// Failure message: Expected APPROVED but got PENDING_APPROVAL
```

## Setup and Teardown

### Kotlin @BeforeEach Pattern

**Current Pattern:**
```kotlin
@BeforeEach
fun setUp() {
    service = ApprovalService(
        PaymentRepository(),
        ApprovalRuleRepository(),
        ApprovalRequestRepository(),
        NotificationService()
    )
}
```

**Characteristics:**
- Single `@BeforeEach` method (not teardown needed)
- Creates fresh service instance and all dependencies for each test
- No shared state between tests
- Fast (no actual setup/cleanup overhead; in-memory)

### Teardown

**Not Used:** No `@AfterEach` method needed

**Reason:** In-memory data structures garbage-collected automatically after each test

## Async Testing

**Current State:** Not used

**Frontend Pattern Observed** (when testing is added):
- Tests would likely use `async/await` with Jest/Vitest
- Fetch mocking would be required (e.g., `jest.mock('fetch')`)

## Error Testing Pattern

### Current Pattern

From `ApprovalServiceTest.kt`:
```kotlin
@Test
fun `a single rejection rejects the whole payment`() {
    val payment = service.submitPayment(request("12000.00", Department.ENGINEERING))
    val approvals = service.getApprovalsForPayment(payment.id)

    service.processDecision(
        approvals[0].id, 
        ApprovalDecisionRequest(ApprovalStatus.REJECTED, "Out of budget")
    )

    assertEquals(PaymentStatus.REJECTED, service.getPayment(payment.id)!!.status)
}
```

**Characteristics:**
- Tests success and error paths as domain scenarios (rejection is valid outcome)
- No explicit exception assertion (all paths return valid objects)
- Uses null coalescing operator `!!` where result is guaranteed non-null

### Exceptions NOT Tested

**Current Gap:** No tests for `IllegalArgumentException` or `IllegalStateException`

**Expected Pattern** (if added):
```kotlin
@Test
fun `processing a decided request throws IllegalStateException`() {
    // Setup: approvalRequest already decided
    // Then: Expect exception
    assertThrows<IllegalStateException> {
        service.processDecision(decidedRequestId, ...)
    }
}
```

## Frontend Testing (Planned)

**Not Yet Implemented**

**When Added, Follow:**
- Jest or Vitest for test runner
- React Testing Library for component testing
  - Query by role/label (not implementation details)
  - User-centric assertions (user sees success message, not state change)
- Mock fetch API calls with jest.mock or MSW (Mock Service Worker)
- Test component state and UI rendering, not internal functions

**Example Pattern** (not yet in repo):
```typescript
// jest.config.js would configure TypeScript
// jest.mock('fetch') or MSW setup for API mocking
describe('Payments page', () => {
  test('submitting a payment shows success message', async () => {
    // Render page
    // Fill form
    // Submit
    // Assert toast appears
  })
})
```

## Running Tests

### Run All Tests
```bash
cd backend
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests ApprovalServiceTest
```

### Run Specific Test Method
```bash
./gradlew test --tests ApprovalServiceTest.payments*under*1000*
```

### Watch Mode

**Not Configured** - Gradle doesn't have built-in watch mode like npm

Workaround:
```bash
./gradlew test --continuous  # Re-runs on file change (Gradle Enterprise feature)
```

Or use IDE:
- IntelliJ IDEA: Run test with Cmd+Shift+R, re-run with Cmd+R
- VS Code: Run with Gradle extension

### Coverage Report

**Not Configured**

**To Add Coverage:**
```gradle
// In build.gradle.kts
plugins {
    id("jacoco")
}

tasks.jacocoTestReport {
    // Configure report
}
```

Then run:
```bash
./gradlew jacocoTestReport
# View: build/reports/jacoco/test/html/index.html
```

## Missing Test Infrastructure

### Backend Gaps

1. **REST Resource Tests**
   - Need to mock ObjectMapper or use integration test framework
   - Test status codes, error responses

2. **Categorization Service Tests**
   - Need to mock LlmClient
   - Test department classification logic
   - Test vendor inference edge cases

3. **Repository Tests**
   - Could test find/filter logic (though simple in current in-memory implementation)

4. **Integration Tests**
   - Test end-to-end flows with all layers
   - Would require embedded Jersey server or test harness

### Frontend Gaps

1. **Test Framework Setup**
   - Install Jest or Vitest
   - Add React Testing Library
   - Add fetch mocking (MSW or jest.mock)

2. **Component Tests**
   - Test Layout, StatusBadge, form components
   - Test state changes and side effects

3. **Page Tests**
   - Test index.tsx (main Payments page)
   - Test approvals.tsx
   - Test payments/[id].tsx (detail page)

4. **Integration Tests**
   - Test form submission flow end-to-end
   - Test error handling and user feedback

---

*Testing analysis: 2026-08-28*
