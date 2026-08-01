package com.choiyoonseo.automoney.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.choiyoonseo.automoney.data.repository.AssetRepository
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.domain.assets.buildCategoryBudgetUsages
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.MoneyAmount
import com.choiyoonseo.automoney.domain.model.MoneyTransaction
import com.choiyoonseo.automoney.domain.model.SourceType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.review.RecordWalletTopupUsageUseCase
import com.choiyoonseo.automoney.domain.review.ResolveReviewUseCase
import com.choiyoonseo.automoney.domain.review.MAX_SETTLEMENT_PARTY_COUNT
import com.choiyoonseo.automoney.domain.review.MIN_SETTLEMENT_PARTY_COUNT
import com.choiyoonseo.automoney.domain.settlement.LinkSettlementRepaymentUseCase
import com.choiyoonseo.automoney.domain.settlement.findSettlementMatch
import com.choiyoonseo.automoney.domain.review.ReviewResolution
import com.choiyoonseo.automoney.domain.review.WalletTopupReviewService
import com.choiyoonseo.automoney.domain.refund.LinkRefundUseCase
import com.choiyoonseo.automoney.domain.review.WalletTopupUsageResult
import com.choiyoonseo.automoney.domain.review.resolveReview
import com.choiyoonseo.automoney.domain.transactions.EditTransactionUseCase
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import com.choiyoonseo.automoney.ui.components.AutoClearMessageEffect
import com.choiyoonseo.automoney.ui.components.EmptyStateVisual
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.IllustratedSummaryCard
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.MoneyMuted
import com.choiyoonseo.automoney.ui.components.MoneyDialog
import com.choiyoonseo.automoney.ui.components.ReviewActionCard
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import com.choiyoonseo.automoney.ui.components.TransactionEditDialog
import com.choiyoonseo.automoney.ui.components.rememberMergedCategoryLabels
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.ui.components.reviewAccentForLabel
import com.choiyoonseo.automoney.ui.model.ReviewCardKind
import com.choiyoonseo.automoney.ui.model.ReviewCardUi
import com.choiyoonseo.automoney.ui.model.dismissReviewCard
import com.choiyoonseo.automoney.ui.model.formatWon
import com.choiyoonseo.automoney.ui.model.openReviewItemsToCards
import com.choiyoonseo.automoney.ui.model.sampleReviewCards
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ReviewScreen(
    padding: PaddingValues,
    recordWalletTopupUsageUseCase: RecordWalletTopupUsageUseCase? = null,
    moneyRepository: MoneyRepository? = null,
    editTransactionUseCase: EditTransactionUseCase? = null,
    assetRepository: AssetRepository? = null,
    resolveReviewUseCase: ResolveReviewUseCase? = null,
    linkSettlementRepaymentUseCase: LinkSettlementRepaymentUseCase? = null,
    linkRefundUseCase: LinkRefundUseCase? = null,
    userCategoryRepository: UserCategoryRepository? = null
) {
    val scope = rememberCoroutineScope()
    val reviewService = remember { WalletTopupReviewService() }
    val openReviewItems by remember(moneyRepository) {
        moneyRepository?.observeOpenReviewItems() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val dbReviewCards = remember(openReviewItems) { openReviewItemsToCards(openReviewItems) }
    val allTransactions by remember(moneyRepository) {
        moneyRepository?.observeAllTransactions() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    var budgetMonth by remember { mutableStateOf(YearMonth.now(AppDateZoneId)) }
    val monthlyPlans by remember(assetRepository, budgetMonth) {
        assetRepository?.observeMonthlyPlanItems(budgetMonth) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val budgetUsages = remember(monthlyPlans, allTransactions, budgetMonth) {
        buildCategoryBudgetUsages(
            monthlyPlans,
            allTransactions.filter { it.monthKey == budgetMonth }
        )
    }
    val fixedExpenses by remember(assetRepository) {
        assetRepository?.observeFixedExpenses() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val (expenseCategoryLabels, incomeCategoryLabels) = rememberMergedCategoryLabels(userCategoryRepository)
    var sampleReviewCardsState by remember { mutableStateOf(sampleReviewCards) }
    val reviewCards = if (moneyRepository == null) sampleReviewCardsState else dbReviewCards
    var activeWalletCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var activeUnusedWalletCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var activeReviewMemoAction by remember { mutableStateOf<ReviewMemoAction?>(null) }
    var activeEditReviewCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var activeSettlementCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var activeRefundCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var activeDeleteReviewCard by remember { mutableStateOf<ReviewCardUi?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    AutoClearMessageEffect(resultMessage) {
        resultMessage = null
    }

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

    fun reviewReasonFor(card: ReviewCardUi): ReviewReason? =
        openReviewItems.firstOrNull { it.id == card.reviewItemId }?.reason

    val openSettlements = allTransactions.filter {
        it.type == TransactionType.SETTLEMENT && it.settlementMyShareWon != null && !it.settlementTrackingHidden
    }
    val linkedRecoveries = allTransactions.filter { it.settlementParentId != null }
    fun settlementMatchFor(card: ReviewCardUi) =
        card.sourceTransaction?.let { findSettlementMatch(it, openSettlements, linkedRecoveries) }

    fun presentedCard(card: ReviewCardUi): ReviewCardUi {
        if (settlementMatchFor(card) != null) {
            return card.copy(
                tag = "정산 회수",
                message = "이 입금은 예전에 낸 N분의1 정산으로 받은 돈 같아요. 맞으면 연결하면 수입에는 잡히지 않아요.",
                primaryAction = "정산 받은 돈"
            )
        }
        return card
    }

    fun handleSettlement(card: ReviewCardUi, partyCount: Int, myShareWon: Long, memo: String?) {
        val source = card.sourceTransaction
        scope.launch {
            isSaving = true
            errorMessage = null
            try {
                if (resolveReviewUseCase != null && card.reviewItemId != null && source != null) {
                    resolveReviewUseCase.resolve(
                        reviewItemId = card.reviewItemId,
                        transaction = source,
                        resolution = ReviewResolution.SETTLEMENT,
                        userMemo = memo,
                        settlementPartyCount = partyCount,
                        settlementMyShareWon = myShareWon
                    )
                    resultMessage = "${card.title}을 N분의1 정산으로 저장했어요. 내 몫 ${formatWon(myShareWon)}만 지출에 반영했어요."
                } else {
                    sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
                    resultMessage = "정산으로 저장했어요."
                }
                activeSettlementCard = null
            } catch (e: IllegalArgumentException) {
                errorMessage = e.message ?: "입력값을 확인해 주세요."
            } finally {
                isSaving = false
            }
        }
    }

    fun handleLinkRepayment(card: ReviewCardUi, settlementTransactionId: Long) {
        val source = card.sourceTransaction
        scope.launch {
            isSaving = true
            errorMessage = null
            try {
                if (linkSettlementRepaymentUseCase != null && card.reviewItemId != null && source != null) {
                    linkSettlementRepaymentUseCase.link(
                        reviewItemId = card.reviewItemId,
                        repaymentTransaction = source,
                        settlementTransactionId = settlementTransactionId
                    )
                    resultMessage = "정산 받은 돈으로 연결했어요. 수입에는 잡지 않아요."
                } else {
                    sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
                    resultMessage = "정산 받은 돈으로 처리했어요."
                }
            } catch (e: IllegalArgumentException) {
                errorMessage = e.message ?: "연결 중 문제가 생겼어요."
            } finally {
                isSaving = false
            }
        }
    }

    fun handleLinkRefund(card: ReviewCardUi, paymentId: Long) {
        val refundId = card.sourceTransaction?.id
        scope.launch {
            isSaving = true
            errorMessage = null
            try {
                if (linkRefundUseCase != null && refundId != null) {
                    linkRefundUseCase.linkConfirmed(refundId = refundId, paymentId = paymentId)
                    resultMessage = "원결제에 연결했어요. 그만큼 사용액에서 빠져요."
                } else {
                    sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
                    resultMessage = "원결제에 연결했어요."
                }
                activeRefundCard = null
            } catch (e: IllegalArgumentException) {
                errorMessage = e.message ?: "연결 중 문제가 생겼어요."
            } catch (e: RuntimeException) {
                errorMessage = "연결 중 문제가 생겼어요."
            } finally {
                isSaving = false
            }
        }
    }

    fun filterKindFor(card: ReviewCardUi): ReviewFilterKind = when {
        card.kind == ReviewCardKind.WALLET_TOPUP -> ReviewFilterKind.TOPUP
        card.kind == ReviewCardKind.TRANSFER -> ReviewFilterKind.TRANSFER
        reviewReasonFor(card) == ReviewReason.DUPLICATE_SUSPECTED -> ReviewFilterKind.DUPLICATE
        else -> ReviewFilterKind.OTHER
    }

    var selectedFilter by remember { mutableStateOf(ReviewFilterKind.ALL) }
    val filteredCards = if (selectedFilter == ReviewFilterKind.ALL) {
        reviewCards
    } else {
        reviewCards.filter { filterKindFor(it) == selectedFilter }
    }

    fun handleReviewMemoAction(action: ReviewMemoAction, userMemo: String?) {
        scope.launch {
            isSaving = true
            errorMessage = null
            try {
                val card = action.card
                val sourceTransaction = card.sourceTransaction
                if (resolveReviewUseCase != null && card.reviewItemId != null && sourceTransaction != null) {
                    resolveReviewUseCase.resolve(
                        reviewItemId = card.reviewItemId,
                        transaction = sourceTransaction,
                        resolution = action.resolution,
                        userMemo = userMemo
                    )
                    resultMessage = action.resultMessage
                } else {
                    resolveCard(
                        card = card,
                        message = action.resultMessage,
                        transactionUpdate = { transaction ->
                            transaction.resolveReview(action.resolution, userMemo)
                        }
                    )
                }
                activeReviewMemoAction = null
            } finally {
                isSaving = false
            }
        }
    }

    fun deleteReviewCard(card: ReviewCardUi) {
        scope.launch {
            isSaving = true
            errorMessage = null
            try {
                val transaction = card.sourceTransaction
                if (editTransactionUseCase != null && transaction != null) {
                    editTransactionUseCase.delete(transaction)
                } else if (moneyRepository != null && transaction != null) {
                    moneyRepository.deleteTransaction(transaction.id)
                } else {
                    sampleReviewCardsState = dismissReviewCard(sampleReviewCardsState, card.id)
                }
                resultMessage = "${card.title}을 삭제했어요."
                activeDeleteReviewCard = null
            } catch (e: RuntimeException) {
                errorMessage = "삭제 중 문제가 생겼어요."
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MoneyTheme.colors.canvas)
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
            icon = Icons.Filled.Search
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipEntries = buildList {
                add(Triple(ReviewFilterKind.ALL, "전체 ${reviewCards.size}", MoneyCoral))
                val counts = reviewCards.groupingBy { filterKindFor(it) }.eachCount()
                counts[ReviewFilterKind.TRANSFER]?.let { add(Triple(ReviewFilterKind.TRANSFER, "송금 $it", reviewAccentForLabel("송금"))) }
                counts[ReviewFilterKind.TOPUP]?.let { add(Triple(ReviewFilterKind.TOPUP, "충전 $it", reviewAccentForLabel("충전"))) }
                counts[ReviewFilterKind.DUPLICATE]?.let { add(Triple(ReviewFilterKind.DUPLICATE, "중복 $it", reviewAccentForLabel("중복"))) }
                counts[ReviewFilterKind.OTHER]?.let { add(Triple(ReviewFilterKind.OTHER, "기타 $it", MoneyMuted)) }
            }
            chipEntries.forEach { (kind, label, accent) ->
                ReviewFilterChip(
                    label = label,
                    selected = selectedFilter == kind,
                    accent = accent,
                    onClick = { selectedFilter = kind }
                )
            }
        }

        filteredCards.forEach { card ->
            ReviewActionCard(
                card = presentedCard(card),
                onPrimaryAction = {
                    val match = settlementMatchFor(card)
                    if (card.kind == ReviewCardKind.WALLET_TOPUP) {
                        activeWalletCard = card
                    } else if (match != null) {
                        handleLinkRepayment(card, match.settlementTransactionId)
                    } else if (card.kind == ReviewCardKind.REFUND && linkRefundUseCase != null) {
                        activeRefundCard = card
                        errorMessage = null
                    } else if (card.kind == ReviewCardKind.TRANSFER) {
                        activeSettlementCard = card
                        errorMessage = null
                    } else if (reviewReasonFor(card) == ReviewReason.INCOME_UNKNOWN) {
                        handleReviewMemoAction(card.toPrimaryMemoAction(), null)
                    } else {
                        activeReviewMemoAction = card.toPrimaryMemoAction()
                    }
                },
                onSecondaryAction = {
                    if (card.kind == ReviewCardKind.WALLET_TOPUP) {
                        activeUnusedWalletCard = card
                    } else if (reviewReasonFor(card) == ReviewReason.INCOME_UNKNOWN) {
                        activeDeleteReviewCard = card
                        errorMessage = null
                    } else {
                        activeReviewMemoAction = card.toSecondaryMemoAction()
                    }
                },
                onEditAction = if (card.sourceTransaction != null && editTransactionUseCase != null) {
                    {
                        card.sourceTransaction?.monthKey?.let { budgetMonth = it }
                        activeEditReviewCard = card
                        editErrorMessage = null
                    }
                } else {
                    null
                }
            )
        }

        if (reviewCards.isNotEmpty() && filteredCards.isEmpty()) {
            Text(
                "이 분류의 검토가 없어요.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = MoneyTheme.colors.muted
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

    activeDeleteReviewCard?.let { card ->
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    activeDeleteReviewCard = null
                    errorMessage = null
                }
            },
            title = { Text("거래 삭제") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${card.title} ${formatWon(card.amountWon)}을 삭제할까요?")
                    Text("거래와 검토 기록이 함께 삭제되며 되돌릴 수 없어요.")
                    errorMessage?.let { Text(it, color = MoneyTheme.colors.negative) }
                }
            },
            confirmButton = {
                Button(onClick = { deleteReviewCard(card) }, enabled = !isSaving) {
                    Text(if (isSaving) "삭제 중" else "삭제")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        activeDeleteReviewCard = null
                        errorMessage = null
                    },
                    enabled = !isSaving
                ) {
                    Text("취소")
                }
            }
        )
    }

    activeRefundCard?.let { card ->
        RefundLinkDialog(
            card = card,
            linkRefundUseCase = linkRefundUseCase,
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = {
                activeRefundCard = null
                errorMessage = null
            },
            onSelect = { paymentId -> handleLinkRefund(card, paymentId) },
            onSkip = {
                activeRefundCard = null
                activeReviewMemoAction = card.toPrimaryMemoAction()
            }
        )
    }

    activeSettlementCard?.let { card ->
        SettlementDialog(
            card = card,
            isSaving = isSaving,
            errorMessage = errorMessage,
            onDismiss = {
                activeSettlementCard = null
                errorMessage = null
            },
            onSave = { partyCount, myShareWon, memo ->
                handleSettlement(card, partyCount, myShareWon, memo)
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
                budgetUsages = budgetUsages,
                fixedExpenses = fixedExpenses,
                expenseCategoryLabels = expenseCategoryLabels,
                incomeCategoryLabels = incomeCategoryLabels,
                onDateChanged = { budgetMonth = YearMonth.from(it) },
                onDismiss = {
                    activeEditReviewCard = null
                    editErrorMessage = null
                },
                onSave = { amountWon, category, memo, occurredAt, budgetPlanId, fixedExpensePlanId, transactionType ->
                    scope.launch {
                        isSaving = true
                        editErrorMessage = null
                        try {
                            useCase.update(
                                transaction,
                                amountWon,
                                category,
                                memo,
                                occurredAt,
                                transactionType = transactionType,
                                budgetPlanId = budgetPlanId,
                                fixedExpensePlanId = fixedExpensePlanId
                            )
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
                excludeLabel = "거래 제외",
                deleteLabel = "거래 삭제",
                onDelete = {
                    activeEditReviewCard = null
                    activeDeleteReviewCard = card
                    editErrorMessage = null
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
private fun SettlementDialog(
    card: ReviewCardUi,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (partyCount: Int, myShareWon: Long, memo: String?) -> Unit
) {
    val colors = MoneyTheme.colors
    val total = card.amountWon
    var partyCount by remember(card.id) { mutableStateOf(MIN_SETTLEMENT_PARTY_COUNT) }
    val suggestedShare = (total.toDouble() / partyCount).toLong()
    var myShareText by remember(card.id) { mutableStateOf(suggestedShare.toString()) }
    var edited by remember(card.id) { mutableStateOf(false) }
    val myShareWon = myShareText.replace(",", "").trim().toLongOrNull()
    val receivable = if (myShareWon != null) (total - myShareWon).coerceAtLeast(0) else 0

    MoneyDialog(
        title = "N분의1 정산",
        subtitle = "${card.title} ${formatWon(total)}",
        onDismiss = onDismiss,
        buttons = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text("취소")
                }
                Button(
                    enabled = !isSaving && myShareWon != null && myShareWon in 0..total,
                    modifier = Modifier.weight(1f),
                    onClick = { onSave(partyCount, myShareWon ?: 0, null) }
                ) {
                    Text(if (isSaving) "저장 중" else "저장")
                }
            }
        }
    ) {
        Text("몇 명이서 나눠요?", style = MaterialTheme.typography.labelMedium, color = colors.inkSub)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepperButton("−", enabled = partyCount > MIN_SETTLEMENT_PARTY_COUNT) {
                partyCount -= 1
                if (!edited) myShareText = (total.toDouble() / partyCount).toLong().toString()
            }
            Text("${partyCount}명", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.ink)
            StepperButton("+", enabled = partyCount < MAX_SETTLEMENT_PARTY_COUNT) {
                partyCount += 1
                if (!edited) myShareText = (total.toDouble() / partyCount).toLong().toString()
            }
            Spacer(Modifier.weight(1f))
            Text("1인당 ${formatWon((total.toDouble() / partyCount).toLong())}", style = MaterialTheme.typography.labelMedium, color = colors.muted)
        }
        OutlinedTextField(
            value = myShareText,
            onValueChange = { myShareText = it; edited = true },
            label = { Text("내 몫 (지출에 반영)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Surface(shape = RoundedCornerShape(14.dp), color = colors.canvas, modifier = Modifier.fillMaxWidth()) {
            Text(
                "내 몫 ${formatWon(myShareWon ?: 0)}만 지출로 잡고, 받을 돈 ${formatWon(receivable)}은 참고로만 남겨요.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.inkSub
            )
        }
        (errorMessage)?.let { Text(it, color = colors.negative, style = MaterialTheme.typography.bodySmall) }
    }
}

// 환급을 원결제에 붙여 순사용액으로 만든다. 후보는 금액·상호·시각만 보여준다.
@Composable
private fun RefundLinkDialog(
    card: ReviewCardUi,
    linkRefundUseCase: LinkRefundUseCase?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSelect: (paymentId: Long) -> Unit,
    onSkip: () -> Unit
) {
    val colors = MoneyTheme.colors
    val refundId = card.sourceTransaction?.id
    var candidates by remember(card.id) { mutableStateOf<List<MoneyTransaction>?>(null) }

    LaunchedEffect(card.id, linkRefundUseCase, refundId) {
        candidates = if (linkRefundUseCase == null || refundId == null) {
            emptyList()
        } else {
            try {
                linkRefundUseCase.candidates(refundId)
            } catch (_: RuntimeException) {
                emptyList()
            }
        }
    }

    MoneyDialog(
        title = "어떤 결제의 환급인가요?",
        subtitle = "${card.title} ${formatWon(card.amountWon)}",
        onDismiss = onDismiss,
        buttons = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text("취소")
                }
                OutlinedButton(onClick = onSkip, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text("연결 없이 처리")
                }
            }
        }
    ) {
        when (val loaded = candidates) {
            null -> Text("원결제를 찾는 중이에요.", style = MaterialTheme.typography.bodySmall, color = colors.muted)
            else -> if (loaded.isEmpty()) {
                Text(
                    "최근 30일 안에서 연결할 결제를 찾지 못했어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            } else {
                loaded.forEach { payment ->
                    RefundCandidateRow(
                        payment = payment,
                        enabled = !isSaving,
                        onClick = { onSelect(payment.id) }
                    )
                }
            }
        }
        errorMessage?.let { Text(it, color = colors.negative, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun RefundCandidateRow(
    payment: MoneyTransaction,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = MoneyTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.canvas,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .padding(12.dp)
        ) {
            Text(
                formatWon(payment.amount.won),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )
            Text(
                listOfNotNull(
                    payment.merchant?.takeIf { it.isNotBlank() },
                    payment.occurredAt.toRefundCandidateTime()
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted,
                maxLines = 1
            )
        }
    }
}

private fun Instant.toRefundCandidateTime(): String =
    atZone(AppDateZoneId).format(refundCandidateTimeFormatter)

private val refundCandidateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 HH:mm")

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MoneyTheme.colors
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) colors.soft(colors.primary) else colors.canvas,
        modifier = Modifier.size(40.dp).let { if (enabled) it.clickable(onClick = onClick) else it }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (enabled) colors.primary else colors.muted)
        }
    }
}

@Composable
private fun ReviewFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoneyTheme.colors
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) colors.soft(accent) else accent.copy(alpha = 0.07f)
    ) {
        Text(
            text = label,
            color = if (selected) accent else colors.muted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

enum class ReviewFilterKind { ALL, TRANSFER, TOPUP, DUPLICATE, OTHER }

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
    monthKey = YearMonth.now(AppDateZoneId)
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
            title = "입금 확인",
            message = "${title} ${formatWon(amountWon)}을 입금으로 확인해요.",
            memoLabel = "메모",
            defaultMemo = "",
            resultMessage = "${title}을 입금으로 확인했어요."
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
