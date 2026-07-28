package com.choiyoonseo.automoney.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.choiyoonseo.automoney.domain.model.Category
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.choiyoonseo.automoney.notification.LastNotificationDiagnostic
import com.choiyoonseo.automoney.notification.NotificationAccessUpdateResult
import com.choiyoonseo.automoney.notification.NotificationDiagnosticResult
import com.choiyoonseo.automoney.notification.NotificationSourceAccess
import com.choiyoonseo.automoney.notification.NotificationSourceOption
import com.choiyoonseo.automoney.notification.NotificationSourceSettingsService
import com.choiyoonseo.automoney.ui.components.EmptyStateVisual
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.choiyoonseo.automoney.data.repository.UserCategoryRepository
import com.choiyoonseo.automoney.domain.category.UserCategoryKind
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    onOpenNotificationSettings: () -> Unit = {},
    notificationAccessEnabled: Boolean? = null,
    lastNotificationDiagnostic: LastNotificationDiagnostic? = null,
    onRunSampleNotificationScenario: (() -> Unit)? = null,
    userCategoryRepository: UserCategoryRepository? = null,
    notificationSourceSettingsService: NotificationSourceSettingsService? = null,
    onOpenNotificationHistory: (() -> Unit)? = null
) {
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
            title = "설정",
            subtitle = "자동 기록과 알림 진단 상태를 확인해요."
        )

        EmptyStateVisual(
            title = "알림으로 자동 기록",
            message = "허용된 금융 앱 알림을 읽어 거래 후보로 바꾸고 확인해요."
        )

        FinanceSectionCard(
            title = "알림 접근 권한",
            subtitle = "금융 앱 알림 자동 분석",
            accent = MoneyBlue,
            icon = Icons.Filled.Settings
        ) {
            Text(
                text = when (notificationAccessEnabled) {
                    true -> "권한 켜짐"
                    false -> "권한 꺼짐"
                    null -> "권한 상태 확인 필요"
                },
                fontWeight = FontWeight.Medium
            )
            Text(
                when (notificationAccessEnabled) {
                    true -> "허용된 금융 앱 알림이 들어오면 자동 기록 후보로 처리해요."
                    false -> "알림 접근 권한을 허용해야 결제/송금 알림을 읽을 수 있어요."
                    null -> "권한 설정에서 AutoMoney 알림 접근을 확인해 주세요."
                }
            )
            Button(
                onClick = onOpenNotificationSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("권한 설정 열기")
            }
        }

        CategoryManagementCard(userCategoryRepository)

        notificationSourceSettingsService?.let { NotificationSourceAppsCard(it) }

        FinanceSectionCard(
            title = "최근 알림 결과",
            subtitle = "마지막 처리 상태",
            accent = MoneyCoral,
            icon = Icons.Filled.CheckCircle
        ) {
            onRunSampleNotificationScenario?.let { runSample ->
                Button(
                    onClick = runSample,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("\ud14c\uc2a4\ud2b8 \uc54c\ub9bc \ub123\uae30")
                }
            }
            if (lastNotificationDiagnostic == null) {
                Text("아직 처리한 금융 앱 알림이 없어요.")
                Text("허용된 금융 앱 알림이 들어오면 여기에서 마지막 결과가 표시돼요.")
            } else {
                Text(lastNotificationDiagnostic.result.toDisplayText(), fontWeight = FontWeight.Medium)
                Text("처리 ${lastNotificationDiagnostic.receivedAt.toDisplayTime()}")
                Text("Source ${lastNotificationDiagnostic.packageName}")
                Text(lastNotificationDiagnostic.title ?: "제목 없음")
                Text(lastNotificationDiagnostic.textPreview)
                lastNotificationDiagnostic.parsedType?.let {
                    Text("Type $it")
                }
                lastNotificationDiagnostic.message?.let { Text(it) }
            }
            onOpenNotificationHistory?.let { openHistory ->
                Button(onClick = openHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("알림 처리 내역")
                }
            }
        }
    }
}


