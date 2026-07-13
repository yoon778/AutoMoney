package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationIngestionFeedbackTest {
    @Test
    fun autoConfirmedSaveCreatesPrivateSuccessFeedback() {
        assertThat(
            notificationFeedbackFor(IngestionResult.Saved(TransactionType.EXPENSE, null))
        ).isEqualTo(
            NotificationIngestionFeedback(
                kind = NotificationIngestionFeedbackKind.AUTO_RECORDED,
                title = "거래 자동 입력됨",
                text = "새 거래를 자동으로 기록함"
            )
        )
    }

    @Test
    fun reviewSaveCreatesPrivateReviewFeedback() {
        assertThat(
            notificationFeedbackFor(
                IngestionResult.Saved(TransactionType.INCOME, ReviewReason.INCOME_UNKNOWN)
            )
        ).isEqualTo(
            NotificationIngestionFeedback(
                kind = NotificationIngestionFeedbackKind.NEEDS_REVIEW,
                title = "거래 검토 필요",
                text = "검토 탭에서 새 거래를 확인해 주세요"
            )
        )
    }

    @Test
    fun duplicateAndIgnoredResultsDoNotNotify() {
        assertThat(notificationFeedbackFor(IngestionResult.Duplicate(TransactionType.EXPENSE))).isNull()
        assertThat(notificationFeedbackFor(IngestionResult.Ignored("not parsed"))).isNull()
    }
}
