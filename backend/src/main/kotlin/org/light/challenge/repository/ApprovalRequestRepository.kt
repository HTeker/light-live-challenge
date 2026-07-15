package org.light.challenge.repository

import org.light.challenge.model.ApprovalRequest
import org.light.challenge.model.ApprovalStatus
import java.time.Instant

class ApprovalRequestRepository {
    private val requests = mutableMapOf<String, ApprovalRequest>()

    init {
        val seed = listOf(
            // pay-002 (AWS $8500 - Engineering) - approved by team lead
            ApprovalRequest(
                id = "req-001",
                paymentId = "pay-002",
                approverEmail = "teamlead@light.inc",
                approverName = "Team Lead",
                status = ApprovalStatus.APPROVED,
                createdAt = Instant.parse("2026-03-20T14:30:00Z"),
                decidedAt = Instant.parse("2026-03-20T15:00:00Z"),
                comment = "Approved - standard infra cost"
            ),
            // pay-003 (TechCrunch $15000 - Marketing) - pending CMO approval
            ApprovalRequest(
                id = "req-002",
                paymentId = "pay-003",
                approverEmail = "cmo@light.inc",
                approverName = "CMO",
                status = ApprovalStatus.PENDING,
                createdAt = Instant.parse("2026-04-01T09:00:00Z")
            ),
            // pay-003 also needs finance manager (large payment global rule)
            ApprovalRequest(
                id = "req-003",
                paymentId = "pay-003",
                approverEmail = "finance.manager@light.inc",
                approverName = "Finance Manager",
                status = ApprovalStatus.PENDING,
                createdAt = Instant.parse("2026-04-01T09:00:00Z")
            ),
            // pay-004 (Apple $12000 - Engineering) - pending eng director
            ApprovalRequest(
                id = "req-004",
                paymentId = "pay-004",
                approverEmail = "eng.director@light.inc",
                approverName = "Engineering Director",
                status = ApprovalStatus.PENDING,
                createdAt = Instant.parse("2026-04-02T11:00:00Z")
            ),
            // pay-004 also needs finance manager (large payment global rule)
            ApprovalRequest(
                id = "req-005",
                paymentId = "pay-004",
                approverEmail = "finance.manager@light.inc",
                approverName = "Finance Manager",
                status = ApprovalStatus.PENDING,
                createdAt = Instant.parse("2026-04-02T11:00:00Z")
            ),
            // pay-005 (Legal $25000 - Finance) - approved by finance manager
            ApprovalRequest(
                id = "req-006",
                paymentId = "pay-005",
                approverEmail = "finance.manager@light.inc",
                approverName = "Finance Manager",
                status = ApprovalStatus.APPROVED,
                createdAt = Instant.parse("2026-03-28T16:00:00Z"),
                decidedAt = Instant.parse("2026-03-29T09:00:00Z"),
                comment = "Approved - ongoing engagement"
            ),
            // pay-006 (JetBrains 750 EUR - Engineering) - pending team lead
            ApprovalRequest(
                id = "req-007",
                paymentId = "pay-006",
                approverEmail = "teamlead@light.inc",
                approverName = "Team Lead",
                status = ApprovalStatus.PENDING,
                createdAt = Instant.parse("2026-04-05T08:30:00Z")
            ),
            // pay-007 (Google Ads $3200 - Marketing) - rejected by team lead
            ApprovalRequest(
                id = "req-008",
                paymentId = "pay-007",
                approverEmail = "teamlead@light.inc",
                approverName = "Team Lead",
                status = ApprovalStatus.REJECTED,
                createdAt = Instant.parse("2026-03-25T13:00:00Z"),
                decidedAt = Instant.parse("2026-03-25T14:00:00Z"),
                comment = "Budget exceeded for this quarter"
            )
        )
        seed.forEach { requests[it.id] = it }
    }

    fun findById(id: String): ApprovalRequest? = requests[id]

    fun findByPaymentId(paymentId: String): List<ApprovalRequest> =
        requests.values.filter { it.paymentId == paymentId }

    fun findByStatus(status: ApprovalStatus): List<ApprovalRequest> =
        requests.values.filter { it.status == status }

    fun save(request: ApprovalRequest): ApprovalRequest {
        requests[request.id] = request
        return request
    }
}
