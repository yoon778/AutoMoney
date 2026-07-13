package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class NotificationSnapshotBuilderTest {
    private val builder = NotificationSnapshotBuilder()

    @Test
    fun buildsSnapshotIncludingExpandedTextLines() {
        val snapshot = builder.build(
            NotificationContentFields(
                packageName = "viva.republica.toss",
                postTimeMillis = Instant.parse("2026-07-02T03:00:00Z").toEpochMilli(),
                title = "토스",
                text = "네이버페이 10,000원 충전",
                bigText = null,
                notificationKey = "bank-key",
                textLines = listOf(
                    "네이버페이 10,000원 충전",
                    "충전 알림은 검토가 필요해요"
                )
            )
        )

        assertThat(snapshot.packageName).isEqualTo("viva.republica.toss")
        assertThat(snapshot.title).isEqualTo("토스")
        assertThat(snapshot.text).isEqualTo("네이버페이 10,000원 충전")
        assertThat(snapshot.bigText).isEqualTo("충전 알림은 검토가 필요해요")
        assertThat(snapshot.postedAt).isEqualTo(Instant.parse("2026-07-02T03:00:00Z"))
        assertThat(snapshot.notificationKey).isEqualTo("bank-key")
    }

    @Test
    fun removesBlankAndRepeatedNotificationLines() {
        val snapshot = builder.build(
            NotificationContentFields(
                packageName = "viva.republica.toss",
                postTimeMillis = Instant.parse("2026-07-02T04:00:00Z").toEpochMilli(),
                title = "토스뱅크 체크카드",
                text = "스타벅스 홍대입구 6,100원 결제",
                bigText = "스타벅스 홍대입구 6,100원 결제\n06.27 12:47\n",
                textLines = listOf(
                    "스타벅스 홍대입구 6,100원 결제",
                    "06.27 12:47",
                    " "
                )
            )
        )

        assertThat(snapshot.combinedText.lines()).containsExactly(
            "토스뱅크 체크카드",
            "스타벅스 홍대입구 6,100원 결제",
            "06.27 12:47"
        ).inOrder()
    }

    @Test
    fun limitsAllContentFields() {
        val snapshot = builder.build(
            NotificationContentFields(
                packageName = "com.example.bank",
                postTimeMillis = 0,
                title = "T".repeat(300),
                text = "X".repeat(2_000),
                bigText = (1..20).joinToString("\n") { "$it-${"B".repeat(600)}" },
                textLines = (1..20).map { "$it-${"L".repeat(600)}" }
            )
        )

        assertThat(snapshot.title).hasLength(256)
        assertThat(snapshot.text).hasLength(1_024)
        assertThat(snapshot.bigText.orEmpty().lines().size).isAtMost(10)
        assertThat(snapshot.bigText.orEmpty().length).isAtMost(4_096)
    }
}
