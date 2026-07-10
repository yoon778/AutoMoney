package com.choiyoonseo.automoney.domain.parser

import com.choiyoonseo.automoney.domain.assets.AccountMovementDirection
import com.choiyoonseo.automoney.domain.assets.BankAccountHint
import com.choiyoonseo.automoney.domain.assets.BankEventKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BankAccountHintExtractorTest {
    private val extractor = BankAccountHintExtractor()

    @Test
    fun extractsUniqueWithdrawalHint() {
        val movement = extractor.extract(
            provider = BankProvider.KB,
            text = "계좌 123-***-4567\n10,000원 출금"
        )

        assertThat(movement?.amountWon).isEqualTo(10_000)
        assertThat(movement?.hint).isEqualTo(
            BankAccountHint(
                provider = BankProvider.KB,
                accountLast4 = "4567",
                direction = AccountMovementDirection.DEBIT,
                eventKind = BankEventKind.WITHDRAWAL
            )
        )
    }

    @Test
    fun extractsTransactionAmountInsteadOfBalanceOnSeparateLine() {
        val movement = extractor.extract(
            provider = BankProvider.KB,
            text = "잔액 90,000원\n계좌 123-***-4567\n10,000원 출금"
        )

        assertThat(movement?.amountWon).isEqualTo(10_000)
    }

    @Test
    fun extractsExplicitIncomingTransfer() {
        val movement = extractor.extract(
            provider = BankProvider.SHINHAN,
            text = "통장: 110-***-4567\n김민수님에게 10,000원 송금받았어요"
        )

        assertThat(movement?.hint?.direction).isEqualTo(AccountMovementDirection.CREDIT)
        assertThat(movement?.hint?.eventKind).isEqualTo(BankEventKind.TRANSFER)
    }

    @Test
    fun rejectsMovementLineWithTwoAmounts() {
        assertThat(
            extractor.extract(
                BankProvider.KB,
                "계좌 123-***-4567 10,000원 출금 잔액 90,000원"
            )
        ).isNull()
    }

    @Test
    fun rejectsMissingOrConflictingDirection() {
        assertThat(
            extractor.extract(BankProvider.KB, "계좌 123-***-4567\nATM 10,000원")
        ).isNull()
        assertThat(
            extractor.extract(BankProvider.KB, "계좌 123-***-4567\n10,000원 입금 출금")
        ).isNull()
    }

    @Test
    fun rejectsZeroAmountAndMultipleAccountSuffixes() {
        assertThat(
            extractor.extract(BankProvider.KB, "계좌 123-***-4567\n0원 출금")
        ).isNull()
        assertThat(
            extractor.extract(
                BankProvider.KB,
                "계좌 123-***-4567\n계좌 987-***-7654\n10,000원 출금"
            )
        ).isNull()
    }

    @Test
    fun aggregatorRequiresOneExplicitBankName() {
        assertThat(extractor.resolveAggregatorProvider("10,000원 송금했어요")).isNull()
        assertThat(
            extractor.resolveAggregatorProvider("토스뱅크 계좌 123-***-4567 10,000원 출금")
        ).isEqualTo(BankProvider.TOSS_BANK)
        assertThat(
            extractor.resolveAggregatorProvider("토스뱅크와 카카오뱅크 안내")
        ).isNull()
    }
}
