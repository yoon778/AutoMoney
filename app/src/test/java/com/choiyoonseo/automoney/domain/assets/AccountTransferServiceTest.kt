package com.choiyoonseo.automoney.domain.assets

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AccountTransferServiceTest {

    @Test
    fun applyAccountTransferMovesBalanceBetweenExistingAccounts() {
        val accounts = listOf(
            AssetAccount(id = 1, name = "KB", balanceWon = 100_000),
            AssetAccount(id = 2, name = "Kakao", balanceWon = 50_000)
        )

        val result = applyAccountTransfer(
            accounts = accounts,
            fromAccountName = "KB",
            toAccountName = "Kakao",
            amountWon = 30_000
        )

        assertThat(result.fromAccount.balanceWon).isEqualTo(70_000)
        assertThat(result.toAccount.balanceWon).isEqualTo(80_000)
    }

    @Test
    fun applyAccountTransferRejectsSameAccount() {
        val accounts = listOf(
            AssetAccount(id = 1, name = "KB", balanceWon = 100_000)
        )

        val error = kotlin.runCatching {
            applyAccountTransfer(
                accounts = accounts,
                fromAccountName = "KB",
                toAccountName = "KB",
                amountWon = 10_000
            )
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }
}
