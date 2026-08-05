package com.choiyoonseo.automoney.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.data.repository.NotificationHistoryRepository
import com.choiyoonseo.automoney.domain.manual.SaveMissedNotificationTransactionUseCase
import com.choiyoonseo.automoney.domain.time.AppDateZoneId
import com.choiyoonseo.automoney.ui.components.AutoClearMessageEffect
import com.choiyoonseo.automoney.ui.components.EmptyStateVisual
import com.choiyoonseo.automoney.ui.components.FinanceLazySectionCard
import com.choiyoonseo.automoney.ui.components.FinanceSectionCard
import com.choiyoonseo.automoney.ui.components.MoneyBlue
import com.choiyoonseo.automoney.ui.components.MoneyCoral
import com.choiyoonseo.automoney.ui.components.MoneyDialog
import com.choiyoonseo.automoney.ui.components.ScreenTitle
import com.choiyoonseo.automoney.ui.model.NotificationHistoryRowUi
import com.choiyoonseo.automoney.ui.model.formatWon
import com.choiyoonseo.automoney.ui.model.toUi
import com.choiyoonseo.automoney.ui.theme.MoneyTheme
import com.choiyoonseo.automoney.ui.transactions.ManualTransactionForm
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 최근 알림이 어떻게 처리됐는지만 보여준다.
 * 알림 원문·진단 message·계좌 정보는 저장도 표시도 하지 않는다.
 */
@Composable
fun NotificationHistoryScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
    notificationHistoryRepository: NotificationHistoryRepository? = null,
    saveMissedNotificationTransactionUseCase: SaveMissedNotificationTransactionUseCase? = null
) {
    val scope = rememberCoroutineScope()
    val records by remember(notificationHistoryRepository) {
        notificationHistoryRepository?.observeRecent() ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val rows = remember(records) { records.map { it.toUi() } }
    var pendingClear by remember { mutableStateOf(false) }
    var activeManualRow by remember { mutableStateOf<NotificationHistoryRowUi?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    AutoClearMessageEffect(resultMessage) {
        resultMessage = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MoneyTheme.colors.canvas)
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            title = "알림 처리 내역",
            subtitle = "최근 알림을 어떻게 처리했는지 확인해요"
        )

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("설정으로 돌아가기")
        }

        resultMessage?.let { message ->
            FinanceSectionCard(
                title = "처리 결과",
                accent = MoneyCoral,
                icon = Icons.Filled.CheckCircle
            ) {
                Text(message)
            }
        }

        if (rows.isEmpty()) {
            EmptyStateVisual(
                title = "아직 처리한 알림이 없어요",
                message = "허용한 금융 앱 알림이 들어오면 여기에 결과가 쌓여요."
            )
        } else {
            FinanceLazySectionCard(
                title = "최근 알림",
                subtitle = "최근 30일 · 최대 200건",
                accent = MoneyBlue,
                icon = Icons.Filled.CheckCircle,
                modifier = Modifier.weight(1f)
            ) {
                items(rows, key = { it.id }) { row ->
                    NotificationHistoryRow(
                        row = row,
                        enabled = !isSaving && saveMissedNotificationTransactionUseCase != null,
                        onRecordManually = {
                            activeManualRow = row
                            errorMessage = null
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = { pendingClear = true },
                enabled = !isSaving && notificationHistoryRepository != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("전체 삭제")
            }
        }
    }

    if (pendingClear) {
        AlertDialog(
            onDismissRequest = { pendingClear = false },
            title = { Text("알림 처리 내역 삭제") },
            text = { Text("기록된 처리 내역을 모두 지워요. 저장된 거래는 그대로 남아요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingClear = false
                        scope.launch {
                            notificationHistoryRepository?.clear()
                            resultMessage = "처리 내역을 모두 지웠어요."
                        }
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = false }) { Text("취소") }
            }
        )
    }

    activeManualRow?.let { row ->
        val useCase = saveMissedNotificationTransactionUseCase
        MoneyDialog(
            title = "직접 기록",
            subtitle = "${row.sourceLabel} · ${row.receivedAt.toHistoryTime()}",
            onDismiss = {
                activeManualRow = null
                errorMessage = null
            },
            buttons = {
                OutlinedButton(
                    onClick = {
                        activeManualRow = null
                        errorMessage = null
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("취소")
                }
            }
        ) {
            ManualTransactionForm(
                isSaving = isSaving,
                initialAmountWon = row.amountWon,
                onSave = { type, amountWon, category, memo, occurredAt, _, _ ->
                    if (useCase == null) return@ManualTransactionForm
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            useCase.save(
                                historyId = row.id,
                                type = type,
                                amountWon = amountWon,
                                categoryText = category,
                                memo = memo,
                                occurredAt = occurredAt
                            )
                            resultMessage = "직접 기록했어요."
                            activeManualRow = null
                        } catch (e: IllegalArgumentException) {
                            errorMessage = e.message ?: "입력값을 확인해 주세요."
                        } catch (e: IllegalStateException) {
                            errorMessage = "이미 처리된 알림이에요."
                        } catch (e: RuntimeException) {
                            errorMessage = "기록 중 문제가 생겼어요."
                        } finally {
                            isSaving = false
                        }
                    }
                }
            )
            errorMessage?.let {
                Text(it, color = MoneyTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NotificationHistoryRow(
    row: NotificationHistoryRowUi,
    enabled: Boolean,
    onRecordManually: () -> Unit
) {
    val colors = MoneyTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.canvas,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.sourceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink,
                    maxLines = 1
                )
                Text(
                    listOfNotNull(
                        row.resultLabel,
                        row.amountWon?.let(::formatWon),
                        row.receivedAt.toHistoryTime()
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted,
                    maxLines = 1
                )
            }
            if (row.canRecordManually) {
                Button(onClick = onRecordManually, enabled = enabled) {
                    Text("직접 기록")
                }
            }
        }
    }
}

private fun Instant.toHistoryTime(): String =
    atZone(AppDateZoneId).format(notificationHistoryTimeFormatter)

private val notificationHistoryTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 HH:mm")
