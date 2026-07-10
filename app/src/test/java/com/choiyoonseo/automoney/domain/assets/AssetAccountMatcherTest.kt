package com.choiyoonseo.automoney.domain.assets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssetAccountMatcherTest {
    @Test
    fun missingAccountReturnsMissing() {
        val match = matchAssetAccount(emptyList(), hint(BankProvider.KB, "4567", AccountMovementDirection.DEBIT))

        assertThat(match).isEqualTo(AssetAccountMatch.Missing)
    }

    @Test
    fun exactBankProviderAndSuffixMatchesSingleBankAccount() {
        val account = account(1, BankProvider.KB, "4567")

        val match = matchAssetAccount(listOf(account), hint(BankProvider.KB, "4567", AccountMovementDirection.DEBIT))

        assertThat(match).isEqualTo(AssetAccountMatch.Matched(account))
    }

    @Test
    fun wrongBankDoesNotMatchSameSuffix() {
        val match = matchAssetAccount(
            listOf(account(1, BankProvider.KB, "4567")),
            hint(BankProvider.SHINHAN, "4567", AccountMovementDirection.DEBIT)
        )

        assertThat(match).isEqualTo(AssetAccountMatch.Missing)
    }

    @Test
    fun nonBankAccountDoesNotMatch() {
        val account = account(1, BankProvider.KB, "4567").copy(kind = AssetAccountKind.PAY)

        val match = matchAssetAccount(listOf(account), hint(BankProvider.KB, "4567", AccountMovementDirection.DEBIT))

        assertThat(match).isEqualTo(AssetAccountMatch.Missing)
    }

    @Test
    fun duplicateSuffixInSameBankIsAmbiguous() {
        val match = matchAssetAccount(
            listOf(account(1, BankProvider.KB, "4567"), account(2, BankProvider.KB, "4567")),
            hint(BankProvider.KB, "4567", AccountMovementDirection.DEBIT)
        )

        assertThat(match).isEqualTo(AssetAccountMatch.Ambiguous)
    }
}

internal fun account(
    id: Long,
    provider: BankProvider,
    last4: String,
    balanceWon: Long = 100_000
) = AssetAccount(
    id = id,
    name = provider.displayName,
    balanceWon = balanceWon,
    kind = AssetAccountKind.BANK,
    bankProvider = provider,
    accountLast4 = last4
)

internal fun hint(
    provider: BankProvider,
    last4: String,
    direction: AccountMovementDirection
) = BankAccountHint(
    provider = provider,
    accountLast4 = last4,
    direction = direction,
    eventKind = BankEventKind.TRANSFER
)
