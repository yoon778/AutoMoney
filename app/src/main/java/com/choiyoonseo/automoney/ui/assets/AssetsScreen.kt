package com.choiyoonseo.automoney.ui.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.data.repository.AssetRepository
import com.choiyoonseo.automoney.data.repository.MoneyRepository
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.domain.category.UserCategory
import com.choiyoonseo.automoney.domain.category.UserCategoryKind
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.assets.CategoryBudgetUsage
import com.choiyoonseo.automoney.domain.assets.buildCategoryBudgetUsages
import com.choiyoonseo.automoney.domain.assets.calculateUnbudgetedExpenseWon
import com.choiyoonseo.automoney.domain.report.countsAsActualExpense
import com.choiyoonseo.automoney.domain.report.PlannedUseContribution
import com.choiyoonseo.automoney.domain.report.plannedUseContributions
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import java.time.YearMonth
import com.choiyoonseo.automoney.domain.assets.AssetOverview
import com.choiyoonseo.automoney.domain.assets.FixedExpensePlan
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItem
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItemType
import com.choiyoonseo.automoney.domain.assets.buildAssetOverview
import com.choiyoonseo.automoney.domain.assets.fixedExpenseWithdrawalDayOptions
import com.choiyoonseo.automoney.domain.assets.validatedForSave
import com.choiyoonseo.automoney.ui.components.AutoClearMessageEffect
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyDialog
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.MoneyGreen
import com.choiyoonseo.automoney.ui.components.MoneyMint
import com.choiyoonseo.automoney.ui.components.MonthPagerHeader
import com.choiyoonseo.automoney.ui.components.MonthPagerInitialPage
import com.choiyoonseo.automoney.ui.components.MonthPagerPageCount
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import com.choiyoonseo.automoney.ui.components.categoryAccentForName
import com.choiyoonseo.automoney.ui.components.monthForPagerPage
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import com.choiyoonseo.automoney.ui.model.formatWon
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private enum class AssetSection(val label: String) {
    PLAN("월계획"),
    FIXED("고정지출")
}

@Composable
fun AssetsScreen(
    padding: PaddingValues,
    assetRepository: AssetRepository? = null,
    moneyRepository: MoneyRepository? = null,
    userCategoryRepository: UserCategoryRepository? = null
) {
    val scope = rememberCoroutineScope()
    val anchorMonth = remember { YearMonth.now(AppDateZoneId) }
    val pagerState = rememberPagerState(
        initialPage = MonthPagerInitialPage,
        pageCount = { MonthPagerPageCount }
    )
    val selectedMonth = monthForPagerPage(pagerState.currentPage, anchorMonth)
    val userExpenseCategories by remember(userCategoryRepository) {
        userCategoryRepository?.observeActiveCategories() ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    var selectedSection by remember { mutableStateOf(AssetSection.PLAN) }
    var message by remember { mutableStateOf<String?>(null) }
    AutoClearMessageEffect(message) {
        message = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MoneyTheme.colors.canvas)
            .padding(start = 18.dp, top = 18.dp, end = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            title = "예산",
            subtitle = "카테고리별 예산에서 얼마나 썼는지 봐요"
        )

        MonthPagerHeader(
            month = selectedMonth,
            onPreviousMonth = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onNextMonth = {
                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
        )

        message?.let { AssistChip(onClick = {}, label = { Text(it) }) }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            key = { page -> monthForPagerPage(page, anchorMonth).toString() }
        ) { page ->
            AssetsMonthPage(
                month = monthForPagerPage(page, anchorMonth),
                assetRepository = assetRepository,
                moneyRepository = moneyRepository,
                userExpenseCategories = userExpenseCategories,
                selectedSection = selectedSection,
                onSelectedSection = { selectedSection = it },
                onMessage = { message = it }
            )
        }
    }
}

