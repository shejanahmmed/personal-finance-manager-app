package com.shejan.financebuddy.ui.notifications

import com.shejan.financebuddy.data.db.BudgetEntity
import com.shejan.financebuddy.data.db.GoalEntity
import com.shejan.financebuddy.data.db.LoanEntity
import com.shejan.financebuddy.data.db.PendingSmsTransactionEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import java.text.DecimalFormat
import java.util.Calendar

object NotificationHelper {

    fun generateNotifications(
        pendingSmsList: List<PendingSmsTransactionEntity>,
        loans: List<LoanEntity>,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        transactions: List<TransactionEntity>
    ): List<AppNotification> {
        val list = mutableListOf<AppNotification>()
        val fmt = DecimalFormat("#,##0")

        // 1. Pending SMS Inbox Alert
        if (pendingSmsList.isNotEmpty()) {
            val count = pendingSmsList.size
            list.add(
                AppNotification(
                    id = "sms_inbox_pending",
                    title = "Pending Inbox Items",
                    message = "You have $count detected bank/MFS transaction${if (count > 1) "s" else ""} awaiting your review and approval.",
                    timestamp = pendingSmsList.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis(),
                    type = NotificationType.SMS_PENDING,
                    severity = NotificationSeverity.WARNING,
                    actionRoute = "pending_transactions",
                    actionLabel = "Review Inbox"
                )
            )
        }

        // 2. Budget Alerts (Current Month Spending vs Cap)
        if (budgets.isNotEmpty() && transactions.isNotEmpty()) {
            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH)

            val currentMonthExpenses = transactions.filter { t ->
                if (t.type != "EXPENSE") return@filter false
                val c = Calendar.getInstance().apply { timeInMillis = t.timestamp }
                c.get(Calendar.YEAR) == currentYear && c.get(Calendar.MONTH) == currentMonth
            }

            for (b in budgets) {
                val spent = currentMonthExpenses
                    .filter { it.category.equals(b.category, ignoreCase = true) }
                    .sumOf { it.amount }

                if (spent >= b.limitAmount && b.limitAmount > 0) {
                    list.add(
                        AppNotification(
                            id = "budget_over_${b.id}",
                            title = "${b.category} Budget Exceeded",
                            message = "You spent ৳${fmt.format(spent)}, exceeding your limit of ৳${fmt.format(b.limitAmount)} by ৳${fmt.format(spent - b.limitAmount)}.",
                            timestamp = System.currentTimeMillis(),
                            type = NotificationType.BUDGET_ALERT,
                            severity = NotificationSeverity.CRITICAL,
                            actionRoute = "budget",
                            actionLabel = "View Budget"
                        )
                    )
                } else if (b.limitAmount > 0 && spent >= (b.limitAmount * 0.8)) {
                    val pct = ((spent / b.limitAmount) * 100).toInt()
                    list.add(
                        AppNotification(
                            id = "budget_warn_${b.id}",
                            title = "${b.category} Budget Warning",
                            message = "You have reached $pct% of your monthly limit (৳${fmt.format(spent)} / ৳${fmt.format(b.limitAmount)}).",
                            timestamp = System.currentTimeMillis(),
                            type = NotificationType.BUDGET_ALERT,
                            severity = NotificationSeverity.WARNING,
                            actionRoute = "budget",
                            actionLabel = "View Budget"
                        )
                    )
                }
            }
        }

        // 3. Loans Due / Active Reminders
        val activeLoans = loans.filter { (it.loanAmount - it.repaidAmount) > 0.5 }
        for (loan in activeLoans) {
            val dueAmount = loan.loanAmount - loan.repaidAmount
            list.add(
                AppNotification(
                    id = "loan_${loan.id}",
                    title = "Loan Repayment Reminder",
                    message = "${loan.bankName}: Active loan balance remaining is ৳${fmt.format(dueAmount)}.",
                    timestamp = loan.createdAt,
                    type = NotificationType.LOAN_DUE,
                    severity = NotificationSeverity.INFO,
                    actionRoute = "loans",
                    actionLabel = "View Loans"
                )
            )
        }

        // 4. Goal Milestones
        for (goal in goals) {
            if (goal.savedAmount >= goal.targetAmount && goal.targetAmount > 0) {
                list.add(
                    AppNotification(
                        id = "goal_${goal.id}",
                        title = "Savings Goal Achieved!",
                        message = "Congratulations! You completed your savings goal '${goal.title}' (${goal.emoji}) of ৳${fmt.format(goal.targetAmount)}.",
                        timestamp = goal.createdAt,
                        type = NotificationType.GOAL_MILESTONE,
                        severity = NotificationSeverity.SUCCESS,
                        actionRoute = "goals",
                        actionLabel = "View Goal"
                    )
                )
            }
        }

        // 5. System Security & Privacy status
        list.add(
            AppNotification(
                id = "system_security",
                title = "Local Vault Secured",
                message = "All financial records and SMS data are stored safely on-device with hardware-backed encryption.",
                timestamp = System.currentTimeMillis(),
                type = NotificationType.SYSTEM_INFO,
                severity = NotificationSeverity.INFO,
                actionRoute = null,
                actionLabel = null
            )
        )

        return list.sortedByDescending { it.timestamp }
    }
}