@Composable
private fun NotificationSourceAppsCard(service: NotificationSourceSettingsService) {
    val colors = MoneyTheme.colors
    var options by remember { mutableStateOf(service.options()) }
    var pendingEnable by remember { mutableStateOf<NotificationSourceOption?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        options = service.options()
    }

    fun applyToggle(packageName: String, allowed: Boolean) {
        when (service.setAllowed(packageName, allowed)) {
            NotificationAccessUpdateResult.UPDATED -> {
                errorMessage = null
                options = service.options()
            }
            NotificationAccessUpdateResult.LIMIT_REACHED ->
                errorMessage = "직접 허용은 최대 50개까지예요. 다른 앱을 끄고 다시 시도해 주세요."
            NotificationAccessUpdateResult.INVALID_PACKAGE,
            NotificationAccessUpdateResult.DENIED_PACKAGE ->
                errorMessage = "이 앱은 허용할 수 없어요."
        }
    }

    FinanceSectionCard(
        title = "알림 수집 앱",
        subtitle = "알림 본문을 분석할 앱을 골라요",
        accent = MoneyBlue,
        icon = Icons.Filled.Notifications
    ) {
        val (registered, detected) = options.partition { it.isRegistered }

        Text("기본 지원", fontWeight = FontWeight.Medium, color = colors.inkSub)
        registered.forEach { option ->
            NotificationSourceRow(
                option = option,
                onToggle = { allowed ->
                    if (allowed && !option.isDefaultTrusted) pendingEnable = option
                    else applyToggle(option.packageName, allowed)
                }
            )
        }

        Text("추가 감지 앱", fontWeight = FontWeight.Medium, color = colors.inkSub)
        if (detected.isEmpty()) {
            Text(
                "새 앱 알림이 감지되면 여기에 표시돼요",
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted
            )
        } else {
            detected.forEach { option ->
                NotificationSourceRow(
                    option = option,
                    onToggle = { allowed ->
                        if (allowed) pendingEnable = option
                        else applyToggle(option.packageName, allowed)
                    }
                )
            }
        }

        errorMessage?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MoneyCoral)
        }
        Text(
            "끄면 다음 알림부터 바로 차단돼요.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted
        )
    }

    pendingEnable?.let { option ->
        AlertDialog(
            onDismissRequest = { pendingEnable = null },
            title = { Text("${option.displayName} 알림 분석") },
            text = {
                Text(
                    "이 앱의 알림 본문을 기기 안에서만 분석해 거래 후보로 만들어요. " +
                        "이미 도착한 알림은 처리되지 않고, 켠 뒤 들어오는 다음 알림부터 적용돼요. " +
                        "만들어진 후보는 모두 검토함에서 확인 후 저장돼요."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    applyToggle(option.packageName, true)
                    pendingEnable = null
                }) { Text("허용") }
            },
            dismissButton = {
                TextButton(onClick = { pendingEnable = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun NotificationSourceRow(
    option: NotificationSourceOption,
    onToggle: (Boolean) -> Unit
) {
    val colors = MoneyTheme.colors
    val checked = option.access != NotificationSourceAccess.BLOCKED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(option.displayName, fontWeight = FontWeight.Medium)
            Text(
                option.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                maxLines = 1
            )
            Text(
                text = when {
                    option.isDefaultTrusted -> "기본 지원"
                    checked -> "직접 허용됨 · 전부 검토 후 저장"
                    else -> "감지됨 · 직접 허용 필요"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) MoneyBlue else colors.muted
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryManagementCard(userCategoryRepository: UserCategoryRepository? = null) {
    val context = LocalContext.current
    val store = remember { SharedPreferencesCategoryPreferenceStore(context) }
    var enabledExpense by remember { mutableStateOf(store.enabledExpenseCategories().toSet()) }
    var enabledIncome by remember { mutableStateOf(store.enabledIncomeCategories().toSet()) }
    val colors = MoneyTheme.colors

    FinanceSectionCard(
        title = "분류 관리",
        subtitle = "거래 수정과 직접 입력에서 보여줄 분류를 골라요",
        accent = colors.primary,
        icon = Icons.Filled.Category
    ) {
        Text("지출 분류", fontWeight = FontWeight.Medium, color = colors.inkSub)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            expenseCategoryPool.forEach { category ->
                CategoryToggleChip(
                    label = category.displayName,
                    selected = category in enabledExpense,
                    locked = category == Category.OTHER,
                    onToggle = {
                        val next = if (category in enabledExpense) enabledExpense - category else enabledExpense + category
                        enabledExpense = next + Category.OTHER
                        store.setEnabledExpenseCategories(enabledExpense)
                    }
                )
            }
        }
        Text("수입 분류", fontWeight = FontWeight.Medium, color = colors.inkSub)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            incomeCategoryPool.forEach { category ->
                CategoryToggleChip(
                    label = category.displayName,
                    selected = category in enabledIncome,
                    locked = category == Category.OTHER,
                    onToggle = {
                        val next = if (category in enabledIncome) enabledIncome - category else enabledIncome + category
                        enabledIncome = next + Category.OTHER
                        store.setEnabledIncomeCategories(enabledIncome)
                    }
                )
            }
        }
        Text(
            "\"기타\"는 항상 켜져 있어요. 이미 저장된 거래의 분류는 바뀌지 않아요.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted
        )

        if (userCategoryRepository != null) {
            val scope = rememberCoroutineScope()
            val custom by userCategoryRepository.observeActiveCategories()
                .collectAsState(initial = emptyList())
            var newExpense by remember { mutableStateOf("") }
            var newIncome by remember { mutableStateOf("") }

            Text("직접 만든 분류", fontWeight = FontWeight.Medium, color = colors.inkSub)
            val expenseCustom = custom.filter { it.kind == UserCategoryKind.EXPENSE }
            val incomeCustom = custom.filter { it.kind == UserCategoryKind.INCOME }
            CustomCategoryEditor(
                label = "지출",
                items = expenseCustom.map { it.id to it.name },
                input = newExpense,
                onInput = { newExpense = it },
                onAdd = {
                    val name = newExpense.trim()
                    if (name.isNotEmpty()) {
                        scope.launch { userCategoryRepository.add(UserCategoryKind.EXPENSE, name) }
                        newExpense = ""
                    }
                },
                onDelete = { id -> scope.launch { userCategoryRepository.delete(id) } }
            )
            CustomCategoryEditor(
                label = "수입",
                items = incomeCustom.map { it.id to it.name },
                input = newIncome,
                onInput = { newIncome = it },
                onAdd = {
                    val name = newIncome.trim()
                    if (name.isNotEmpty()) {
                        scope.launch { userCategoryRepository.add(UserCategoryKind.INCOME, name) }
                        newIncome = ""
                    }
                },
                onDelete = { id -> scope.launch { userCategoryRepository.delete(id) } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomCategoryEditor(
    label: String,
    items: List<Pair<Long, String>>,
    input: String,
    onInput: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val colors = MoneyTheme.colors
    Text(label, style = MaterialTheme.typography.labelMedium, color = colors.muted)
    if (items.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (id, name) ->
                Surface(shape = RoundedCornerShape(50), color = colors.soft(colors.primary)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
                    ) {
                        Text(name, color = colors.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            " ×",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onDelete(id) }.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            label = { Text("새 ${label} 분류") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = onAdd, enabled = input.isNotBlank()) { Text("추가") }
    }
}

@Composable
private fun CategoryToggleChip(
    label: String,
    selected: Boolean,
    locked: Boolean,
    onToggle: () -> Unit
) {
    val colors = MoneyTheme.colors
    val accent = colors.primary
    Surface(
        modifier = if (locked) Modifier else Modifier.clickable(onClick = onToggle),
        shape = RoundedCornerShape(50),
        color = if (selected) colors.soft(accent) else colors.canvas
    ) {
        Text(
            text = label,
            color = if (selected) accent else colors.muted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            maxLines = 1
        )
    }
}

private fun NotificationDiagnosticResult.toDisplayText(): String =
    when (this) {
        NotificationDiagnosticResult.SAVED -> "저장됨"
        NotificationDiagnosticResult.DUPLICATE -> "중복"
        NotificationDiagnosticResult.IGNORED -> "무시됨"
        NotificationDiagnosticResult.ERROR -> "오류"
    }

private fun Instant.toDisplayTime(): String =
    DIAGNOSTIC_TIME_FORMATTER.format(this)

private val DIAGNOSTIC_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.of("Asia/Seoul"))
