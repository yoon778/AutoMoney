package com.choiyoonseo.automoney.domain.parser

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class NotificationIdentityTest {
    @Test
    fun sameAndroidNotificationKeepsHashWhenTextChanges() {
        val first = snapshot(key = "bank-key", text = "10,000 transfer")
        val updated = snapshot(key = "bank-key", text = "10,000 transfer complete")

        assertThat(first.sourceNotificationHash).isEqualTo(updated.sourceNotificationHash)
    }

    @Test
    fun repeatedTextAtAnotherPostTimeIsNotDuplicate() {
        val first = snapshot(key = null, text = "10,000 transfer", postTime = 1_000)
        val second = snapshot(key = null, text = "10,000 transfer", postTime = 2_000)

        assertThat(first.sourceNotificationHash).isNotEqualTo(second.sourceNotificationHash)
    }

    private fun snapshot(
        key: String?,
        text: String,
        postTime: Long = 1_000
    ) = NotificationSnapshot(
        packageName = "test.bank",
        title = "Bank",
        text = text,
        bigText = null,
        postedAt = Instant.ofEpochMilli(postTime),
        notificationKey = key
    )
}
