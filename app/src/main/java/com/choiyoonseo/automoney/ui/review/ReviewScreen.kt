package com.choiyoonseo.automoney.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.R
import com.choiyoonseo.automoney.data.repository.AssetRepository
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.applyAccountTransfer
import com.choiyoonseo.automoney.domain.assets.findAccountTransferCandidates
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.review.RecordWalletTopupUsageUseCase
import com.choiyoonseo.automoney.domain.review.ReviewResolution
import com.choiyoonseo.automoney.domain.review.WalletTopupReviewService
import com.choiyoonseo.automoney.domain.review.WalletTopupUsageResult
import com.choiyoonseo.automoney.domain.review.resolveReview
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.ui.components.EmptyStateVisual
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.IllustratedSummaryCard
import com.choiyoonseo.automoney.ui.components.MoneyCanvas
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.MoneyMuted
import com.choiyoonseo.automoney.ui.components.ReviewActionCard
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import com.choiyoonseo.automoney.ui.components.TransactionEditDialog
import com.choiyoonseo.automoney.ui.components.reviewAccentForLabel
import com.choiyoonseo.automoney.ui.model.ReviewCardKind
import com.choiyoonseo.automoney.ui.model.ReviewCardUi
import com.choiyoonseo.automoney.ui.model.dismissReviewCard
import com.choiyoonseo.automoney.ui.model.formatWon
import com.choiyoonseo.automoney.ui.model.openReviewItemsToCards
import com.choiyoonseo.automoney.ui.model.sampleReviewCards
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth

