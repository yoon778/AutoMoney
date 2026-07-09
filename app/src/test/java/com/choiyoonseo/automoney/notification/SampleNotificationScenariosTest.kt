package com.choiyoonseo.automoney.notification

import com.choiyoonseo.automoney.domain.parser.TossNotificationParser
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class SampleNotificationScenariosTest {
    @Test
    fun sampleNotificationScenariosCoverCoreMoneyCases() {
        assertThat(sampleNotificationScenarios.map { it.id }).containsExactly(
            "card_payment",
            "wallet_topup",
            "transfer",
            "refund_cancel",
            "payment_gateway"
        ).inOrder()
        assertThat(sampleNotificationScenarios.map { it.label }).containsExactly(
            "카드 결제",
            "포인트 충전",
            "친구 송금",
            "결제 취소",
            "결제대행사"
        ).inOrder()
    }

    @Test
    fun sampleNotificationScenarioBuildsTossSnapshot() {
        val snapshot = sampleNotificationScenarios.first().toSnapshot(
            postedAt = Instant.parse("2026-07-01T01:00:00Z")
        )

        assertThat(snapshot.packageName).isEqualTo(TossNotificationParser.TOSS_PACKAGE)
        assertThat(snapshot.title).isEqualTo("토스뱅크 체크카드")
        assertThat(snapshot.text).isEqualTo("스타벅스 홍대입구 6,100원 결제")
        assertThat(snapshot.bigText).contains("06.27 12:47")
        assertThat(snapshot.postedAt).isEqualTo(Instant.parse("2026-07-01T01:00:00Z"))
    }

    @Test
    fun sampleNotificationScenarioRunnerIngestsAndStoresDiagnostic() = runTest {
        var ingestedPackage: String? = null
        var savedDiagnostic: LastNotificationDiagnostic? = null
        val runner = RunSampleNotificationScenarioUseCase(
            ingestSnapshot = { snapshot ->
                ingestedPackage = snapshot.packageName
                IngestionResult.Ignored("test only")
            },
            saveDiagnostic = { diagnostic ->
                savedDiagnostic = diagnostic
            },
            clock = { Instant.parse("2026-07-01T01:00:00Z") }
        )

        runner.run(sampleNotificationScenarios.first())

        assertThat(ingestedPackage).isEqualTo(TossNotificationParser.TOSS_PACKAGE)
        assertThat(savedDiagnostic?.result).isEqualTo(NotificationDiagnosticResult.IGNORED)
        assertThat(savedDiagnostic?.message).isEqualTo("test only")
    }
}
