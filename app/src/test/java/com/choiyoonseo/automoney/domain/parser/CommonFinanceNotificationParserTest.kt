package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.assets.AccountMovementDirection
import com.choiyoonseo.automoney.domain.assets.BankEventKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.notification.FinancialAppRegistry
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class CommonFinanceNotificationParserTest {
    private val parser = CommonFinanceNotificationParser()

    @Test
    fun parsesKbCardPaymentAsAutoConfirmedExpense() {
        val result = parser.parse(snapshot(text = "STARBUCKS 6,100${WON} ${APPROVAL}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6100)
        assertThat(draft.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(draft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
        assertThat(draft.merchant).isEqualTo("STARBUCKS")
        assertThat(draft.sourceApp).isEqualTo("com.kbstar.kbbank")
        assertThat(draft.bankAccountHint).isNull()
    }

    @Test
    fun parsesKbCardPaymentWithoutCommaAsAutoConfirmedExpense() {
        val result = parser.parse(snapshot(text = "STARBUCKS 6100${WON} ${APPROVAL}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6100)
        assertThat(draft.merchant).isEqualTo("STARBUCKS")
    }

    @Test
    fun parsesMerchantAfterAmountInPaymentNotification() {
        val result = parser.parse(snapshot(text = "6,100${WON} ${APPROVAL} STARBUCKS"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.merchant).isEqualTo("STARBUCKS")
        assertThat(draft.memo).isEqualTo("STARBUCKS")
    }

    @Test
    fun parsesTransferAsNeedsReview() {
        val result = parser.parse(snapshot(text = "Kim 10,000${WON} ${TRANSFER}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.TRANSFER_UNKNOWN)
    }

    @Test
    fun parsesCounterpartyAfterAmountInWithdrawalNotification() {
        val result = parser.parse(snapshot(text = "10,000${WON} ${WITHDRAWAL} 김민수"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(draft.counterparty).isEqualTo("김민수")
        assertThat(draft.memo).contains("김민수")
    }

    @Test
    fun parsesDepositAsNeedsReviewIncome() {
        val result = parser.parse(snapshot(text = "Company 500,000${WON} ${DEPOSIT}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.INCOME)
        assertThat(draft.direction).isEqualTo(TransactionDirection.INCOME)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.INCOME_UNKNOWN)
    }

    @Test
    fun parsesTopupAsNeedsReviewWalletTopup() {
        val result = parser.parse(snapshot(text = "Naver Pay 10,000${WON} ${TOPUP}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.WALLET_TOPUP)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.WALLET_TOPUP)
    }

    @Test
    fun parsesCancelAsRefundReview() {
        val result = parser.parse(snapshot(text = "Coupang 12,000${WON} ${CANCEL}"))

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.type).isEqualTo(TransactionType.REFUND)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.REFUND_OR_CANCEL)
    }

    @Test
    fun ignoresCouponPromotionWithMoneyAmount() {
        val result = parser.parse(snapshot(text = "Event 10,000${WON} ${COUPON}"))

        assertThat(result).isEqualTo(ParseResult.Ignored("promotional notification"))
    }

    @Test
    fun ignoresPromotionsFailuresAndAvailableBalanceNotifications() {
        listOf(
            "카드 사용 혜택 10,000원",
            "결제 실패 10,000원",
            "승인 거절 10,000원",
            "결제 예정 10,000원",
            "출금가능액 18,501원"
        ).forEach { text ->
            assertThat(parser.parse(snapshot(text = text)))
                .isInstanceOf(ParseResult.Ignored::class.java)
        }
    }

    @Test
    fun cardApprovalWinsOverDateAndWithdrawalTokens() {
        listOf(
            "2026-07-24 승인 17,700원 아이디푸드",
            "출금 승인 17,700원 아이디푸드"
        ).forEach { text ->
            val draft = (parser.parse(snapshot(text = text)) as ParseResult.Parsed).draft

            assertThat(draft.amount.won).isEqualTo(17_700)
            assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
            assertThat(draft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
            assertThat(draft.merchant).isEqualTo("아이디푸드")
        }
    }

    @Test
    fun ignoresUnsupportedPackage() {
        val result = parser.parse(
            snapshot(
                packageName = "com.shopping.adapp",
                text = "STARBUCKS 6,100${WON} ${APPROVAL}"
            )
        )

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported package"))
    }

    @Test
    fun parsesKbankPaymentBeforeSeparateCashbackNotification() {
        val payment = parser.parse(
            snapshot(
                packageName = FinancialAppRegistry.K_BANKING_PACKAGE,
                text = "스타벅스 6,000원 결제 완료"
            )
        )
        val cashback = parser.parse(
            snapshot(
                packageName = FinancialAppRegistry.K_BANKING_PACKAGE,
                text = "캐시백 6원 환급"
            )
        )

        val paymentDraft = (payment as ParseResult.Parsed).draft
        val cashbackDraft = (cashback as ParseResult.Parsed).draft
        assertThat(paymentDraft.amount.won).isEqualTo(6_000)
        assertThat(paymentDraft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(paymentDraft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
        assertThat(cashbackDraft.amount.won).isEqualTo(6)
        assertThat(cashbackDraft.type).isEqualTo(TransactionType.REFUND)
    }

    @Test
    fun parsesKbankMultilineApprovalWithMerchantOnFollowingLine() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = FinancialAppRegistry.K_BANKING_PACKAGE,
                title = "케이뱅크",
                text = "승인 17,700원",
                bigText = "아이디푸드(화서본점)\n" +
                    "카드(7712) | 07/24 09:02\n" +
                    "출금가능액 18,501원",
                postedAt = Instant.parse("2026-07-24T00:02:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(17_700)
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(draft.status).isEqualTo(TransactionStatus.AUTO_CONFIRMED)
        assertThat(draft.merchant).isEqualTo("아이디푸드(화서본점)")
    }

    @Test
    fun doesNotUseNotificationTitleAsMerchantForSelectedPaymentClause() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = FinancialAppRegistry.K_BANKING_PACKAGE,
                title = "케이뱅크",
                text = "승인 17,700원 / 캐시백 10원 환급",
                bigText = null,
                postedAt = Instant.parse("2026-07-24T00:02:00Z")
            )
        )

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported finance notification"))
    }

    @Test
    fun kbankCombinedPaymentAndCashbackNeverTurnsPaymentIntoRefund() {
        val result = parser.parse(
            snapshot(
                packageName = FinancialAppRegistry.K_BANKING_PACKAGE,
                text = "스타벅스 6,000원 결제 완료\n캐시백 6원 환급"
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6_000)
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
    }

    @Test
    fun kbankSameLinePaymentAndCashbackKeepsPaymentAmount() {
        val result = parser.parse(
            snapshot(
                packageName = FinancialAppRegistry.K_BANKING_PACKAGE,
                text = "스타벅스 6,000원 결제 / 캐시백 6원 환급"
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6_000)
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
    }

    @Test
    fun availableAmountLineDoesNotOverrideAccountWithdrawal() {
        val result = parser.parse(
            snapshot(
                packageName = FinancialAppRegistry.K_BANKING_PACKAGE,
                text = "계좌 123-***-4567\n10,000원 출금\n사용가능금액 90,000원"
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(10_000)
        assertThat(draft.bankAccountHint?.eventKind).isEqualTo(BankEventKind.WITHDRAWAL)
    }

    @Test
    fun kbankUpdatedPrimaryCashbackIgnoresOldExpandedPayment() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = FinancialAppRegistry.K_BANKING_PACKAGE,
                title = "케이뱅크",
                text = "캐시백 6원 환급",
                bigText = "스타벅스 6,000원 결제 완료\n캐시백 6원 환급",
                postedAt = Instant.parse("2026-07-03T01:00:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(6)
        assertThat(draft.type).isEqualTo(TransactionType.REFUND)
    }

    @Test
    fun syntheticDedicatedPackageFixturesProduceTheirMappedHints() {
        dedicatedBankFixtures.forEach { fixture ->
            val result = parser.parse(
                snapshot(
                    packageName = fixture.packageName,
                    text = "계좌 123-***-4567\n10,000원 출금"
                )
            )

            val draft = (result as ParseResult.Parsed).draft
            assertThat(draft.bankAccountHint?.provider).isEqualTo(fixture.provider)
            assertThat(draft.bankAccountHint?.accountLast4).isEqualTo("4567")
            assertThat(draft.bankAccountHint?.direction).isEqualTo(AccountMovementDirection.DEBIT)
            assertThat(draft.bankAccountHint?.eventKind).isEqualTo(BankEventKind.WITHDRAWAL)
        }
    }

    @Test
    fun parsesKakaoBankParenthesizedAccountSuffix() {
        val result = parser.parse(
            NotificationSnapshot(
                packageName = FinancialAppRegistry.KAKAO_BANKING_PACKAGE,
                title = "출금 20,000원",
                text = "입출금통장(0303) → 주택청약",
                bigText = null,
                postedAt = Instant.parse("2026-07-12T05:06:00Z")
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(20_000)
        assertThat(draft.bankAccountHint).isEqualTo(
            com.choiyoonseo.automoney.domain.assets.BankAccountHint(
                provider = BankProvider.KAKAO_BANK,
                accountLast4 = "0303",
                direction = AccountMovementDirection.DEBIT,
                eventKind = BankEventKind.WITHDRAWAL
            )
        )
    }

    @Test
    fun movementUsesTransactionAmountAndStableSnapshotHash() {
        val notification = NotificationSnapshot(
            packageName = FinancialAppRegistry.KB_STAR_BANKING_PACKAGE,
            title = "잔액 90,000원",
            text = "계좌 123-***-4567\n10,000원 출금",
            bigText = null,
            postedAt = Instant.parse("2026-07-03T01:00:00Z"),
            notificationKey = "movement-key"
        )

        val draft = (parser.parse(notification) as ParseResult.Parsed).draft

        assertThat(draft.amount.won).isEqualTo(10_000)
        assertThat(draft.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(draft.direction).isEqualTo(TransactionDirection.EXPENSE)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN)
        assertThat(draft.sourceNotificationHash).isEqualTo(notification.sourceNotificationHash)
    }

    @Test
    fun ignoresAmbiguousAccountMovementRatherThanUsingBalanceAmount() {
        val result = parser.parse(
            snapshot(
                text = "계좌 123-***-4567 10,000원 출금 잔액 90,000원"
            )
        )

        assertThat(result).isEqualTo(ParseResult.Ignored("ambiguous bank movement"))
    }

    @Test
    fun ambiguousMovementWithSingleAmountBecomesReviewTransfer() {
        // 실기기 KB 오픈뱅킹출금 알림의 비식별화 fixture —
        // title과 body 두 줄 모두 출금 keyword가 있어 movement extractor가 특정 실패,
        // 잔액에 원 표기가 없어 금액은 1개뿐
        val result = parser.parse(
            snapshot(
                text = "${WITHDRAWAL} 30,000${WON}\n" +
                    "김*수님 07/13 ****1234-**-***567 오픈뱅킹${WITHDRAWAL} 30,000 잔액20,880"
            )
        )

        val draft = (result as ParseResult.Parsed).draft
        assertThat(draft.amount.won).isEqualTo(30000)
        assertThat(draft.type).isEqualTo(TransactionType.TRANSFER)
        assertThat(draft.status).isEqualTo(TransactionStatus.NEEDS_REVIEW)
        assertThat(draft.reviewReason).isEqualTo(ReviewReason.TRANSFER_UNKNOWN)
        assertThat(draft.bankAccountHint).isNull()
    }

    @Test
    fun nearMatchPackageNeverProducesHint() {
        val result = parser.parse(
            snapshot(
                packageName = "com.kbstar.kbbank.fake",
                text = "계좌 123-***-4567\n10,000원 출금"
            )
        )

        assertThat(result).isEqualTo(ParseResult.Ignored("unsupported package"))
    }

    private fun snapshot(
        packageName: String = "com.kbstar.kbbank",
        text: String
    ) = NotificationSnapshot(
        packageName = packageName,
        title = "KB",
        text = text,
        bigText = null,
        postedAt = Instant.parse("2026-07-03T01:00:00Z")
    )

    companion object {
        private val dedicatedBankFixtures = listOf(
            DedicatedBankFixture(FinancialAppRegistry.KB_STAR_BANKING_PACKAGE, BankProvider.KB),
            DedicatedBankFixture(FinancialAppRegistry.SHINHAN_BANKING_PACKAGE, BankProvider.SHINHAN),
            DedicatedBankFixture(FinancialAppRegistry.SHINHAN_LEGACY_PACKAGE, BankProvider.SHINHAN),
            DedicatedBankFixture(FinancialAppRegistry.HANA_BANKING_PACKAGE, BankProvider.HANA),
            DedicatedBankFixture(FinancialAppRegistry.HANA_LEGACY_PACKAGE, BankProvider.HANA),
            DedicatedBankFixture(FinancialAppRegistry.WOORI_BANKING_PACKAGE, BankProvider.WOORI),
            DedicatedBankFixture(FinancialAppRegistry.NH_BANKING_PACKAGE, BankProvider.NH),
            DedicatedBankFixture(FinancialAppRegistry.IBK_BANKING_PACKAGE, BankProvider.IBK),
            DedicatedBankFixture(FinancialAppRegistry.KAKAO_BANKING_PACKAGE, BankProvider.KAKAO_BANK),
            DedicatedBankFixture(FinancialAppRegistry.K_BANKING_PACKAGE, BankProvider.K_BANK),
            DedicatedBankFixture(FinancialAppRegistry.SC_BANKING_PACKAGE, BankProvider.SC),
            DedicatedBankFixture(FinancialAppRegistry.IM_BANKING_PACKAGE, BankProvider.IM_BANK),
            DedicatedBankFixture(FinancialAppRegistry.BNK_BUSAN_BANKING_PACKAGE, BankProvider.BNK_BUSAN),
            DedicatedBankFixture(FinancialAppRegistry.BNK_BUSAN_PUSH_PACKAGE, BankProvider.BNK_BUSAN)
        )

        private const val WON = "\uc6d0"
        private const val APPROVAL = "\uc2b9\uc778"
        private const val TRANSFER = "\uc774\uccb4"
        private const val WITHDRAWAL = "\ucd9c\uae08"
        private const val DEPOSIT = "\uc785\uae08"
        private const val TOPUP = "\ucda9\uc804"
        private const val CANCEL = "\ucde8\uc18c"
        private const val COUPON = "\ucfe0\ud3f0"
    }

    private data class DedicatedBankFixture(
        val packageName: String,
        val provider: BankProvider
    )
}
