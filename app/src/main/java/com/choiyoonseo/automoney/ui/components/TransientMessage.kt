package com.choiyoonseo.automoney.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

internal const val TRANSIENT_MESSAGE_DURATION_MILLIS = 3_000L

@Composable
fun AutoClearMessageEffect(
    message: String?,
    onClear: () -> Unit
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(TRANSIENT_MESSAGE_DURATION_MILLIS)
            onClear()
        }
    }
}
