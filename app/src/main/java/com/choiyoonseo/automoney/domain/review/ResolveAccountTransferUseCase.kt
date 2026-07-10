package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.AccountTransferResult
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.domain.assets.applyAccountTransfer
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.OpenReviewItem

data class AccountTransferReviewResult(
    val fromAccount: AssetAccount,
    val toAccount: AssetAccount,
    val transaction: MoneyTransaction,
    val pairedTransaction: MoneyTransaction?
)

class ResolveAccountTransferUseCase(
    private val repository: MoneyRepository
) {
    suspend fun resolve(
        accounts: List<AssetAccount>,
        reviewItemId: Long,
        transaction: MoneyTransaction,
        fromAccountName: String,
        toAccountName: String,
        amountWon: Long,
        pairedIncomingReviewItem: OpenReviewItem?
    ): AccountTransferReviewResult {
        val transfer = if (transaction.balanceImpact == null) {
            applyAccountTransfer(
                accounts = accounts,
                fromAccountName = fromAccountName,
                toAccountName = toAccountName,
                amountWon = amountWon
            )
        } else {
            explicitTransferAccounts(
                accounts = accounts,
                transaction = transaction,
                fromAccountName = fromAccountName,
                toAccountName = toAccountName,
                pairedIncomingReviewItem = pairedIncomingReviewItem
            )
        }
        val transferMemo = "$fromAccountName -> $toAccountName"
        val updatedTransaction = transaction.resolveReview(
            resolution = ReviewResolution.ACCOUNT_TRANSFER,
            userMemo = transferMemo
        )
        val updatedPairedTransaction = pairedIncomingReviewItem?.transaction?.resolveReview(
            resolution = ReviewResolution.ACCOUNT_TRANSFER,
            userMemo = "paired $transferMemo"
        )

        repository.resolveAccountTransferReview(
            reviewItemId = reviewItemId,
            transaction = updatedTransaction,
            fromAccount = transfer.fromAccount,
            toAccount = transfer.toAccount,
            pairedReviewItemId = pairedIncomingReviewItem?.id,
            pairedTransaction = updatedPairedTransaction
        )

        return AccountTransferReviewResult(
            fromAccount = transfer.fromAccount,
            toAccount = transfer.toAccount,
            transaction = updatedTransaction,
            pairedTransaction = updatedPairedTransaction
        )
    }

    private fun explicitTransferAccounts(
        accounts: List<AssetAccount>,
        transaction: MoneyTransaction,
        fromAccountName: String,
        toAccountName: String,
        pairedIncomingReviewItem: OpenReviewItem?
    ): AccountTransferResult {
        require(fromAccountName != toAccountName) { "출금 계좌와 입금 계좌가 달라야 해요." }
        val fromAccount = accounts.firstOrNull { it.name == fromAccountName }
            ?: throw IllegalArgumentException("출금 계좌를 찾을 수 없어요.")
        val toAccount = accounts.firstOrNull { it.name == toAccountName }
            ?: throw IllegalArgumentException("입금 계좌를 찾을 수 없어요.")
        val paired = requireNotNull(pairedIncomingReviewItem) {
            "입금 알림을 확인한 뒤 내 계좌 이동으로 처리해 주세요."
        }
        require(transaction.amount == paired.transaction.amount) {
            "출금과 입금 금액이 같아야 해요."
        }
        require(
            setOf(transaction.balanceImpact, paired.transaction.balanceImpact) ==
                setOf(BalanceImpact.DEBIT, BalanceImpact.CREDIT)
        ) {
            "출금과 입금 방향을 확인해 주세요."
        }

        val debit = listOf(transaction, paired.transaction)
            .single { it.balanceImpact == BalanceImpact.DEBIT }
        val credit = listOf(transaction, paired.transaction)
            .single { it.balanceImpact == BalanceImpact.CREDIT }
        require(
            debit.linkedAssetAccountId == fromAccount.id &&
                credit.linkedAssetAccountId == toAccount.id
        ) {
            "선택한 계좌가 출금·입금 알림의 연결 계좌와 일치해야 해요."
        }

        return AccountTransferResult(fromAccount = fromAccount, toAccount = toAccount)
    }
}
