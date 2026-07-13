package com.choiyoonseo.automoney.notification

enum class NotificationIngestionFeedbackKind {
    AUTO_RECORDED,
    NEEDS_REVIEW
}

data class NotificationIngestionFeedback(
    val kind: NotificationIngestionFeedbackKind,
    val title: String,
    val text: String
)

fun notificationFeedbackFor(result: IngestionResult): NotificationIngestionFeedback? =
    when (result) {
        is IngestionResult.Saved -> if (result.reviewReason == null) {
            NotificationIngestionFeedback(
                kind = NotificationIngestionFeedbackKind.AUTO_RECORDED,
                title = "거래 자동 입력됨",
                text = "새 거래를 자동으로 기록함"
            )
        } else {
            NotificationIngestionFeedback(
                kind = NotificationIngestionFeedbackKind.NEEDS_REVIEW,
                title = "거래 검토 필요",
                text = "검토 탭에서 새 거래를 확인해 주세요"
            )
        }
        is IngestionResult.Duplicate,
        is IngestionResult.Ignored -> null
    }