@Composable
fun ReviewScreen(
    padding: PaddingValues,
    recordWalletTopupUsageUseCase: RecordWalletTopupUsageUseCase? = null,
    moneyRepository: MoneyRepository? = null,
    editTransactionUseCase: EditTransactionUseCase? = null,
    assetRepository: AssetRepository? = null
) {
    val scope = rememberCoroutineScope()
    val reviewService = remember { WalletTopupReviewService() }
    val openReviewItems by remember(moneyRepository) {
        moneyRepository?.observeOpenReviewItems() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val dbReviewCards = remember(openReviewItems) { openReviewItemsToCards(openReviewItems) }
    val accountTransferCandidates = remember(openReviewItems) {
        findAccountTransferCandidates(openReviewItems.map { it.transaction })
    }
    val assetAccounts by remember(assetRepository) {
        assetRepository?.observeAccounts() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    var sampleReviewCardsState by remember { mutableStateOf(sampleReviewCards) }
    val reviewCards = if (moneyRepository == null) sampleReviewCardsState else dbReviewCards
    var activeWalletCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var activeUnusedWalletCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var activeReviewMemoAction by remember { mutableStateOf<ReviewMemoAction?>(null) }
    var activeEditReviewCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var activeAccountTransferCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    suspend fun resolveCard(
        card: ReviewCardUi,
        message: String,
        transactionUpdate: ((MoneyTransaction) -> MoneyTransaction)? = null
    ) {
        if (moneyRepository != null && card.reviewItemId != null) {
            val sourceTransaction = card.sourceTransaction
            if (sourceTransaction != null && transactionUpdate != null) {
                moneyRepository.updateTransaction(transactionUpdate(sourceTransaction))
            }
            moneyRepository.resolveReviewItem(card.reviewItemId)
        } else {
            sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
        }
        resultMessage = message
    }

    fun pairedIncomingReviewItem(card: ReviewCardUi) =
        card.sourceTransaction?.id?.let { transactionId ->
            accountTransferCandidates
                .firstOrNull { it.outgoingTransactionId == transactionId }
                ?.let { candidate ->
                    openReviewItems.firstOrNull { it.transaction.id == candidate.incomingTransactionId }
                }
        }

    fun handleReviewMemoAction(action: ReviewMemoAction, userMemo: String?) {
        scope.launch {
            isSaving = true
            errorMessage = null
            try {
                resolveCard(
                    card = action.card,
                    message = action.resultMessage,
                    transactionUpdate = { transaction ->
                        transaction.resolveReview(action.resolution, userMemo)
                    }
                )
                activeReviewMemoAction = null
            } finally {
                isSaving = false
            }
        }
    }

    fun recordWalletUsage(card: ReviewCardUi, usedWon: Long, merchant: String, memo: String?) {
        val topup = card.sourceTransaction ?: card.toSampleTopupTransaction()
        val category = Category.CAFE_SNACK
        isSaving = true
        errorMessage = null

        scope.launch {
            try {
                val result = if (recordWalletTopupUsageUseCase != null) {
                    recordWalletTopupUsageUseCase.recordUsage(
                        topup = topup,
                        usedAmount = MoneyAmount(usedWon),
                        category = category,
                        merchant = merchant.ifBlank { "실제 사용처" },
                        memo = memo
                    )
                } else {
                    reviewService.recordUsage(
                        topup = topup,
                        usedAmount = MoneyAmount(usedWon),
                        category = category,
                        merchant = merchant.ifBlank { "실제 사용처" },
                        memo = memo
                    )
                }
                if (moneyRepository != null && card.reviewItemId != null) {
                    moneyRepository.resolveReviewItem(card.reviewItemId)
                } else {
                    sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
                }
                resultMessage = result.toSummaryMessage()
                activeWalletCard = null
                activeUnusedWalletCard = null
            } catch (e: IllegalArgumentException) {
                errorMessage = e.message ?: "입력값을 확인해 주세요"
            } finally {
                isSaving = false
            }
        }
    }

    fun recordAccountTransfer(card: ReviewCardUi, fromAccount: AssetAccount, toAccount: AssetAccount) {
        val repository = assetRepository ?: return
        isSaving = true
        errorMessage = null

        scope.launch {
            try {
                val pairedItem = pairedIncomingReviewItem(card)
                val transfer = applyAccountTransfer(
                    accounts = assetAccounts,
                    fromAccountName = fromAccount.name,
                    toAccountName = toAccount.name,
                    amountWon = card.amountWon
                )
                repository.saveAccount(transfer.fromAccount)
                repository.saveAccount(transfer.toAccount)
                if (
                    moneyRepository != null &&
                    pairedItem != null &&
                    pairedItem.id != card.reviewItemId
                ) {
                    moneyRepository.updateTransaction(
                        pairedItem.transaction.resolveReview(
                            resolution = ReviewResolution.ACCOUNT_TRANSFER,
                            userMemo = "paired ${fromAccount.name} -> ${toAccount.name}"
                        )
                    )
                    moneyRepository.resolveReviewItem(pairedItem.id)
                }
                resolveCard(
                    card = card,
                    message = buildString {
                        append("${fromAccount.name}에서 ${toAccount.name}으로 ${formatWon(card.amountWon)} 이동 처리했어요.")
                        if (pairedItem != null) {
                            append(" 입금 알림도 같이 검토 완료했어요.")
                        }
                    },
                    transactionUpdate = { transaction ->
                        transaction.resolveReview(
                            resolution = ReviewResolution.ACCOUNT_TRANSFER,
                            userMemo = "${fromAccount.name} -> ${toAccount.name}"
                        )
                    }
                )
                activeAccountTransferCard = null
            } catch (e: IllegalArgumentException) {
                errorMessage = e.message ?: "계좌 이동값을 확인해 주세요."
            } catch (e: RuntimeException) {
                errorMessage = "계좌 이동 저장 중 문제가 생겼어요."
            } finally {
                isSaving = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MoneyCanvas)
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            title = "오늘 검토",
            subtitle = "송금, 충전, 환불처럼 애매한 거래만 모아서 확인해요"
        )

        resultMessage?.let { message ->
            FinanceSectionCard(
                title = "저장 결과",
                accent = MoneyCoral,
                icon = Icons.Filled.CheckCircle
            ) {
                Text(message)
            }
        }

        IllustratedSummaryCard(
            title = "확인 필요",
            value = "${reviewCards.size}건",
            helper = "놓치지 말고 확인해 주세요",
            accent = MoneyCoral,
            imageRes = R.drawable.illustration_review_magnifier
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewFilterChip("전체 ${reviewCards.size}", selected = true, accent = MoneyCoral, modifier = Modifier.weight(1f))
            ReviewFilterChip("지출 아님", selected = false, accent = MoneyMuted, modifier = Modifier.weight(1f))
            ReviewFilterChip("송금", selected = false, accent = reviewAccentForLabel("송금"), modifier = Modifier.weight(1f))
            ReviewFilterChip("중복", selected = false, accent = reviewAccentForLabel("중복"), modifier = Modifier.weight(1f))
        }

        reviewCards.forEach { card ->
            ReviewActionCard(
                card = card,
                onPrimaryAction = {
                    if (card.kind == ReviewCardKind.WALLET_TOPUP) {
                        activeWalletCard = card
                    } else {
                        activeReviewMemoAction = card.toPrimaryMemoAction()
                    }
                },
                onSecondaryAction = {
                    if (card.kind == ReviewCardKind.WALLET_TOPUP) {
                        activeUnusedWalletCard = card
                    } else {
                        activeReviewMemoAction = card.toSecondaryMemoAction()
                    }
                },
                onAccountTransferAction = if (
                    card.kind == ReviewCardKind.TRANSFER &&
                    assetRepository != null &&
                    assetAccounts.size >= 2
                ) {
                    {
                        activeAccountTransferCard = card
                        errorMessage = null
                    }
                } else {
                    null
                },
                onEditAction = if (card.sourceTransaction != null && editTransactionUseCase != null) {
                    {
                        activeEditReviewCard = card
                        editErrorMessage = null
                    }
                } else {
                    null
                }
            )
        }

        if (reviewCards.isEmpty()) {
            EmptyStateVisual(
                title = "검토가 끝났어요",
                message = "하루 한 번만 확인하면 자동 기록이 더 정확해져요."
            )
        }

        Text("확정 거래는 거래 탭으로 이동해요.", fontWeight = FontWeight.Medium)
    }

    activeWalletCard?.let { card ->
        WalletUsageInputDialog(
            card = card,
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = {
                activeWalletCard = null
                errorMessage = null
            },
            onSave = { usedWon, merchant, memo ->
                recordWalletUsage(card, usedWon, merchant, memo)
            }
        )
    }

    activeUnusedWalletCard?.let { card ->
        WalletUnusedMemoDialog(
            card = card,
            isSaving = isSaving,
            onDismiss = {
                activeUnusedWalletCard = null
                errorMessage = null
            },
            onSave = { memo ->
                recordWalletUsage(card, usedWon = 0, merchant = "미사용", memo = memo)
            }
        )
    }

    activeReviewMemoAction?.let { action ->
        ReviewMemoInputDialog(
            action = action,
            isSaving = isSaving,
            onDismiss = {
                activeReviewMemoAction = null
                errorMessage = null
            },
            onSave = { memo ->
            handleReviewMemoAction(action, memo)
            }
        )
    }

    activeAccountTransferCard?.let { card ->
        AccountTransferDialog(
            card = card,
            accounts = assetAccounts,
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = {
                activeAccountTransferCard = null
                errorMessage = null
            },
            onSave = { fromAccount, toAccount ->
                recordAccountTransfer(card, fromAccount, toAccount)
            }
        )
    }

    activeEditReviewCard?.let { card ->
        val transaction = card.sourceTransaction
        val useCase = editTransactionUseCase
        if (transaction != null && useCase != null) {
            TransactionEditDialog(
                transaction = transaction,
                isSaving = isSaving,
                errorMessage = editErrorMessage,
                accountNames = assetAccounts.map { it.name },
                onDismiss = {
                    activeEditReviewCard = null
                    editErrorMessage = null
                },
                onSave = { amountWon, category, memo, occurredAt, paymentMethod, transactionType ->
                    scope.launch {
                        isSaving = true
                        editErrorMessage = null
                        try {
                            useCase.update(transaction, amountWon, category, memo, occurredAt, paymentMethod, transactionType)
                            if (moneyRepository != null && card.reviewItemId != null) {
                                moneyRepository.resolveReviewItem(card.reviewItemId)
                            } else {
                                sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
                            }
                            resultMessage = "${card.title}을 수정하고 검토 완료했어요."
                            activeEditReviewCard = null
                        } catch (e: IllegalArgumentException) {
                            editErrorMessage = e.message ?: "입력값을 확인해 주세요."
                        } catch (e: RuntimeException) {
                            editErrorMessage = "수정 중 문제가 생겼어요."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        isSaving = true
                        editErrorMessage = null
                        try {
                            if (moneyRepository != null && card.reviewItemId != null) {
                                moneyRepository.resolveReviewItem(card.reviewItemId)
                            }
                            useCase.delete(transaction)
                            if (moneyRepository == null) {
                                sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
                            }
                            resultMessage = "${card.title}을 삭제했어요."
                            activeEditReviewCard = null
                        } catch (e: RuntimeException) {
                            editErrorMessage = "삭제 중 문제가 생겼어요."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                onExclude = {
                    scope.launch {
                        isSaving = true
                        editErrorMessage = null
                        try {
                            useCase.exclude(transaction)
                            if (moneyRepository != null && card.reviewItemId != null) {
                                moneyRepository.resolveReviewItem(card.reviewItemId)
                            } else {
                                sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
                            }
                            resultMessage = "${card.title}을 지출에서 제외했어요."
                            activeEditReviewCard = null
                        } catch (e: RuntimeException) {
                            editErrorMessage = "제외 처리 중 문제가 생겼어요."
                        } finally {
                            isSaving = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ReviewFilterChip(label: String, selected: Boolean, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) accent.copy(alpha = 0.14f) else accent.copy(alpha = 0.07f)
    ) {
        Text(
            text = label,
            color = if (selected) accent else MoneyMuted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun AccountTransferDialog(
    card: ReviewCardUi,
    accounts: List<AssetAccount>,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (fromAccount: AssetAccount, toAccount: AssetAccount) -> Unit
) {
    var fromAccount by remember(card.id, accounts) {
        mutableStateOf<AssetAccount?>(accounts.firstOrNull())
    }
    var toAccount by remember(card.id, accounts) {
        mutableStateOf<AssetAccount?>(accounts.drop(1).firstOrNull() ?: accounts.firstOrNull())
    }
    val canSave = fromAccount != null &&
        toAccount != null &&
        fromAccount?.name != toAccount?.name &&
        !isSaving

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("내 계좌 이동") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${formatWon(card.amountWon)}을 지출이 아닌 계좌 이동으로 처리해요.")
                AccountPicker(
                    label = "출금 계좌",
                    selectedAccount = fromAccount,
                    accounts = accounts,
                    onSelected = { fromAccount = it }
                )
                AccountPicker(
                    label = "입금 계좌",
                    selectedAccount = toAccount,
                    accounts = accounts,
                    onSelected = { toAccount = it }
                )
                errorMessage?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val from = fromAccount
                    val to = toAccount
                    if (from != null && to != null) {
                        onSave(from, to)
                    }
                }
            ) {
                Text(if (isSaving) "저장 중" else "저장")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun AccountPicker(
    label: String,
    selectedAccount: AssetAccount?,
    accounts: List<AssetAccount>,
    onSelected: (AssetAccount) -> Unit
) {
    var expanded by remember(label, accounts) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.Medium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    selectedAccount?.let { "${it.name}  ${formatWon(it.balanceWon)}" } ?: "계좌 없음"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text("${account.name}  ${formatWon(account.balanceWon)}") },
                        onClick = {
                            onSelected(account)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletUsageInputDialog(
    card: ReviewCardUi,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (usedWon: Long, merchant: String, memo: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("6000") }
    var merchant by remember { mutableStateOf("스타벅스 홍대입구") }
    var memo by remember { mutableStateOf("네이버페이 실제 사용") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("실제 사용액 입력") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${card.title} ${formatWon(card.amountWon)} 중 사용한 금액만 지출로 저장해요.")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("사용액") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("사용처") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val usedWon = amountText.replace(",", "").trim().toLongOrNull()
                    if (usedWon != null) {
                        onSave(usedWon, merchant, memo.ifBlank { null })
                    }
                }
            ) {
                Text(if (isSaving) "저장 중" else "저장")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun WalletUnusedMemoDialog(
    card: ReviewCardUi,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (memo: String?) -> Unit
) {
    var memo by remember(card.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("충전 메모 입력") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${card.title} ${formatWon(card.amountWon)}을 아직 지출로 처리하지 않고 보류해요.")
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("충전한 이유 / 아직 안 쓴 이유") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = { onSave(memo.ifBlank { null }) }
            ) {
                Text(if (isSaving) "저장 중" else "저장")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun ReviewMemoInputDialog(
    action: ReviewMemoAction,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (memo: String?) -> Unit
) {
    var memo by remember(action.card.id, action.resolution) {
        mutableStateOf(action.defaultMemo)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(action.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(action.message)
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text(action.memoLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = { onSave(memo.ifBlank { null }) }
            ) {
                Text(if (isSaving) "저장 중" else "저장")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text("취소")
            }
        }
    )
}

private fun ReviewCardUi.toSampleTopupTransaction() = MoneyTransaction(
    occurredAt = Instant.now(),
    amount = MoneyAmount(amountWon),
    direction = TransactionDirection.NEUTRAL,
    type = TransactionType.WALLET_TOPUP,
    category = null,
    paymentMethod = "토스",
    merchant = title.substringBefore(" 충전").ifBlank { title },
    counterparty = null,
    memo = "$title 검토",
    sourceApp = "viva.republica.toss",
    sourceType = SourceType.NOTIFICATION,
    sourceNotificationHash = null,
    status = TransactionStatus.NEEDS_REVIEW,
    confidence = 0.85,
    monthKey = YearMonth.now()
)

private fun WalletTopupUsageResult.toSummaryMessage(): String {
    val spent = walletSpend?.amount?.won ?: 0
    return if (spent == 0L) {
        "충전액 ${formatWon(reviewedTopup.amount.won)}은 지출 처리하지 않고 보류했어요."
    } else {
        "사용 ${formatWon(spent)} 저장 · 잔액 ${formatWon(remainingAmount.won)} 보류"
    }
}

private data class ReviewMemoAction(
    val card: ReviewCardUi,
    val resolution: ReviewResolution,
    val title: String,
    val message: String,
    val memoLabel: String,
    val defaultMemo: String,
    val resultMessage: String
)

private fun ReviewCardUi.toPrimaryMemoAction(): ReviewMemoAction {
    val isIncome = sourceTransaction?.direction == TransactionDirection.INCOME ||
        sourceTransaction?.type == TransactionType.INCOME

    return when {
        kind == ReviewCardKind.TRANSFER -> ReviewMemoAction(
            card = this,
            resolution = ReviewResolution.SETTLEMENT,
            title = "송금 사유 입력",
            message = "${title} ${formatWon(amountWon)}을 N분의1 정산으로 저장해요.",
            memoLabel = "송금 사유",
            defaultMemo = "",
            resultMessage = "${title}을 N분의1 정산으로 저장했어요."
        )

        kind == ReviewCardKind.REFUND -> ReviewMemoAction(
            card = this,
            resolution = ReviewResolution.REFUND,
            title = "환불 메모 입력",
            message = "${title} ${formatWon(amountWon)}을 환불/취소로 저장해요.",
            memoLabel = "환불 사유",
            defaultMemo = "",
            resultMessage = "${title}을 환불/취소로 저장했어요."
        )

        isIncome -> ReviewMemoAction(
            card = this,
            resolution = ReviewResolution.CONFIRM,
            title = "입금 메모 입력",
            message = "${title} ${formatWon(amountWon)}을 받은 이유를 남겨요.",
            memoLabel = "받은 이유",
            defaultMemo = "",
            resultMessage = "${title} 입금 메모를 저장했어요."
        )

        else -> ReviewMemoAction(
            card = this,
            resolution = ReviewResolution.CONFIRM,
            title = "검토 메모 입력",
            message = "${title} ${formatWon(amountWon)}을 확인한 이유를 남겨요.",
            memoLabel = "메모",
            defaultMemo = "",
            resultMessage = "${title} 검토를 처리했어요."
        )
    }
}

private fun ReviewCardUi.toSecondaryMemoAction(): ReviewMemoAction =
    ReviewMemoAction(
        card = this,
        resolution = ReviewResolution.EXCLUDE,
        title = "제외 사유 입력",
        message = "${title} ${formatWon(amountWon)}을 지출에서 제외해요.",
        memoLabel = "제외 사유",
        defaultMemo = "",
        resultMessage = "${title}을 지출에서 제외했어요."
    )
