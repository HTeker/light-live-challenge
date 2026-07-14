package org.light.challenge.service

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class NotificationService {

    fun notifyApprover(approverEmail: String, approverName: String, paymentDescription: String, amount: String) {
        // In production this would integrate with Slack/email
        logger.info { "Sending notification to $approverName ($approverEmail): " +
            "Payment approval needed - $paymentDescription ($amount)" }
    }

    fun notifyPaymentApproved(submitterEmail: String, paymentDescription: String) {
        logger.info { "Sending notification to $submitterEmail: " +
            "Your payment has been approved - $paymentDescription" }
    }

    fun notifyPaymentRejected(submitterEmail: String, paymentDescription: String, reason: String?) {
        logger.info { "Sending notification to $submitterEmail: " +
            "Your payment has been rejected - $paymentDescription. Reason: ${reason ?: "No reason provided"}" }
    }
}
