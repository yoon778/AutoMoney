package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryRecord
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryStatus
import java.time.Instant

data class NotificationHistoryRowUi(
    val id: Long,
    val sourceLabel: String,
    val receivedAt: Instant,
    val resultLabel: String,
    val amountWon: Long?,
    val canRecordManually: Boolean
)

fun NotificationHistoryRecord.toUi(): NotificationHistoryRowUi = NotificationHistoryRowUi(
    id = id,
    sourceLabel = sourceLabel ?: packageName,
    receivedAt = receivedAt,
    resultLabel = status.toKoreanLabel(),
    amountWon = amountWon,
    canRecordManually = status == NotificationHistoryStatus.IGNORED ||
        status == NotificationHistoryStatus.ERROR
)

private fun NotificationHistoryStatus.toKoreanLabel(): String = when (this) {
    NotificationHistoryStatus.SAVED -> "저장 완료"
    NotificationHistoryStatus.REVIEW -> "검토 필요"
    NotificationHistoryStatus.IGNORED -> "처리 안 됨"
    NotificationHistoryStatus.DUPLICATE -> "중복 알림"
    NotificationHistoryStatus.ERROR -> "처리 오류"
    NotificationHistoryStatus.RESOLVED_MANUALLY -> "직접 기록 완료"
}
