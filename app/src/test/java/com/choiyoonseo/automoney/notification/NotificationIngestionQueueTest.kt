package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationIngestionQueueTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun processesNotificationsInSubmissionOrder() = runTest {
        val processed = mutableListOf<String>()
        val queue = NotificationIngestionQueue(
            scope = this,
            process = { prepared -> processed += prepared.snapshot.text.orEmpty() }
        )

        queue.submit(prepared("6,000원 결제"))
        queue.submit(prepared("6원 입금"))
        advanceUntilIdle()

        assertThat(processed).containsExactly("6,000원 결제", "6원 입금").inOrder()
        queue.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun processingFailureDoesNotStopLaterNotifications() = runTest {
        val processed = mutableListOf<String>()
        val failures = mutableListOf<String>()
        val queue = NotificationIngestionQueue(
            scope = this,
            process = { prepared ->
                val text = prepared.snapshot.text.orEmpty()
                if (text == "실패") error("test failure")
                processed += text
            },
            onFailure = { prepared, _ -> failures += prepared.snapshot.text.orEmpty() }
        )

        queue.submit(prepared("실패"))
        queue.submit(prepared("6,000원 결제"))
        advanceUntilIdle()

        assertThat(failures).containsExactly("실패")
        assertThat(processed).containsExactly("6,000원 결제")
        queue.close()
    }

    private fun prepared(text: String) = PreparedNotification(
        snapshot = NotificationSnapshot(
            packageName = "com.kbankwith.smartbank",
            title = "케이뱅크",
            text = text,
            bigText = null,
            postedAt = Instant.parse("2026-07-21T01:00:00Z"),
            notificationKey = "shared-kbank-key"
        ),
        sourceAccess = NotificationSourceAccess.SELECTED_UNVERIFIED
    )
}
