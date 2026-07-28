package com.choiyoonseo.automoney.ui.model

import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryReason
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryRecord
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryStatus
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class NotificationHistoryUiTest {
    @Test
    fun mapsAllStatusesToStableKoreanLabels() {
        val labels = NotificationHistoryStatus.entries.associateWith { status ->
            record(status).toUi().resultLabel
        }

        assertThat(labels).containsExactly(
            NotificationHistoryStatus.SAVED, "저장 완료",
            NotificationHistoryStatus.REVIEW, "검토 필요",
            NotificationHistoryStatus.IGNORED, "처리 안 됨",
            NotificationHistoryStatus.DUPLICATE, "중복 알림",
            NotificationHistoryStatus.BLOCKED, "차단된 앱",
            NotificationHistoryStatus.ERROR, "처리 오류",
            NotificationHistoryStatus.RESOLVED_MANUALLY, "직접 기록 완료"
        )
    }

    @Test
    fun onlyIgnoredAndErrorCanBeRecordedManually() {
        val recordable = NotificationHistoryStatus.entries.filter { record(it).toUi().canRecordManually }

        assertThat(recordable).containsExactly(
            NotificationHistoryStatus.IGNORED,
            NotificationHistoryStatus.ERROR
        )
    }

    @Test
    fun fallsBackToPackageNameWhenLabelIsMissing() {
        assertThat(record(NotificationHistoryStatus.SAVED).copy(sourceLabel = null).toUi().sourceLabel)
            .isEqualTo("com.kbankwith.smartbank")
    }

    private fun record(status: NotificationHistoryStatus) = NotificationHistoryRecord(
        id = 1,
        packageName = "com.kbankwith.smartbank",
        sourceLabel = "케이뱅크",
        receivedAt = Instant.EPOCH,
        status = status,
        transactionType = null,
        amountWon = 6_000,
        reason = NotificationHistoryReason.SAVED_AUTOMATICALLY,
        linkedTransactionId = null
    )
}
