package com.choiyoonseo.automoney.domain.review

import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
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
        val transfer = applyAccountTransfer(
            accounts = accounts,
            fromAccountName = fromAccountName,
            toAccountName = toAccountName,
            amountWon = amountWon
        )
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
}
