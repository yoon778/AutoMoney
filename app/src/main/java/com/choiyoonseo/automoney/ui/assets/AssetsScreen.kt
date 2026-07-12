package com.choiyoonseo.automoney.ui.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
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
import com.choiyoonseo.automoney.domain.assets.AssetAccount
import com.choiyoonseo.automoney.domain.assets.AssetAccountKind
import com.choiyoonseo.automoney.domain.assets.BankProvider
import com.choiyoonseo.automoney.domain.assets.FixedExpensePlan
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItem
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItemType
import com.choiyoonseo.automoney.domain.assets.assetOverviewBalanceHelper
import com.choiyoonseo.automoney.domain.assets.buildAssetOverview
import com.choiyoonseo.automoney.domain.assets.fixedExpenseWithdrawalDayOptions
import com.choiyoonseo.automoney.domain.assets.validatedForSave
import com.choiyoonseo.automoney.ui.components.AutoClearMessageEffect
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.IllustratedSummaryCard
import com.choiyoonseo.automoney.ui.components.MetricTile
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyDialog
import com.choiyoonseo.automoney.ui.components.MoneyPickerField
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.MoneyGreen
import com.choiyoonseo.automoney.ui.components.MoneyMint
import com.choiyoonseo.automoney.ui.components.MoneyMuted
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import com.choiyoonseo.automoney.ui.components.accountAccentForName
import com.choiyoonseo.automoney.ui.components.categoryAccentForName
import com.choiyoonseo.automoney.ui.model.MetricTileUi
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import com.choiyoonseo.automoney.ui.model.formatWon
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private enum class AssetSection(val label: String) {
    ACCOUNTS("계좌"),
    FIXED("고정지출"),
    PLAN("월계획")
}

