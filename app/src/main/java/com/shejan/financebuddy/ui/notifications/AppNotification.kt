package com.shejan.financebuddy.ui.notifications

enum class NotificationType {
    SMS_PENDING,
    LOAN_DUE,
    BUDGET_ALERT,
    GOAL_MILESTONE,
    SYSTEM_INFO
}

enum class NotificationSeverity {
    INFO,
    WARNING,
    CRITICAL,
    SUCCESS
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: NotificationType,
    val severity: NotificationSeverity = NotificationSeverity.INFO,
    val actionRoute: String? = null,
    val actionLabel: String? = null,
    val isRead: Boolean = false
)
