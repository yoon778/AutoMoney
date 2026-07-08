package com.choiyoonseo.automoney.ui.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditTransactionScreen(
    merchant: String,
    amountWon: Long,
    currentCategory: String,
    onSaveThisOnly: (String) -> Unit,
    onSaveAsRule: (String) -> Unit
) {
    var category by remember { mutableStateOf(currentCategory) }

    Column(Modifier.padding(16.dp)) {
        Text(text = merchant)
        Text(text = "%,d원".format(amountWon))
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("분류") }
        )
        Row(Modifier.padding(top = 12.dp)) {
            Button(onClick = { onSaveThisOnly(category) }) {
                Text("이번 거래만 수정")
            }
            Button(
                onClick = { onSaveAsRule(category) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("앞으로도 적용")
            }
        }
    }
}