@Composable
private fun AssetsMonthPage(
    month: YearMonth,
    assetRepository: AssetRepository?,
    moneyRepository: MoneyRepository?,
    userExpenseCategories: List<UserCategory>,
    selectedSection: AssetSection,
    onSelectedSection: (AssetSection) -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val fixedExpenses by remember(assetRepository, month) {
        assetRepository?.observeFixedExpenses(month) ?: flowOf(sampleFixedExpenses)
    }.collectAsState(initial = emptyList())
    val monthlyPlans by remember(assetRepository, month) {
        assetRepository?.observeMonthlyPlanItems(month) ?: flowOf(sampleMonthlyPlanItems)
    }.collectAsState(initial = emptyList())
    val monthTransactions by remember(moneyRepository, month) {
        moneyRepository?.observeTransactionsForMonth(month) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val overview = remember(fixedExpenses, monthlyPlans, monthTransactions) {
        buildAssetOverview(
            emptyList(),
            fixedExpenses,
            monthlyPlans,
            spentThisMonthWon = plannedUseContributions(monthTransactions)
                .filter { it.transaction.countsAsActualExpense() }
                .sumOf(PlannedUseContribution::amountWon)
        )
    }
    val budgetUsages = remember(monthlyPlans, monthTransactions) {
        buildCategoryBudgetUsages(monthlyPlans, monthTransactions)
    }
    val livingBudgetUsages = remember(budgetUsages) { budgetUsages.filterNot { it.isInvestmentPlan() } }
    val investmentUsages = remember(budgetUsages) { budgetUsages.filter { it.isInvestmentPlan() } }
    val unbudgetedExpenseWon = remember(monthlyPlans, monthTransactions) {
        calculateUnbudgetedExpenseWon(monthlyPlans, monthTransactions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FinanceSectionCard(
            title = "${month.monthValue}월 예산",
            subtitle = "남은 금액 기준 · 초과는 빨간색",
            accent = MoneyBlue,
            icon = Icons.Filled.AccountBalance
        ) {
            livingBudgetUsages.forEach { usage ->
                val over = usage.remainingWon < 0
                AssetRow(
                    title = usage.plan.label,
                    subtitle = if (over) {
                        "${formatWon(usage.spentWon)} 사용 · ${formatWon(-usage.remainingWon)} 초과"
                    } else {
                        "${formatWon(usage.spentWon)} 사용 · ${formatWon(usage.remainingWon)} 남음"
                    },
                    amountWon = usage.plan.amountWon,
                    ratio = usage.usedRatio,
                    accent = if (over) MoneyCoral else categoryAccentForName(usage.plan.label)
                )
            }
            if (livingBudgetUsages.isEmpty()) {
                Text("아래 월계획에서 예산을 만들면 여기에 남은 금액이 표시돼요.")
            }
            if (unbudgetedExpenseWon > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MoneyTheme.colors.soft(MoneyCoral)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "예산 밖 지출 ${formatWon(unbudgetedExpenseWon)}",
                            fontWeight = FontWeight.Bold,
                            color = MoneyCoral
                        )
                        Text(
                            "어느 예산에도 안 잡힌 지출이에요. 월계획에 예산을 추가해 관리해 보세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MoneyTheme.colors.muted
                        )
                    }
                }
            }
        }

        if (investmentUsages.isNotEmpty()) {
            InvestmentPlanCard(investmentUsages, month)
        }

        IncomeAllocationCard(overview, month)

        TabRow(
            selectedTabIndex = selectedSection.ordinal,
            containerColor = MoneyTheme.colors.surface,
            contentColor = MoneyTheme.colors.primary
        ) {
            AssetSection.entries.forEach { section ->
                Tab(
                    selected = selectedSection == section,
                    onClick = { onSelectedSection(section) },
                    text = { Text(section.label) }
                )
            }
        }

        when (selectedSection) {
            AssetSection.FIXED -> FixedExpensePanel(
                month = month,
                plans = fixedExpenses,
                onSave = { plan ->
                    val repository = assetRepository
                    if (repository == null) {
                        onMessage("미리보기에서는 저장하지 않아요.")
                    } else {
                        scope.launch {
                            repository.saveFixedExpense(plan, month)
                            onMessage("${month.monthValue}월부터 ${plan.name} 고정지출을 저장했어요.")
                        }
                    }
                },
                onDelete = { plan ->
                    val repository = assetRepository
                    if (repository == null) {
                        onMessage("미리보기에서는 삭제하지 않아요.")
                    } else {
                        scope.launch {
                            repository.deleteFixedExpense(plan.id, month)
                            onMessage("${month.monthValue}월부터 ${plan.name} 고정지출을 종료했어요.")
                        }
                    }
                }
            )

            AssetSection.PLAN -> {
                MonthlyPlanPanel(
                    items = monthlyPlans,
                    usages = budgetUsages,
                    userExpenseCategories = userExpenseCategories.filter { it.kind == UserCategoryKind.EXPENSE },
                    plannedRemainingWon = overview.plannedRemainingWon.takeIf { overview.totalIncomeWon > 0 },
                    onSave = { item ->
                        val repository = assetRepository
                        if (repository == null) {
                            onMessage("미리보기에서는 저장하지 않아요.")
                        } else {
                            scope.launch {
                                repository.saveMonthlyPlanItem(item, month)
                                onMessage("${month.monthValue}월 ${item.label} 계획을 저장했어요.")
                            }
                        }
                    },
                    onDelete = { item ->
                        val repository = assetRepository
                        if (repository == null) {
                            onMessage("미리보기에서는 삭제하지 않아요.")
                        } else {
                            scope.launch {
                                repository.deleteMonthlyPlanItem(item.id)
                                onMessage("${item.label} 계획을 삭제했어요.")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InvestmentPlanCard(usages: List<CategoryBudgetUsage>, month: YearMonth) {
    FinanceSectionCard(
        title = "${month.monthValue}월 투자 계획",
        subtitle = "생활비 지출과 따로 계산해요",
        accent = MoneyMint,
        icon = Icons.Filled.BarChart
    ) {
        usages.forEach { usage ->
            val over = usage.remainingWon < 0
            AssetRow(
                title = usage.plan.label,
                subtitle = if (over) {
                    "${formatWon(usage.spentWon)} 사용 · ${formatWon(-usage.remainingWon)} 초과"
                } else {
                    "${formatWon(usage.spentWon)} 사용 · ${formatWon(usage.remainingWon)} 남음"
                },
                amountWon = usage.plan.amountWon,
                ratio = usage.usedRatio,
                accent = if (over) MoneyCoral else MoneyMint
            )
        }
    }
}

@Composable
private fun IncomeAllocationCard(overview: AssetOverview, month: YearMonth) {
    val colors = MoneyTheme.colors
    val income = overview.totalIncomeWon
    val fixed = overview.totalFixedExpenseWon
    val budget = overview.totalBudgetWon
    val remaining = income - fixed - budget
    val denom = maxOf(income, fixed + budget).coerceAtLeast(1L)

    FinanceSectionCard(
        title = "${month.monthValue}월 배분",
        subtitle = if (income > 0) "수입에서 고정·변동 예산을 뺀 나머지" else "수입을 등록하면 남는 돈까지 봐요",
        accent = MoneyGreen,
        icon = Icons.Filled.BarChart
    ) {
        Text(
            when {
                income <= 0 -> "이번 달 나갈 돈 ${formatWon(fixed + budget)}"
                remaining >= 0 -> "쓸 수 있는 돈 ${formatWon(remaining)}"
                else -> "예산이 수입보다 ${formatWon(-remaining)} 많아요"
            },
            fontWeight = FontWeight.Bold,
            color = if (income > 0 && remaining < 0) MoneyCoral else colors.ink,
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.canvas)
        ) {
            if (fixed > 0) {
                Box(Modifier.weight(fixed.toFloat() / denom).fillMaxHeight().background(MoneyCoral))
            }
            if (budget > 0) {
                Box(Modifier.weight(budget.toFloat() / denom).fillMaxHeight().background(MoneyBlue))
            }
            if (remaining > 0) {
                Box(Modifier.weight(remaining.toFloat() / denom).fillMaxHeight().background(MoneyGreen))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AllocationLegend("고정지출", fixed, MoneyCoral)
            AllocationLegend("변동지출 예산", budget, MoneyBlue)
            if (income > 0) {
                AllocationLegend("남는 돈", remaining.coerceAtLeast(0), MoneyGreen)
            }
        }
    }
}

@Composable
private fun AllocationLegend(label: String, amountWon: Long, accent: Color) {
    val colors = MoneyTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(50))
                .background(accent)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.muted)
            Text(formatWon(amountWon), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.ink)
        }
    }
}

@Composable
private fun FixedExpensePanel(
    month: YearMonth,
    plans: List<FixedExpensePlan>,
    onSave: (FixedExpensePlan) -> Unit,
    onDelete: (FixedExpensePlan) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<FixedExpensePlan?>(null) }
    var editingPlan by remember { mutableStateOf<FixedExpensePlan?>(null) }
    editingPlan?.let { plan ->
        MoneyDialog(
            title = "고정지출 수정",
            subtitle = "금액·출금일·출금 수단을 바꿀 수 있어요.",
            onDismiss = { editingPlan = null },
            buttons = {}
        ) {
            FixedExpenseInputCard(
                editingPlan = plan,
                onSave = { updated ->
                    onSave(updated)
                    editingPlan = null
                },
                onCancel = { editingPlan = null },
                onDelete = { target ->
                    pendingDelete = target
                    editingPlan = null
                },
                showCard = false
            )
        }
    }
    pendingDelete?.let { plan ->
        DeleteConfirmDialog(
            name = plan.name,
            title = "고정지출을 종료할까요?",
            subtitle = "${month.monthValue}월부터 적용하지 않아요. 이전 달 기록은 유지돼요.",
            confirmLabel = "종료",
            onConfirm = {
                onDelete(plan)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceSectionCard(
            title = "고정지출 목록",
            subtitle = "매월 자동으로 빠지는 돈",
            accent = MoneyCoral,
            icon = Icons.Filled.BarChart
        ) {
            plans.forEach { plan ->
                AssetRow(
                    title = plan.name,
                    subtitle = "매월 ${plan.withdrawalDay}일 · ${plan.accountName}",
                    amountWon = plan.amountWon,
                    accent = categoryAccentForName(plan.name),
                    actionLabel = "수정",
                    onAction = { editingPlan = plan },
                    actionIcon = Icons.Filled.Edit
                )
            }
            if (plans.isEmpty()) {
                Text("아직 등록된 고정지출이 없어요.")
            }
        }
        FixedExpenseInputCard(
            editingPlan = null,
            onSave = onSave,
            onCancel = {},
            onDelete = {},
            showCard = true
        )
    }
}

internal fun fixedExpensePlanForSave(
    existing: FixedExpensePlan?,
    name: String,
    amountWon: Long,
    withdrawalDay: Int,
    accountName: String
): FixedExpensePlan {
    val cleanAccountName = accountName.trim()
    return FixedExpensePlan(
        id = existing?.id ?: 0,
        name = name.trim(),
        amountWon = amountWon,
        withdrawalDay = withdrawalDay,
        accountName = cleanAccountName,
        accountId = existing?.accountId?.takeIf { existing.accountName.trim() == cleanAccountName },
        active = existing?.active ?: true
    ).validatedForSave()
}

@Composable
private fun FixedExpenseInputCard(
    editingPlan: FixedExpensePlan?,
    onSave: (FixedExpensePlan) -> Unit,
    onCancel: () -> Unit,
    onDelete: (FixedExpensePlan) -> Unit,
    showCard: Boolean
) {
    var name by remember(editingPlan) { mutableStateOf(editingPlan?.name.orEmpty()) }
    var amount by remember(editingPlan) { mutableStateOf(editingPlan?.amountWon?.toString().orEmpty()) }
    var day by remember(editingPlan) { mutableStateOf(editingPlan?.withdrawalDay ?: 1) }
    var accountName by remember(editingPlan) { mutableStateOf(editingPlan?.accountName.orEmpty()) }

    FormContainer(
        title = if (editingPlan == null) "고정지출 추가" else "고정지출 수정",
        showCard = showCard
    ) {
        OutlinedTextField(name, { name = it }, label = { Text("이름") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = it }, label = { Text("금액") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        WithdrawalDayPicker(selectedDay = day, onSelected = { day = it })
        OutlinedTextField(
            value = accountName,
            onValueChange = { accountName = it },
            label = { Text("출금 수단 (예: 국민은행)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val cleanAmount = amount.cleanWonOrNull()
                if (name.isNotBlank() && accountName.isNotBlank() && cleanAmount != null && day in fixedExpenseWithdrawalDayOptions) {
                    onSave(
                        fixedExpensePlanForSave(
                            existing = editingPlan,
                            name = name,
                            amountWon = cleanAmount,
                            withdrawalDay = day,
                            accountName = accountName
                        )
                    )
                    if (editingPlan == null) {
                        name = ""
                        amount = ""
                        day = 1
                        accountName = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (editingPlan == null) "저장" else "수정 저장")
        }
        if (editingPlan != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("취소")
                }
                OutlinedButton(onClick = { onDelete(editingPlan) }, modifier = Modifier.weight(1f)) {
                    Text("삭제")
                }
            }
        }
    }
}

@Composable
private fun MonthlyPlanPanel(
    items: List<MonthlyPlanItem>,
    usages: List<CategoryBudgetUsage>,
    userExpenseCategories: List<UserCategory>,
    plannedRemainingWon: Long?,
    onSave: (MonthlyPlanItem) -> Unit,
    onDelete: (MonthlyPlanItem) -> Unit
) {
    val usageByPlanId = remember(usages) { usages.associateBy { it.plan.id } }
    var pendingDelete by remember { mutableStateOf<MonthlyPlanItem?>(null) }
    var editingItem by remember { mutableStateOf<MonthlyPlanItem?>(null) }
    editingItem?.let { item ->
        MoneyDialog(
            title = "월계획 수정",
            subtitle = "이 달의 수입·예산 계획만 변경돼요.",
            onDismiss = { editingItem = null },
            buttons = {}
        ) {
            MonthlyPlanInputCard(
                userExpenseCategories = userExpenseCategories,
                editingItem = item,
                onSave = { updated ->
                    onSave(updated)
                    editingItem = null
                },
                onCancel = { editingItem = null },
                onDelete = { target ->
                    pendingDelete = target
                    editingItem = null
                },
                showCard = false
            )
        }
    }
    pendingDelete?.let { item ->
        DeleteConfirmDialog(
            name = item.label,
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceSectionCard(
            title = "월계획",
            subtitle = "예산 항목 합계가 위 '변동지출 예산'이 돼요",
            accent = MoneyBlue,
            icon = Icons.Filled.BarChart
        ) {
            items.forEach { item ->
                val usage = usageByPlanId[item.id]
                val subtitle = when {
                    item.type == MonthlyPlanItemType.INCOME -> item.type.label
                    usage == null || (item.category == null && item.customCategoryId == null) -> "분류 연결 필요"
                    usage.remainingWon >= 0 ->
                        "${budgetCategoryName(item)} · ${formatWon(usage.spentWon)} 사용 · ${formatWon(usage.remainingWon)} 남음"
                    else ->
                        "${budgetCategoryName(item)} · ${formatWon(usage.spentWon)} 사용 · ${formatWon(-usage.remainingWon)} 초과"
                }
                AssetRow(
                    title = item.label,
                    subtitle = subtitle,
                    amountWon = item.amountWon,
                    ratio = usage?.usedRatio
                        ?: (item.amountWon.toFloat() / (items.maxOfOrNull { it.amountWon }?.coerceAtLeast(1)?.toFloat() ?: 1f)),
                    accent = if (item.type == MonthlyPlanItemType.INCOME) MoneyGreen else categoryAccentForName(budgetCategoryName(item) ?: item.label),
                    actionLabel = "수정",
                    onAction = { editingItem = item },
                    actionIcon = Icons.Filled.Edit
                )
            }
            if (items.isEmpty()) {
                Text("아직 월계획이 없어요.")
            }
            plannedRemainingWon?.let { remaining ->
                Text(
                    if (remaining >= 0) {
                        "수입에서 고정지출·예산을 빼면 ${formatWon(remaining)} 남아요"
                    } else {
                        "계획이 수입보다 ${formatWon(-remaining)} 많아요"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (remaining >= 0) MoneyGreen else MoneyCoral
                )
            }
        }
        MonthlyPlanInputCard(
            userExpenseCategories = userExpenseCategories,
            editingItem = null,
            onSave = onSave,
            onCancel = {},
            onDelete = {},
            showCard = true
        )
    }
}

private fun budgetCategoryName(item: MonthlyPlanItem): String? =
    item.customCategoryName ?: item.category?.let(::planCategoryLabel)

internal fun monthlyPlanItemForSave(
    existing: MonthlyPlanItem?,
    label: String,
    amountWon: Long,
    type: MonthlyPlanItemType,
    builtInCategory: Category?,
    userCategory: UserCategory?
): MonthlyPlanItem = MonthlyPlanItem(
    id = existing?.id ?: 0,
    label = label.trim(),
    amountWon = amountWon,
    type = type,
    category = if (type == MonthlyPlanItemType.BUDGET) {
        userCategory?.let { Category.OTHER } ?: builtInCategory
    } else null,
    customCategoryId = userCategory?.id.takeIf { type == MonthlyPlanItemType.BUDGET },
    customCategoryName = userCategory?.name.takeIf { type == MonthlyPlanItemType.BUDGET }
)

@Composable
private fun MonthlyPlanInputCard(
    userExpenseCategories: List<UserCategory>,
    editingItem: MonthlyPlanItem?,
    onSave: (MonthlyPlanItem) -> Unit,
    onCancel: () -> Unit,
    onDelete: (MonthlyPlanItem) -> Unit,
    showCard: Boolean
) {
    var label by remember(editingItem) { mutableStateOf(editingItem?.label.orEmpty()) }
    var amount by remember(editingItem) { mutableStateOf(editingItem?.amountWon?.toString().orEmpty()) }
    var type by remember(editingItem) { mutableStateOf(editingItem?.type ?: MonthlyPlanItemType.BUDGET) }
    var builtInCategory by remember(editingItem) {
        mutableStateOf(editingItem?.takeIf { it.customCategoryId == null }?.category)
    }
    var userCategory by remember(editingItem, userExpenseCategories) {
        mutableStateOf(userExpenseCategories.firstOrNull { it.id == editingItem?.customCategoryId })
    }

    FormContainer(
        title = if (editingItem == null) "월계획 추가" else "월계획 수정",
        showCard = showCard
    ) {
        OutlinedTextField(label, { label = it }, label = { Text("항목") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = it }, label = { Text("금액") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonthlyPlanItemType.entries.forEach { option ->
                if (type == option) {
                    FilledTonalButton(onClick = { type = option }) { Text(option.label) }
                } else {
                    OutlinedButton(onClick = { type = option }) { Text(option.label) }
                }
            }
        }
        if (type == MonthlyPlanItemType.BUDGET) {
            Text("분류", fontWeight = FontWeight.Medium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                planCategoryPool.forEach { category ->
                    val selected = builtInCategory == category && userCategory == null
                    if (selected) {
                        FilledTonalButton(onClick = {}) { Text(planCategoryLabel(category)) }
                    } else {
                        OutlinedButton(onClick = { builtInCategory = category; userCategory = null }) { Text(planCategoryLabel(category)) }
                    }
                }
                userExpenseCategories.forEach { custom ->
                    val selected = userCategory?.id == custom.id
                    if (selected) {
                        FilledTonalButton(onClick = {}) { Text(custom.name) }
                    } else {
                        OutlinedButton(onClick = { userCategory = custom; builtInCategory = null }) { Text(custom.name) }
                    }
                }
            }
            if (builtInCategory == null && userCategory == null) {
                Text(
                    "예산이 합산될 분류를 골라 주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MoneyTheme.colors.muted
                )
            }
        }
        Button(
            onClick = {
                val cleanAmount = amount.cleanWonOrNull()
                val categoryChosen = type == MonthlyPlanItemType.INCOME ||
                    builtInCategory != null || userCategory != null
                if (label.isNotBlank() && cleanAmount != null && categoryChosen) {
                    onSave(
                        monthlyPlanItemForSave(
                            existing = editingItem,
                            label = label,
                            amountWon = cleanAmount,
                            type = type,
                            builtInCategory = builtInCategory,
                            userCategory = userCategory
                        )
                    )
                    if (editingItem == null) {
                        label = ""
                        amount = ""
                        builtInCategory = null
                        userCategory = null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (editingItem == null) "저장" else "수정 저장")
        }
        if (editingItem != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("취소")
                }
                OutlinedButton(onClick = { onDelete(editingItem) }, modifier = Modifier.weight(1f)) {
                    Text("삭제")
                }
            }
        }
    }
}

@Composable
private fun FormContainer(
    title: String,
    showCard: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    if (showCard) {
        InputCard(title = title, content = content)
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun InputCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MoneyTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = colors.ink)
            content()
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    name: String,
    title: String = "삭제할까요?",
    subtitle: String = "'${name}' 항목을 삭제하면 되돌릴 수 없어요.",
    confirmLabel: String = "삭제",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MoneyDialog(
        title = title,
        subtitle = subtitle,
        onDismiss = onDismiss,
        buttons = {
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text(confirmLabel) }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("취소") }
        }
    ) {}
}

@Composable
private fun WithdrawalDayPicker(
    selectedDay: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("출금일", fontWeight = FontWeight.Medium)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("매월 ${selectedDay}일")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                fixedExpenseWithdrawalDayOptions.forEach { d ->
                    DropdownMenuItem(
                        text = { Text("${d}일") },
                        onClick = {
                            onSelected(d)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    title: String,
    subtitle: String,
    amountWon: Long,
    ratio: Float? = null,
    accent: Color = MoneyMint,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Edit
) {
    val colors = MoneyTheme.colors
    val rowModifier = if (onAction == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onAction)
    }
    Surface(
        modifier = rowModifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.soft(accent)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.16f))
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title.take(1), color = accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, color = colors.ink)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(formatWon(amountWon), fontWeight = FontWeight.Bold, color = colors.ink)
                    if (actionLabel != null && onAction != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accent.copy(alpha = 0.16f),
                            modifier = Modifier.clickable(onClick = onAction)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = actionIcon,
                                    contentDescription = actionLabel,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    actionLabel,
                                    color = accent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            if (ratio != null) {
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = accent,
                    trackColor = colors.surface.copy(alpha = 0.75f)
                )
            }
        }
    }
}

private fun String.cleanWonOrNull(): Long? =
    replace(",", "").trim().toLongOrNull()?.takeIf { it >= 0 }

private val sampleFixedExpenses = listOf(
    FixedExpensePlan(name = "통신비", amountWon = 70_000, withdrawalDay = 15, accountName = "국민은행"),
    FixedExpensePlan(name = "구독", amountWon = 17_000, withdrawalDay = 20, accountName = "카카오뱅크")
)

private val sampleMonthlyPlanItems = listOf(
    MonthlyPlanItem(label = "월급", amountWon = 2_500_000, type = MonthlyPlanItemType.INCOME),
    MonthlyPlanItem(label = "식비", amountWon = 450_000, type = MonthlyPlanItemType.BUDGET),
    MonthlyPlanItem(label = "교통비", amountWon = 80_000, type = MonthlyPlanItemType.BUDGET)
)