@Composable
fun AssetsScreen(
    padding: PaddingValues,
    assetRepository: AssetRepository? = null
) {
    val scope = rememberCoroutineScope()
    val accounts by remember(assetRepository) {
        assetRepository?.observeAccounts() ?: flowOf(sampleAccounts)
    }.collectAsState(initial = emptyList())
    val fixedExpenses by remember(assetRepository) {
        assetRepository?.observeFixedExpenses() ?: flowOf(sampleFixedExpenses)
    }.collectAsState(initial = emptyList())
    val monthlyPlans by remember(assetRepository) {
        assetRepository?.observeMonthlyPlanItems() ?: flowOf(sampleMonthlyPlanItems)
    }.collectAsState(initial = emptyList())
    val overview = remember(accounts, fixedExpenses, monthlyPlans) {
        buildAssetOverview(accounts, fixedExpenses, monthlyPlans)
    }
    var selectedSection by remember { mutableStateOf(AssetSection.ACCOUNTS) }
    var message by remember { mutableStateOf<String?>(null) }
    AutoClearMessageEffect(message) {
        message = null
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
            title = "자산",
            subtitle = "계좌 잔액, 고정지출, 이번 달 계획을 한곳에서 봐요"
        )

        message?.let { AssistChip(onClick = {}, label = { Text(it) }) }

        IllustratedSummaryCard(
            title = "총 계좌 잔액",
            value = formatWon(overview.totalAssetsWon),
            helper = assetOverviewBalanceHelper(accounts.size),
            accent = MoneyMint,
            icon = Icons.Filled.AccountBalance
        )

        val incomeSet = overview.totalIncomeWon > 0
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                MetricTileUi(
                    "월 고정지출",
                    formatWon(overview.totalFixedExpenseWon),
                    progress = overview.fixedExpenseRatio,
                    helper = if (incomeSet) "매월 자동 출금 · 수입의 ${(overview.fixedExpenseRatio * 100).toInt()}%" else "매월 자동 출금 · 수입 미등록"
                ),
                MoneyCoral,
                Modifier.weight(1f)
            )
            MetricTile(
                MetricTileUi(
                    "생활예산",
                    formatWon(overview.totalBudgetWon),
                    progress = overview.budgetRatio,
                    helper = if (incomeSet) "월계획 예산 합 · 수입의 ${(overview.budgetRatio * 100).toInt()}%" else "월계획 예산 합 · 수입 미등록"
                ),
                MoneyBlue,
                Modifier.weight(1f)
            )
        }

        TabRow(
            selectedTabIndex = selectedSection.ordinal,
            containerColor = MoneyTheme.colors.surface,
            contentColor = MoneyTheme.colors.primary
        ) {
            AssetSection.entries.forEach { section ->
                Tab(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    text = { Text(section.label) }
                )
            }
        }

        when (selectedSection) {
            AssetSection.ACCOUNTS -> AccountsPanel(
                accounts = accounts,
                onSave = { account ->
                    val repository = assetRepository
                    if (repository == null) {
                        message = "미리보기에서는 저장하지 않아요."
                    } else {
                        scope.launch {
                            repository.saveAccount(account)
                            message = "${account.name} 계좌를 저장했어요."
                        }
                    }
                }
            )

            AssetSection.FIXED -> FixedExpensePanel(
                plans = fixedExpenses,
                accounts = accounts,
                onSave = { plan ->
                    val repository = assetRepository
                    if (repository == null) {
                        message = "미리보기에서는 저장하지 않아요."
                    } else {
                        scope.launch {
                            repository.saveFixedExpense(plan)
                            message = "${plan.name} 고정지출을 저장했어요."
                        }
                    }
                }
            )

            AssetSection.PLAN -> MonthlyPlanPanel(
                items = monthlyPlans,
                onSave = { item ->
                    val repository = assetRepository
                    if (repository == null) {
                        message = "미리보기에서는 저장하지 않아요."
                    } else {
                        scope.launch {
                            repository.saveMonthlyPlanItem(item)
                            message = "${item.label} 계획을 저장했어요."
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun AccountsPanel(
    accounts: List<AssetAccount>,
    onSave: (AssetAccount) -> Unit
) {
    var editingAccount by remember(accounts) { mutableStateOf<AssetAccount?>(null) }
    val maxBalance = remember(accounts) { accounts.maxOfOrNull { it.balanceWon }?.coerceAtLeast(1) ?: 1 }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceSectionCard(
            title = "계좌 잔액",
            subtitle = "잔고를 수정하면 총 자산도 바로 바뀌어요",
            accent = MoneyMint,
            icon = Icons.Filled.AccountBalance
        ) {
            accounts.forEach { account ->
                AssetRow(
                    title = account.name,
                    subtitle = assetAccountMetadataLabel(account),
                    amountWon = account.balanceWon,
                    ratio = account.balanceWon.toFloat() / maxBalance.toFloat(),
                    accent = accountAccentForName(account.name),
                    actionLabel = "수정",
                    onAction = { editingAccount = account }
                )
            }
            if (accounts.isEmpty()) {
                Text("아직 등록된 계좌가 없어요.")
            }
        }
        AccountInputCard(onSave)
    }

    editingAccount?.let { account ->
        AccountEditDialog(
            account = account,
            onDismiss = { editingAccount = null },
            onSave = { updated ->
                onSave(updated)
                editingAccount = null
            }
        )
    }
}

@Composable
private fun AccountInputCard(onSave: (AssetAccount) -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(AssetAccountKind.BANK) }
    var kindExpanded by remember { mutableStateOf(false) }
    var bankProvider by remember { mutableStateOf<BankProvider?>(null) }
    var accountNumberInput by remember { mutableStateOf("") }
    var providerLabelInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    InputCard(title = "계좌 추가") {
        OutlinedTextField(name, { name = it }, label = { Text("계좌 이름") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = balance,
            onValueChange = { balance = it },
            label = { Text("잔액") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.fillMaxWidth()) {
            MoneyPickerField(
                label = "종류",
                value = kind.label,
                onClick = { kindExpanded = true }
            )
            DropdownMenu(
                expanded = kindExpanded,
                onDismissRequest = { kindExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                AssetAccountKind.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            kind = option
                            if (option != AssetAccountKind.BANK) {
                                bankProvider = null
                                accountNumberInput = ""
                            }
                            if (option != AssetAccountKind.SECURITIES && option != AssetAccountKind.PAY) {
                                providerLabelInput = ""
                            }
                            kindExpanded = false
                        }
                    )
                }
            }
        }
        if (kind == AssetAccountKind.BANK) {
            BankProviderPicker(
                selectedProvider = bankProvider,
                onSelected = { bankProvider = it }
            )
            if (bankProvider != null) {
                OutlinedTextField(
                    value = accountNumberInput,
                    onValueChange = { accountNumberInput = it },
                    label = { Text("계좌번호") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (kind == AssetAccountKind.SECURITIES || kind == AssetAccountKind.PAY) {
            ProviderLabelSection(
                kind = kind,
                value = providerLabelInput,
                onValueChange = { providerLabelInput = it }
            )
        }
        errorMessage?.let {
            Text(it, color = MoneyTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                val amount = balance.cleanWonOrNull()
                if (name.isNotBlank() && amount != null) {
                    try {
                        onSave(
                            createAssetAccountFromForm(
                                name = name,
                                balanceWon = amount,
                                kind = kind,
                                bankProvider = bankProvider,
                                accountNumberInput = accountNumberInput,
                                providerLabel = providerLabelInput
                            )
                        )
                        name = ""
                        balance = ""
                        bankProvider = null
                        accountNumberInput = ""
                        providerLabelInput = ""
                        errorMessage = null
                    } catch (e: IllegalArgumentException) {
                        errorMessage = e.message ?: "입력값을 확인해 주세요."
                    }
                } else {
                    errorMessage = "계좌 이름과 잔액을 확인해 주세요."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }
    }
}

@Composable
private fun AccountEditDialog(
    account: AssetAccount,
    onDismiss: () -> Unit,
    onSave: (AssetAccount) -> Unit
) {
    var name by remember(account.id) { mutableStateOf(account.name) }
    var balance by remember(account.id) { mutableStateOf(account.balanceWon.toString()) }
    var kind by remember(account.id) { mutableStateOf(account.kind) }
    var kindExpanded by remember(account.id) { mutableStateOf(false) }
    var bankProvider by remember(account.id) { mutableStateOf(account.bankProvider) }
    var accountNumberInput by remember(account.id) { mutableStateOf("") }
    var providerLabelInput by remember(account.id) { mutableStateOf(account.providerLabel ?: "") }
    var errorMessage by remember(account.id) { mutableStateOf<String?>(null) }

    MoneyDialog(
        title = "계좌 수정",
        subtitle = account.name,
        onDismiss = onDismiss,
        buttons = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("취소")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val cleanBalance = balance.cleanWonOrNull()
                        if (cleanBalance == null) {
                            errorMessage = "잔액을 확인해 주세요."
                            return@Button
                        }
                        try {
                            onSave(
                                updateAssetAccountFromForm(
                                    account = account,
                                    name = name,
                                    balanceWon = cleanBalance,
                                    kind = kind,
                                    bankProvider = bankProvider,
                                    accountNumberInput = accountNumberInput
                                )
                            )
                        } catch (e: IllegalArgumentException) {
                            errorMessage = e.message ?: "입력값을 확인해 주세요."
                        }
                    }
                ) {
                    Text("저장")
                }
            }
        }
    ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("계좌 이름") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("현재 잔액") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.fillMaxWidth()) {
                    MoneyPickerField(
                        label = "종류",
                        value = kind.label,
                        onClick = { kindExpanded = true }
                    )
                    DropdownMenu(
                        expanded = kindExpanded,
                        onDismissRequest = { kindExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssetAccountKind.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    kind = option
                                    if (option != AssetAccountKind.BANK) {
                                        bankProvider = null
                                        accountNumberInput = ""
                                    }
                                    kindExpanded = false
                                }
                            )
                        }
                    }
                }
                if (kind == AssetAccountKind.BANK) {
                    BankProviderPicker(
                        selectedProvider = bankProvider,
                        onSelected = { bankProvider = it }
                    )
                    account.accountLast4?.let { last4 ->
                        Text(
                            "등록된 계좌번호 ****$last4 · 바꾸려면 새 번호를 입력하세요",
                            style = MaterialTheme.typography.labelSmall,
                            color = MoneyTheme.colors.muted
                        )
                    }
                    if (bankProvider != null) {
                        OutlinedTextField(
                            value = accountNumberInput,
                            onValueChange = { accountNumberInput = it },
                            label = { Text("새 계좌번호") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (kind == AssetAccountKind.SECURITIES || kind == AssetAccountKind.PAY) {
                    ProviderLabelSection(
                        kind = kind,
                        value = providerLabelInput,
                        onValueChange = { providerLabelInput = it }
                    )
                }
                errorMessage?.let {
                    Text(it, color = MoneyTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
                }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderLabelSection(
    kind: AssetAccountKind,
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = MoneyTheme.colors
    val presets = providerPresetsFor(kind.label)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val selected = value.trim() == preset
                Surface(
                    modifier = Modifier.clickable { onValueChange(preset) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) colors.soft(colors.primary) else colors.canvas
                ) {
                    Text(
                        preset,
                        color = if (selected) colors.primary else colors.muted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        maxLines = 1
                    )
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("회사 이름 (직접 입력 가능)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BankProviderPicker(
    selectedProvider: BankProvider?,
    onSelected: (BankProvider) -> Unit
) {
    val colors = MoneyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("은행", style = MaterialTheme.typography.labelMedium, color = colors.muted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BankProvider.entries.forEach { provider ->
                val selected = provider == selectedProvider
                Surface(
                    modifier = Modifier.clickable { onSelected(provider) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) colors.soft(colors.primary) else colors.canvas
                ) {
                    Text(
                        provider.displayName,
                        color = if (selected) colors.primary else colors.muted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun FixedExpensePanel(
    plans: List<FixedExpensePlan>,
    accounts: List<AssetAccount>,
    onSave: (FixedExpensePlan) -> Unit
) {
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
                    ratio = plan.amountWon.toFloat() / (plans.maxOfOrNull { it.amountWon }?.coerceAtLeast(1)?.toFloat() ?: 1f),
                    accent = categoryAccentForName(plan.name)
                )
            }
            if (plans.isEmpty()) {
                Text("아직 등록된 고정지출이 없어요.")
            }
        }
        FixedExpenseInputCard(accounts = accounts, onSave = onSave)
    }
}

@Composable
private fun FixedExpenseInputCard(
    accounts: List<AssetAccount>,
    onSave: (FixedExpensePlan) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(1) }
    var accountName by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.name.orEmpty()) }
    var accountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id) }

    InputCard(title = "고정지출 추가") {
        OutlinedTextField(name, { name = it }, label = { Text("이름") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = it }, label = { Text("금액") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        WithdrawalDayPicker(selectedDay = day, onSelected = { day = it })
        if (accounts.isEmpty()) {
            OutlinedTextField(accountName, { accountName = it; accountId = null }, label = { Text("출금 계좌") }, modifier = Modifier.fillMaxWidth())
        } else {
            AssetAccountNamePicker(
                label = "출금 계좌",
                selectedName = accountName,
                accounts = accounts,
                onSelected = { accountName = it.name; accountId = it.id }
            )
        }
        Button(
            onClick = {
                val cleanAmount = amount.cleanWonOrNull()
                if (name.isNotBlank() && accountName.isNotBlank() && cleanAmount != null && day in fixedExpenseWithdrawalDayOptions) {
                    onSave(
                        FixedExpensePlan(
                            name = name,
                            amountWon = cleanAmount,
                            withdrawalDay = day,
                            accountName = accountName,
                            accountId = accountId
                        ).validatedForSave()
                    )
                    name = ""
                    amount = ""
                    day = 1
                    accountName = ""
                    accountId = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }
    }
}

@Composable
private fun MonthlyPlanPanel(
    items: List<MonthlyPlanItem>,
    onSave: (MonthlyPlanItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceSectionCard(
            title = "월계획",
            subtitle = "예산 항목 합계가 위 '생활예산'이 돼요",
            accent = MoneyBlue,
            icon = Icons.Filled.BarChart
        ) {
            items.forEach { item ->
                AssetRow(
                    title = item.label,
                    subtitle = item.type.label,
                    amountWon = item.amountWon,
                    ratio = item.amountWon.toFloat() / (items.maxOfOrNull { it.amountWon }?.coerceAtLeast(1)?.toFloat() ?: 1f),
                    accent = if (item.type == MonthlyPlanItemType.INCOME) MoneyGreen else categoryAccentForName(item.label)
                )
            }
            if (items.isEmpty()) {
                Text("아직 월계획이 없어요.")
            }
        }
        MonthlyPlanInputCard(onSave)
    }
}

@Composable
private fun MonthlyPlanInputCard(onSave: (MonthlyPlanItem) -> Unit) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MonthlyPlanItemType.BUDGET) }

    InputCard(title = "월계획 추가") {
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
        Button(
            onClick = {
                val cleanAmount = amount.cleanWonOrNull()
                if (label.isNotBlank() && cleanAmount != null) {
                    onSave(MonthlyPlanItem(label = label.trim(), amountWon = cleanAmount, type = type))
                    label = ""
                    amount = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }
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
private fun AssetAccountNamePicker(
    label: String,
    selectedName: String,
    accounts: List<AssetAccount>,
    onSelected: (AssetAccount) -> Unit
) {
    var expanded by remember(label, accounts) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.Medium)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedName.ifBlank { "계좌 선택" })
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text("${account.name} · ${formatWon(account.balanceWon)}") },
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
private fun AssetRow(
    title: String,
    subtitle: String,
    amountWon: Long,
    ratio: Float = 0f,
    accent: Color = MoneyMint,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
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
                                    imageVector = Icons.Filled.Edit,
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

private fun String.cleanWonOrNull(): Long? =
    replace(",", "").trim().toLongOrNull()?.takeIf { it >= 0 }

private val sampleAccounts = listOf(
    AssetAccount(name = "국민은행", balanceWon = 3_799_195),
    AssetAccount(name = "카카오뱅크", balanceWon = 110_067),
    AssetAccount(name = "네이버페이", balanceWon = 40_000, kind = AssetAccountKind.PAY)
)

private val sampleFixedExpenses = listOf(
    FixedExpensePlan(name = "통신비", amountWon = 70_000, withdrawalDay = 15, accountName = "국민은행"),
    FixedExpensePlan(name = "구독", amountWon = 17_000, withdrawalDay = 20, accountName = "카카오뱅크")
)

private val sampleMonthlyPlanItems = listOf(
    MonthlyPlanItem(label = "월급", amountWon = 2_500_000, type = MonthlyPlanItemType.INCOME),
    MonthlyPlanItem(label = "식비", amountWon = 450_000, type = MonthlyPlanItemType.BUDGET),
    MonthlyPlanItem(label = "교통비", amountWon = 80_000, type = MonthlyPlanItemType.BUDGET)
)
