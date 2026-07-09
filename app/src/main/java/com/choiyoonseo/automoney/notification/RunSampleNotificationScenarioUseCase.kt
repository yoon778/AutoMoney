package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.NotificationSnapshot
import java.time.Instant

class RunSampleNotificationScenarioUseCase(
    private val ingestSnapshot: suspend (NotificationSnapshot) -> IngestionResult,
    private val saveDiagnostic: (LastNotificationDiagnostic) -> Unit,
    private val clock: () -> Instant = Instant::now
) {
    constructor(
        notificationIngestionUseCase: NotificationIngestionUseCase,
        notificationDiagnosticsStore: NotificationDiagnosticsStore
    ) : this(
        ingestSnapshot = notificationIngestionUseCase::ingest,
        saveDiagnostic = notificationDiagnosticsStore::save
    )

    suspend fun run(scenario: SampleNotificationScenario): IngestionResult {
        val now = clock()
        val snapshot = scenario.toSnapshot(postedAt = now)
        return try {
            val result = ingestSnapshot(snapshot)
            saveDiagnostic(
                LastNotificationDiagnostic.fromIngestionResult(
                    snapshot = snapshot,
                    result = result,
                    receivedAt = now
                )
            )
            result
        } catch (e: RuntimeException) {
            saveDiagnostic(
                LastNotificationDiagnostic.fromError(
                    snapshot = snapshot,
                    throwable = e,
                    receivedAt = now
                )
            )
            throw e
        }
    }
}
