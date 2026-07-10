package com.choiyoonseo.automoney.domain.assets

import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class AssetBalanceSyncTest {
    @Test
    fun expenseSubtractsFromMatchedAccount() {
        val accounts = listOf(AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 100_000))

        val updated = applyTransactionBalance(accounts, transaction(amountWon = 12_000, paymentMethod = "KB"))

        assertThat(updated.single().balanceWon).isEqualTo(88_000)
    }

    @Test
    fun incomeAddsToMatchedAccount() {
        val accounts = listOf(AssetAccount(id = 1, name = "\ud1a0\uc2a4\ubc45\ud06c", balanceWon = 100_000))

        val updated = applyTransactionBalance(
            accounts,
            transaction(
                amountWon = 50_000,
                direction = TransactionDirection.INCOME,
                type = TransactionType.INCOME,
                paymentMethod = "\ud1a0\uc2a4"
            )
        )

        assertThat(updated.single().balanceWon).isEqualTo(150_000)
    }

    @Test
    fun walletSpendSubtractsFromWalletAccount() {
        val accounts = listOf(AssetAccount(id = 1, name = "\ub124\uc774\ubc84\ud398\uc774", balanceWon = 10_000))

        val updated = applyTransactionBalance(
            accounts,
            transaction(
                amountWon = 6_000,
                type = TransactionType.WALLET_SPEND,
                paymentMethod = "\ub124\uc774\ubc84\ud398\uc774"
            )
        )

        assertThat(updated.single().balanceWon).isEqualTo(4_000)
    }

    @Test
    fun needsReviewTransactionDoesNotChangeBalanceUntilResolved() {
        val accounts = listOf(AssetAccount(id = 1, name = "\uce74\uce74\uc624\ubc45\ud06c", balanceWon = 100_000))

        val updated = applyTransactionBalance(
            accounts,
            transaction(paymentMethod = "\uce74\uce74\uc624", status = TransactionStatus.NEEDS_REVIEW)
        )

        assertThat(updated.single().balanceWon).isEqualTo(100_000)
    }

    @Test
    fun explicitDebitUsesIdEvenDuringReview() {
        val accounts = listOf(
            AssetAccount(id = 1, name = "Salary", balanceWon = 100_000),
            AssetAccount(id = 2, name = "Living", balanceWon = 50_000)
        )
        val transaction = transaction(
            amountWon = 10_000,
            status = TransactionStatus.NEEDS_REVIEW
        ).copy(
            linkedAssetAccountId = 2,
            balanceImpact = BalanceImpact.DEBIT
        )

        val updated = applyTransactionBalance(accounts, transaction)

        assertThat(updated.map(AssetAccount::balanceWon))
            .containsExactly(100_000L, 40_000L).inOrder()
    }

    @Test
    fun explicitNoneNeverFallsBackToNameMatching() {
        val accounts = listOf(AssetAccount(id = 1, name = "KB", balanceWon = 100_000))
        val transaction = transaction(paymentMethod = "KB")
            .copy(balanceImpact = BalanceImpact.NONE)

        assertThat(applyTransactionBalance(accounts, transaction)).isEqualTo(accounts)
    }

    @Test
    fun replacingExplicitCreditValidatesOnlyFinalBalance() {
        val accounts = listOf(AssetAccount(id = 1, name = "Living", balanceWon = 50))
        val old = transaction(amountWon = 100).copy(
            linkedAssetAccountId = 1,
            balanceImpact = BalanceImpact.CREDIT
        )
        val new = old.copy(amount = MoneyAmount(50))

        val updated = replaceTransactionBalance(accounts, oldTransaction = old, newTransaction = new)

        assertThat(updated.single().balanceWon).isEqualTo(0)
    }

    @Test
    fun reviewedWalletTopupMovesBalanceFromSourceAccountToWalletAccount() {
        val accounts = listOf(
            AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 100_000),
            AssetAccount(id = 2, name = "\ub124\uc774\ubc84\ud398\uc774", balanceWon = 0)
        )

        val topup = transaction(
            amountWon = 10_000,
            direction = TransactionDirection.NEUTRAL,
            type = TransactionType.WALLET_TOPUP,
            paymentMethod = "\uad6d\ubbfc\uc740\ud589",
            merchant = "\ub124\uc774\ubc84\ud398\uc774",
            status = TransactionStatus.USER_EDITED
        )

        assertThat(topup.type).isEqualTo(TransactionType.WALLET_TOPUP)
        assertThat(topup.status).isEqualTo(TransactionStatus.USER_EDITED)
        assertThat(topup.merchant).isEqualTo("\ub124\uc774\ubc84\ud398\uc774")
        assertThat(topup.amount.won).isEqualTo(10_000)

        val updated = applyTransactionBalance(accounts, topup)

        assertThat(updated.map { it.balanceWon }).containsExactly(90_000L, 10_000L).inOrder()
    }

    @Test
    fun walletTopupDoesNotMoveBalanceUntilReviewIsResolved() {
        val accounts = listOf(
            AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 100_000),
            AssetAccount(id = 2, name = "\ub124\uc774\ubc84\ud398\uc774", balanceWon = 0)
        )

        val updated = applyTransactionBalance(
            accounts,
            transaction(
                amountWon = 10_000,
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.WALLET_TOPUP,
                paymentMethod = "\uad6d\ubbfc\uc740\ud589",
                merchant = "\ub124\uc774\ubc84\ud398\uc774",
                status = TransactionStatus.NEEDS_REVIEW
            )
        )

        assertThat(updated.map { it.balanceWon }).containsExactly(100_000L, 0L).inOrder()
    }

    @Test
    fun walletTopupMoveRequiresBothSourceAndWalletAccounts() {
        val accounts = listOf(
            AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 100_000)
        )

        val updated = applyTransactionBalance(
            accounts,
            transaction(
                amountWon = 10_000,
                direction = TransactionDirection.NEUTRAL,
                type = TransactionType.WALLET_TOPUP,
                paymentMethod = "\uad6d\ubbfc\uc740\ud589",
                merchant = "\ub124\uc774\ubc84\ud398\uc774",
                status = TransactionStatus.USER_EDITED
            )
        )

        assertThat(updated.single().balanceWon).isEqualTo(100_000)
    }

    @Test
    fun replacingWalletTopupWithExcludedTransactionRestoresMovedBalance() {
        val accounts = listOf(
            AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 90_000),
            AssetAccount(id = 2, name = "\ub124\uc774\ubc84\ud398\uc774", balanceWon = 10_000)
        )
        val oldTopup = transaction(
            amountWon = 10_000,
            direction = TransactionDirection.NEUTRAL,
            type = TransactionType.WALLET_TOPUP,
            paymentMethod = "\uad6d\ubbfc\uc740\ud589",
            merchant = "\ub124\uc774\ubc84\ud398\uc774",
            status = TransactionStatus.USER_EDITED
        )
        val excluded = oldTopup.copy(
            type = TransactionType.EXCLUDED,
            status = TransactionStatus.EXCLUDED
        )

        val updated = replaceTransactionBalance(accounts, oldTransaction = oldTopup, newTransaction = excluded)

        assertThat(updated.map { it.balanceWon }).containsExactly(100_000L, 0L).inOrder()
    }

    @Test
    fun replacingTransactionReversesPreviousEffectBeforeApplyingNewEffect() {
        val accounts = listOf(AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 88_000))
        val old = transaction(amountWon = 12_000, paymentMethod = "\uad6d\ubbfc")
        val new = transaction(amountWon = 5_000, paymentMethod = "\uad6d\ubbfc")

        val updated = replaceTransactionBalance(accounts, oldTransaction = old, newTransaction = new)

        assertThat(updated.single().balanceWon).isEqualTo(95_000)
    }

    @Test
    fun removingTransactionRestoresPreviousEffect() {
        val accounts = listOf(AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 88_000))

        val updated = removeTransactionBalance(
            accounts,
            transaction(amountWon = 12_000, paymentMethod = "\uad6d\ubbfc")
        )

        assertThat(updated.single().balanceWon).isEqualTo(100_000)
    }

    @Test
    fun autoConfirmedExpenseWithUnmatchedPaymentMethodNeedsAccountReview() {
        val accounts = listOf(AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 100_000))

        val needsReview = needsAccountMatchReview(
            accounts = accounts,
            transaction = transaction(paymentMethod = "\uc2e0\ud55c\uce74\ub4dc")
        )

        assertThat(needsReview).isTrue()
    }

    @Test
    fun matchedOrAlreadyReviewTransactionsDoNotNeedAccountReview() {
        val accounts = listOf(AssetAccount(id = 1, name = "\uad6d\ubbfc\uc740\ud589", balanceWon = 100_000))

        assertThat(needsAccountMatchReview(accounts, transaction(paymentMethod = "\uad6d\ubbfc"))).isFalse()
        assertThat(
            needsAccountMatchReview(
                accounts,
                transaction(paymentMethod = "\uc2e0\ud55c\uce74\ub4dc", status = TransactionStatus.NEEDS_REVIEW)
            )
        ).isFalse()
    }

    private fun transaction(
        amountWon: Long = 10_000,
        direction: TransactionDirection = TransactionDirection.EXPENSE,
        type: TransactionType = TransactionType.EXPENSE,
        paymentMethod: String? = "\uad6d\ubbfc\uc740\ud589",
        merchant: String? = "\ud14c\uc2a4\ud2b8",
        status: TransactionStatus = TransactionStatus.AUTO_CONFIRMED
    ) = MoneyTransaction(
        occurredAt = Instant.parse("2026-07-01T01:00:00Z"),
        amount = MoneyAmount(amountWon),
        direction = direction,
        type = type,
        category = Category.OTHER,
        paymentMethod = paymentMethod,
        merchant = merchant,
        counterparty = null,
        memo = null,
        sourceApp = null,
        sourceType = SourceType.MANUAL,
        sourceNotificationHash = null,
        status = status,
        confidence = 1.0,
        monthKey = YearMonth.of(2026, 7)
    )
}
