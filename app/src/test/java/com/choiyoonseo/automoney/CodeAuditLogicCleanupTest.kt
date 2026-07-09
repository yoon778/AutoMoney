package com.choiyoonseo.automoney

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class CodeAuditLogicCleanupTest {
    @Test
    fun unusedLogicEntryPointsAreRemoved() {
        assertThat(File("src/main/java/com/choiyoonseo/automoney/export/CsvExporter.kt").exists()).isFalse()
        assertThat(File("src/main/java/com/choiyoonseo/automoney/notification/DailyReviewNotifier.kt").exists()).isFalse()
    }
}
