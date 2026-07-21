package com.choiyoonseo.automoney.domain.notificationhistory

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class SafeNotificationAmountExtractorTest {
    private val extractor = SafeNotificationAmountExtractor()

    @Test
    fun extractsOnlyOneActionAmount() {
        assertThat(extractor.extract(snapshot("스타벅스 6,000원 결제"))).isEqualTo(6_000)
    }

    @Test
    fun rejectsAmbiguousAndBalanceAmounts() {
        assertThat(extractor.extract(snapshot("6,000원 또는 6원 결제"))).isNull()
        assertThat(extractor.extract(snapshot("잔액 6,000원"))).isNull()
    }

    @Test
    fun duplicateExpandedTextStillProducesOneAmount() {
        val snapshot = NotificationSnapshot(
            packageName = "com.kbankwith.smartbank",
            title = "케이뱅크",
            text = "스타벅스 6,000원 결제",
            bigText = "스타벅스 6,000원 결제 완료",
            postedAt = Instant.EPOCH
        )

        assertThat(extractor.extract(snapshot)).isEqualTo(6_000)
    }

    private fun snapshot(text: String) = NotificationSnapshot(
        packageName = "com.kbankwith.smartbank",
        title = null,
        text = text,
        bigText = null,
        postedAt = Instant.EPOCH
    )
}
