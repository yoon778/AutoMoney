package com.choiyoonseo.automoney.ui.components

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class TransientMessageTest {
    @Test
    fun confirmationMessagesAutoClearInScreens() {
        assertThat(TRANSIENT_MESSAGE_DURATION_MILLIS).isEqualTo(3_000L)

        listOf(
            "src/main/java/com/choiyoonseo/automoney/ui/transactions/TransactionsScreen.kt",
            "src/main/java/com/choiyoonseo/automoney/ui/assets/AssetsScreen.kt",
            "src/main/java/com/choiyoonseo/automoney/ui/review/ReviewScreen.kt"
        ).forEach { path ->
            assertThat(File(path).readText()).contains("AutoClearMessageEffect")
        }
    }
}
