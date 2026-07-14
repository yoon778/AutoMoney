package com.choiyoonseo.automoney.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.choiyoonseo.automoney.domain.assets.BalanceImpact
import com.choiyoonseo.automoney.ui.model.TransactionRowUi
import com.choiyoonseo.automoney.ui.theme.MoneyTheme

data class DetailSheetState(
    val title: String,
    val headlineValue: String? = null,
    val caption: String? = null,
    val summaryLines: List<String> = emptyList(),
    val rows: List<TransactionRowUi>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomSheet(
    state: DetailSheetState,
    onDismiss: () -> Unit,
    onRowClick: ((TransactionRowUi) -> Unit)? = null,
    balanceImpactFor: ((Long) -> BalanceImpact?)? = null
) {
    val colors = MoneyTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text(
                state.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink
            )
            state.headlineValue?.let { headline ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        headline,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    state.caption?.let { caption ->
                        Text(
                            caption,
                            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.muted
                        )
                    }
                }
            }
            if (state.summaryLines.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.summaryLines.forEach { line ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.soft(colors.primary)
                        ) {
                            Text(
                                line,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                if (state.rows.isEmpty()) {
                    Text(
                        "표시할 거래가 없어요",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = colors.muted
                    )
                } else {
                    state.rows.take(20).forEachIndexed { index, row ->
                        if (index > 0) {
                            HorizontalDivider(color = colors.divider)
                        }
                        TransactionRow(
                            transaction = row,
                            balanceImpact = row.id?.let { rowId -> balanceImpactFor?.invoke(rowId) },
                            onClick = if (onRowClick != null && row.id != null) {
                                { onRowClick(row) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
            if (onRowClick != null && state.rows.any { it.id != null }) {
                Text(
                    "항목을 누르면 바로 수정할 수 있어요",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
