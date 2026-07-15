package org.light.challenge.repository

import org.light.challenge.model.ApprovalRule
import org.light.challenge.model.Department
import java.math.BigDecimal

class ApprovalRuleRepository {
    private val rules = mutableListOf<ApprovalRule>()

    init {
        rules.addAll(listOf(
            // Global rules (apply to all departments)
            ApprovalRule(
                id = "rule-001",
                name = "Standard Payment",
                department = null,
                minAmount = BigDecimal("1000.00"),
                maxAmount = BigDecimal("10000.00"),
                approverEmail = "teamlead@light.inc",
                approverName = "Team Lead"
            ),
            ApprovalRule(
                id = "rule-002",
                name = "Large Payment",
                department = null,
                minAmount = BigDecimal("10000.00"),
                maxAmount = BigDecimal("50000.00"),
                approverEmail = "finance.manager@light.inc",
                approverName = "Finance Manager"
            ),
            ApprovalRule(
                id = "rule-003",
                name = "Enterprise Payment",
                department = null,
                minAmount = BigDecimal("50000.00"),
                maxAmount = null,
                approverEmail = "cfo@light.inc",
                approverName = "CFO"
            ),

            // Department-specific rules
            ApprovalRule(
                id = "rule-004",
                name = "Engineering Vendor Payment",
                department = Department.ENGINEERING,
                minAmount = BigDecimal("5000.00"),
                maxAmount = BigDecimal("25000.00"),
                approverEmail = "eng.director@light.inc",
                approverName = "Engineering Director"
            ),
            ApprovalRule(
                id = "rule-005",
                name = "Marketing Campaign",
                department = Department.MARKETING,
                minAmount = BigDecimal("5000.00"),
                maxAmount = BigDecimal("30000.00"),
                approverEmail = "cmo@light.inc",
                approverName = "CMO"
            )
        ))
    }

    fun findAll(): List<ApprovalRule> = rules.toList()
}
