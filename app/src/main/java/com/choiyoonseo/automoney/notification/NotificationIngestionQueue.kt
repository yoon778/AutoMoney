package com.choiyoonseo.automoney.notification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

internal class NotificationIngestionQueue(
    scope: CoroutineScope,
    private val process: suspend (PreparedNotification) -> Unit,
    private val onFailure: suspend (PreparedNotification, RuntimeException) -> Unit = { _, _ -> }
) {
    private val notifications = Channel<PreparedNotification>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (prepared in notifications) {
                try {
                    process(prepared)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: RuntimeException) {
                    try {
                        onFailure(prepared, e)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: RuntimeException) {
                        // 한 알림의 진단 실패가 이후 금융 알림 수집을 중단하면 안 된다.
                    }
                }
            }
        }
    }

    fun submit(prepared: PreparedNotification) {
        notifications.trySend(prepared).getOrThrow()
    }

    fun close() {
        notifications.close()
    }
}
