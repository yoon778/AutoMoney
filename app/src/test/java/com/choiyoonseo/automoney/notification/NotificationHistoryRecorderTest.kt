package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.data.repository.NotificationHistoryRepository
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryReason
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryRecord
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryStatus
import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NotificationHistoryRecorderTest {
    @Test
    fun recordContainsNoSnapshotTextOrRawError() = runTest {
        val repository = RecordingHistoryRepository()
        val recorder = NotificationHistoryRecorder(repository, clock = fixedClock)

        recorder.recordError(prepared(text = "계좌 123-456 홍길동", label = "케이뱅크"))

        val row = repository.saved.single()
        assertThat(row.sourceLabel).isEqualTo("케이뱅크")
        assertThat(row.reason).isEqualTo(NotificationHistoryReason.PROCESSING_ERROR)
        val entitySource = File(
            "src/main/java/com/choiyoonseo/automoney/data/local/entity/NotificationHistoryEntity.kt"
        ).readText()
        assertThat(entitySource).doesNotContain("val title:")
        assertThat(entitySource).doesNotContain("val text:")
        assertThat(entitySource).doesNotContain("val bigText:")
        assertThat(entitySource).doesNotContain("val errorMessage:")
        assertThat(entitySource).doesNotContain("val notificationKey:")
    }

    @Test
    fun historyFailureDoesNotReplaceSavedResult() = runTest {
        val recorder = NotificationHistoryRecorder(RecordingHistoryRepository())

        val result = recorder.bestEffort { error("db") }

        assertThat(result).isFalse()
    }

    @Test
    fun savedReviewAndDuplicateMapToFixedCodes() = runTest {
        val repository = RecordingHistoryRepository()
        val recorder = NotificationHistoryRecorder(repository, clock = fixedClock)
        val prepared = prepared("6,000원 결제", "케이뱅크")

        recorder.recordResult(
            prepared,
            IngestionResult.Saved(TransactionType.EXPENSE, reviewReason = null, transactionId = 7)
        )
        recorder.recordResult(prepared, IngestionResult.Duplicate(TransactionType.EXPENSE))

        assertThat(repository.saved.map { it.status })
            .containsExactly(NotificationHistoryStatus.SAVED, NotificationHistoryStatus.DUPLICATE)
            .inOrder()
        assertThat(repository.saved.first().linkedTransactionId).isEqualTo(7)
        assertThat(repository.saved.map { it.amountWon }).containsExactly(6_000L, 6_000L)
    }

    @Test
    fun savedHistoryUsesParsedEventAmountInsteadOfRawFirstAmount() = runTest {
        val repository = RecordingHistoryRepository()
        val recorder = NotificationHistoryRecorder(repository, clock = fixedClock)

        recorder.recordResult(
            prepared("잔액 90,000원\n스타벅스 6,000원 결제", "토스"),
            IngestionResult.Saved(
                transactionType = TransactionType.EXPENSE,
                reviewReason = null,
                transactionId = 7,
                amountWon = 6_000
            )
        )

        assertThat(repository.saved.single().amountWon).isEqualTo(6_000)
    }

    @Test
    fun blockedSourceIsRecordedWithoutAmountOrContent() = runTest {
        val repository = RecordingHistoryRepository()
        val recorder = NotificationHistoryRecorder(repository, clock = fixedClock)

        recorder.recordResult(
            prepared(text = null, label = "케이뱅크")
                .copy(sourceAccess = NotificationSourceAccess.BLOCKED),
            IngestionResult.Ignored("blocked source")
        )

        val row = repository.saved.single()
        assertThat(row.status).isEqualTo(NotificationHistoryStatus.BLOCKED)
        assertThat(row.reason).isEqualTo(NotificationHistoryReason.BLOCKED_SOURCE)
        assertThat(row.amountWon).isNull()
        assertThat(row.transactionType).isNull()
    }

    private fun prepared(text: String?, label: String) = PreparedNotification(
        snapshot = NotificationSnapshot(
            packageName = "com.kbankwith.smartbank",
            title = null,
            text = text,
            bigText = null,
            postedAt = NOW
        ),
        sourceAccess = NotificationSourceAccess.TRUSTED,
        sourceLabel = label
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-21T01:00:00Z")
        val fixedClock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)
    }
}

private class RecordingHistoryRepository : NotificationHistoryRepository {
    val saved = mutableListOf<NotificationHistoryRecord>()
    override fun observeRecent(): Flow<List<NotificationHistoryRecord>> = flowOf(saved)
    override suspend fun recordAndPrune(record: NotificationHistoryRecord): Long {
        saved += record
        return saved.size.toLong()
    }
    override suspend fun clear() = saved.clear()
    override suspend fun markResolvedManually(historyId: Long, transactionId: Long): Boolean = true
}
