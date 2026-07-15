package org.light.challenge.service

import mu.KotlinLogging
import org.light.challenge.model.*
import org.light.challenge.repository.ApprovalRequestRepository
import org.light.challenge.repository.ApprovalRuleRepository
import org.light.challenge.repository.PaymentRepository
import java.math.BigDecimal
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * A pending approval request enriched with summary details of the payment it belongs to,
 * so the approvals queue can show context (amount, vendor) without a second lookup.
 */
data class PendingApprovalView(
    val id: String,
    val paymentId: String,
    val approverEmail: String,
    val approverName: String,
    val status: ApprovalStatus,
    val createdAt: Instant,
    val amount: BigDecimal,
    val currency: String,
    val description: String,
    val vendor: String,
    val department: Department
)

class ApprovalService(
    private val paymentRepository: PaymentRepository,
    private val approvalRuleRepository: ApprovalRuleRepository,
    private val approvalRequestRepository: ApprovalRequestRepository,
    private val notificationService: NotificationService
) {

    fun submitPayment(request: CreatePaymentRequest): Payment {
        val payment = Payment(
            amount = request.amount,
            currency = request.currency,
            department = request.department,
            vendor = request.vendor,
            description = request.description,
            submittedBy = request.submittedBy
        )
        paymentRepository.save(payment)

        val matchingRules = findMatchingRules(payment)

        if (matchingRules.isEmpty()) {
            payment.status = PaymentStatus.APPROVED
            paymentRepository.save(payment)
            logger.info { "Payment ${payment.id} auto-approved (no matching rules)" }
        } else {
            for (rule in matchingRules) {
                val approvalRequest = ApprovalRequest(
                    paymentId = payment.id,
                    approverEmail = rule.approverEmail,
                    approverName = rule.approverName
                )
                approvalRequestRepository.save(approvalRequest)
                notificationService.notifyApprover(
                    approverEmail = rule.approverEmail,
                    approverName = rule.approverName,
                    paymentDescription = payment.description,
                    amount = "${payment.currency} ${payment.amount}"
                )
            }
            logger.info { "Payment ${payment.id} requires ${matchingRules.size} approval(s)" }
        }

        return payment
    }

    fun findMatchingRules(payment: Payment): List<ApprovalRule> =
        approvalRuleRepository.findAll()
            .filter { it.department == null || it.department == payment.department }
            .filter { it.minAmount <= payment.amount }
            .filter { it.maxAmount == null || it.maxAmount > payment.amount }

    fun processDecision(approvalRequestId: String, decision: ApprovalDecisionRequest): ApprovalRequest {
        val approvalRequest = approvalRequestRepository.findById(approvalRequestId)
            ?: throw IllegalArgumentException("Approval request not found: $approvalRequestId")

        if (approvalRequest.status != ApprovalStatus.PENDING) {
            throw IllegalStateException("Approval request has already been decided")
        }

        val payment = paymentRepository.findById(approvalRequest.paymentId)
            ?: throw IllegalStateException("Payment not found for approval request")

        approvalRequest.status = decision.decision
        approvalRequest.decidedAt = Instant.now()
        approvalRequest.comment = decision.comment
        approvalRequestRepository.save(approvalRequest)

        val allRequests = approvalRequestRepository.findByPaymentId(payment.id)

        if (allRequests.any { it.status == ApprovalStatus.REJECTED }) {
            payment.status = PaymentStatus.REJECTED
            paymentRepository.save(payment)
            notificationService.notifyPaymentRejected(
                submitterEmail = payment.submittedBy,
                paymentDescription = payment.description,
                reason = decision.comment
            )
            logger.info { "Payment ${payment.id} rejected by ${approvalRequest.approverName}" }
        }

        if (allRequests.all { it.status == ApprovalStatus.APPROVED }) {
            payment.status = PaymentStatus.APPROVED
            paymentRepository.save(payment)
            notificationService.notifyPaymentApproved(
                submitterEmail = payment.submittedBy,
                paymentDescription = payment.description
            )
            logger.info { "Payment ${payment.id} fully approved" }
        }

        return approvalRequest
    }

    fun getAllPayments(): List<Payment> = paymentRepository.findAll()

    fun getPaymentsByStatus(status: PaymentStatus): List<Payment> = paymentRepository.findByStatus(status)

    fun getPayment(id: String): Payment? = paymentRepository.findById(id)

    fun getPendingApprovalDetails(): List<PendingApprovalView> =
        approvalRequestRepository.findByStatus(ApprovalStatus.PENDING).mapNotNull { request ->
            val payment = paymentRepository.findById(request.paymentId) ?: return@mapNotNull null
            PendingApprovalView(
                id = request.id,
                paymentId = request.paymentId,
                approverEmail = request.approverEmail,
                approverName = request.approverName,
                status = request.status,
                createdAt = request.createdAt,
                amount = payment.amount,
                currency = payment.currency,
                description = payment.description,
                vendor = payment.vendor,
                department = payment.department
            )
        }

    fun getApprovalsForPayment(paymentId: String): List<ApprovalRequest> =
        approvalRequestRepository.findByPaymentId(paymentId)
}
