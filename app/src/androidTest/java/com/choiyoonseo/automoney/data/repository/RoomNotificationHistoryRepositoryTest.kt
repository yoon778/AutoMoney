package com.choiyoonseo.automoney.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.choiyoonseo.automoney.data.local.AppDatabase
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryReason
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryRecord
import com.choiyoonseo.automoney.domain.notificationhistory.NotificationHistoryStatus
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomNotificationHistoryRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomNotificationHistoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomNotificationHistoryRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun recordPrunesOlderThan30DaysAndKeepsNewest200() = runBlocking {
        repeat(205) { index ->
            repository.recordAndPrune(record(NOW.minusSeconds(index.toLong())))
        }
        repository.recordAndPrune(record(NOW.minus(31, ChronoUnit.DAYS)))

        val rows = repository.observeRecent().first()

        assertEquals(200, rows.size)
        assertTrue(rows.minOf { it.receivedAt } >= NOW.minus(30, ChronoUnit.DAYS))
    }

    private fun record(receivedAt: Instant) = NotificationHistoryRecord(
        packageName = "com.kbankwith.smartbank",
        sourceLabel = "케이뱅크",
        receivedAt = receivedAt,
        status = NotificationHistoryStatus.IGNORED,
        transactionType = null,
        amountWon = 6_000,
        reason = NotificationHistoryReason.PARSER_IGNORED,
        linkedTransactionId = null
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-21T01:00:00Z")
    }
}
