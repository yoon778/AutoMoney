package com.choiyoonseo.automoney.notification

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class NotificationDispatchCoordinatorTest {
    @Test
    fun blockedSourceRecordsMetadataWithoutReadingContent() {
        var reads = 0
        val observed = mutableListOf<Pair<String, Instant>>()
        val coordinator = NotificationDispatchCoordinator(
            accessFor = { NotificationSourceAccess.BLOCKED },
            recordObserved = { packageName, seenAt -> observed += packageName to seenAt },
            snapshotBuilder = NotificationSnapshotBuilder()
        )

        val prepared = coordinator.prepare(
            packageName = "com.example.chat",
            postedAt = Instant.ofEpochMilli(1000),
            readContent = {
                reads += 1
                error("blocked content must not be read")
            }
        )

        assertThat(prepared).isNull()
        assertThat(reads).isEqualTo(0)
        assertThat(observed).containsExactly(
            "com.example.chat" to Instant.ofEpochMilli(1000)
        )
    }

    @Test
    fun allowedSourceReadsContentExactlyOnceAndCarriesAccess() {
        var reads = 0
        val coordinator = NotificationDispatchCoordinator(
            accessFor = { NotificationSourceAccess.SELECTED_UNVERIFIED },
            recordObserved = { _, _ -> },
            snapshotBuilder = NotificationSnapshotBuilder(),
            resolveInstalledLabel = { "테스트 은행" }
        )

        val prepared = coordinator.prepare(
            packageName = "com.example.bank",
            postedAt = Instant.ofEpochMilli(1000),
            readContent = {
                reads += 1
                NotificationContentFields(
                    packageName = "wrong.package",
                    notificationKey = "key",
                    postTimeMillis = 9999,
                    title = "은행",
                    text = "10,000원 입금",
                    bigText = null
                )
            }
        )

        assertThat(reads).isEqualTo(1)
        assertThat(prepared?.sourceAccess)
            .isEqualTo(NotificationSourceAccess.SELECTED_UNVERIFIED)
        assertThat(prepared?.snapshot?.packageName).isEqualTo("com.example.bank")
        assertThat(prepared?.snapshot?.postedAt).isEqualTo(Instant.ofEpochMilli(1000))
        assertThat(prepared?.sourceLabel).isEqualTo("테스트 은행")
    }
}
