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
            "payment_gateway",
            "kb_account_withdrawal",
            "kb_account_deposit",
            "kb_account_transfer"
        ).inOrder()
        assertThat(sampleNotificationScenarios.take(5).map { it.label }).containsExactly(
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
    fun sampleNotificationScenariosHaveUniqueIds() {
        val ids = sampleNotificationScenarios.map { it.id }

        assertThat(ids.distinct()).containsExactlyElementsIn(ids)
    }

    @Test
    fun sampleNotificationScenarioPropagatesPackageAndNotificationKey() {
        val scenario = sampleNotificationScenarios.last()
        val snapshot = scenario.toSnapshot(
            postedAt = Instant.parse("2026-07-01T01:00:00Z")
        )

        assertThat(snapshot.packageName).isEqualTo(scenario.packageName)
        assertThat(snapshot.notificationKey).isEqualTo(scenario.notificationKey)
    }

    @Test
    fun sampleNotificationScenarioIdentityHashIsStableForSameIdentity() {
        val scenario = sampleNotificationScenarios.first()
        val postedAt = Instant.parse("2026-07-01T01:00:00Z")

        assertThat(scenario.toSnapshot(postedAt).sourceNotificationHash)
            .isEqualTo(scenario.toSnapshot(postedAt).sourceNotificationHash)
    }

    @Test
    fun sampleNotificationScenarioIdentityHashSeparatesDifferentPostTimes() {
        val scenario = sampleNotificationScenarios.first()

        assertThat(scenario.toSnapshot(Instant.parse("2026-07-01T01:00:00Z")).sourceNotificationHash)
            .isNotEqualTo(scenario.toSnapshot(Instant.parse("2026-07-01T01:00:01Z")).sourceNotificationHash)
    }

    @Test
    fun sampleNotificationScenarioRunnerIngestsAndStoresDiagnostic() = runTest {
        var ingestedPackage: String? = null
        var sourceAccess: NotificationSourceAccess? = null
        var savedDiagnostic: LastNotificationDiagnostic? = null
        val runner = RunSampleNotificationScenarioUseCase(
            ingestSnapshot = { snapshot, access ->
                ingestedPackage = snapshot.packageName
                sourceAccess = access
                IngestionResult.Ignored("test only")
            },
            saveDiagnostic = { diagnostic ->
                savedDiagnostic = diagnostic
            },
            clock = { Instant.parse("2026-07-01T01:00:00Z") }
        )

        runner.run(sampleNotificationScenarios.first())

        assertThat(ingestedPackage).isEqualTo(TossNotificationParser.TOSS_PACKAGE)
        assertThat(sourceAccess).isEqualTo(NotificationSourceAccess.TRUSTED)
        assertThat(savedDiagnostic?.result).isEqualTo(NotificationDiagnosticResult.IGNORED)
        assertThat(savedDiagnostic?.message).isEqualTo("test only")
    }
}
