package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AccountBalanceDecisionTest {
    @Test
    fun missingAccountMovesToReviewWithoutLinkOrEffect() {
        val decision = decideAccountBalance(
            emptyList(),
            hint(BankProvider.KB, "4567", AccountMovementDirection.CREDIT),
            10_000
        )

        assertThat(decision.linkedAssetAccountId).isNull()
        assertThat(decision.balanceImpact).isEqualTo(BalanceImpact.NONE)
        assertThat(decision.reviewReason).isEqualTo(ReviewReason.ACCOUNT_UNMATCHED)
    }

    @Test
    fun ambiguousAccountMovesToReviewWithoutLinkOrEffect() {
        val decision = decideAccountBalance(
            listOf(account(1, BankProvider.KB, "4567"), account(2, BankProvider.KB, "4567")),
            hint(BankProvider.KB, "4567", AccountMovementDirection.CREDIT),
            10_000
        )

        assertThat(decision.linkedAssetAccountId).isNull()
        assertThat(decision.balanceImpact).isEqualTo(BalanceImpact.NONE)
        assertThat(decision.reviewReason).isEqualTo(ReviewReason.ACCOUNT_AMBIGUOUS)
    }

    @Test
    fun creditLinksMatchedAccountAndAppliesCredit() {
        val decision = decideAccountBalance(
            listOf(account(1, BankProvider.KB, "4567")),
            hint(BankProvider.KB, "4567", AccountMovementDirection.CREDIT),
            10_000
        )

        assertThat(decision.linkedAssetAccountId).isEqualTo(1)
        assertThat(decision.balanceImpact).isEqualTo(BalanceImpact.CREDIT)
        assertThat(decision.reviewReason).isNull()
    }

    @Test
    fun debitLinksMatchedAccountAndAppliesDebitWhenBalanceSufficient() {
        val decision = decideAccountBalance(
            listOf(account(1, BankProvider.KB, "4567", 10_000)),
            hint(BankProvider.KB, "4567", AccountMovementDirection.DEBIT),
            10_000
        )

        assertThat(decision.linkedAssetAccountId).isEqualTo(1)
        assertThat(decision.balanceImpact).isEqualTo(BalanceImpact.DEBIT)
        assertThat(decision.reviewReason).isNull()
    }

    @Test
    fun unknownDirectionMovesToReviewWithoutLinkOrEffect() {
        val decision = decideAccountBalance(
            listOf(account(1, BankProvider.KB, "4567")),
            hint(BankProvider.KB, "4567", AccountMovementDirection.UNKNOWN),
            10_000
        )

        assertThat(decision.linkedAssetAccountId).isNull()
        assertThat(decision.balanceImpact).isEqualTo(BalanceImpact.NONE)
        assertThat(decision.reviewReason).isEqualTo(ReviewReason.ACCOUNT_MOVEMENT_UNKNOWN)
    }

    @Test
    fun debitOverBalanceMovesToReviewWithoutLinkOrEffect() {
        val decision = decideAccountBalance(
            listOf(account(1, BankProvider.KB, "4567", 5_000)),
            hint(BankProvider.KB, "4567", AccountMovementDirection.DEBIT),
            10_000
        )

        assertThat(decision.linkedAssetAccountId).isNull()
        assertThat(decision.balanceImpact).isEqualTo(BalanceImpact.NONE)
        assertThat(decision.reviewReason).isEqualTo(ReviewReason.BALANCE_MISMATCH)
    }
}
