package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.notification.FinancialAppRegistry
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class SecuritiesNotificationParserTest {
    private val parser = SecuritiesNotificationParser()

    @Test
    fun supportsRegisteredMajorSecuritiesApps() {
        val packages = listOf(
            FinancialAppRegistry.KIWOOM_SECURITIES_PACKAGE,
            FinancialAppRegistry.MIRAE_SECURITIES_PACKAGE,
            FinancialAppRegistry.SAMSUNG_SECURITIES_PACKAGE,
            FinancialAppRegistry.SHINHAN_SECURITIES_PACKAGE,
            FinancialAppRegistry.KB_SECURITIES_PACKAGE,
            FinancialAppRegistry.NH_SECURITIES_PACKAGE,
            FinancialAppRegistry.KOREA_INVESTMENT_PACKAGE,
            FinancialAppRegistry.HANA_SECURITIES_PACKAGE
        )

        assertThat(packages.all { parser.canParse(snapshot(it, "예수금 사용액 1,000원")) }).isTrue()
    }

    @Test
    fun parsesOnlyConfirmedCashUsageWithoutStockDetails() {
        val result = parser.parse(
            snapshot(
                FinancialAppRegistry.SAMSUNG_SECURITIES_PACKAGE,
                "삼성전자 매수 체결\n예수금 사용액 700,000원"
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(700_000)
        assertThat(draft.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(draft.type).isEqualTo(TransactionType.INVESTMENT)
        assertThat(draft.category).isEqualTo(Category.STOCK)
        assertThat(draft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
        assertThat(draft.confidence).isAtLeast(0.9)
        assertThat(draft.merchant).isNull()
        assertThat(draft.counterparty).isNull()
        assertThat(draft.memo).isEqualTo("예수금 투자 사용")
    }

    @Test
    fun parsesSupportedCashUsageLabelForms() {
        val contents = mapOf(
            "예수금 350,000원 사용 완료" to 350_000L,
            "매수 체결\n사용 예수금: 120,000원" to 120_000L,
            "예수금 차감액 80,000원" to 80_000L,
            "매수 체결\n정산금액 420,000원" to 420_000L,
            "매수 체결\n예수금 사용액 100,000원\n예수금 잔액 900,000원" to 100_000L
        )

        contents.forEach { (text, expected) ->
            val draft = (parser.parse(snapshot(text = text)) as ParseResult.Parsed).draft
            assertThat(draft.amount.won).isEqualTo(expected)
        }
    }

    @Test
    fun ignoresSellOrderBalancePriceAndUnconfirmedNotifications() {
        val contents = listOf(
            "매도 체결 정산금액 420,000원",
            "삼성전자 10주 70,000원 매수 체결",
            "예수금 잔액 1,000,000원",
            "삼성전자 현재가 70,000원 도달",
            "예수금 사용액 100,000원 주문접수",
            "예수금 사용액 100,000원 미체결",
            "예수금 사용액 100,000원 취소",
            "예수금 사용액 100,000원 예정",
            "매수 주문 확인\n예수금 사용액 100,000원"
        )

        contents.forEach { text ->
            assertThat(parser.parse(snapshot(text = text)) is ParseResult.Ignored).isTrue()
        }
    }

    @Test
    fun ignoresAmbiguousCashUsageAmountsAndUnsupportedPackage() {
        assertThat(
            parser.parse(snapshot(text = "예수금 사용액 100,000원\n예수금 차감액 90,000원"))
        ).isEqualTo(ParseResult.Ignored("ambiguous cash usage amount"))
        assertThat(
            parser.parse(snapshot(packageName = "com.future.securities", text = "예수금 사용액 100,000원"))
        ).isEqualTo(ParseResult.Ignored("unsupported package"))
    }

    @Test
    fun currentConfirmedUsageIgnoresHistoricalSellInExpandedText() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = FinancialAppRegistry.KIWOOM_SECURITIES_PACKAGE,
                title = "매수 체결",
                text = "예수금 사용액 100,000원 완료",
                bigText = "이전 매도 체결 정산금액 90,000원",
                postedAt = Instant.parse("2026-07-21T01:00:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(100_000)
        assertThat(draft.type).isEqualTo(TransactionType.INVESTMENT)
    }

    private fun snapshot(
        packageName: String = FinancialAppRegistry.KIWOOM_SECURITIES_PACKAGE,
        text: String
    ) = NotificationSnapshot(
        packageName = packageName,
        title = "투자 알림",
        text = text,
        bigText = null,
        postedAt = Instant.parse("2026-07-21T01:00:00Z")
    )
}
