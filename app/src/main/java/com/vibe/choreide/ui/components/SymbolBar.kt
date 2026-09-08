package com.vibe.choreide.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SymbolBar(
    symbols: List<String> = listOf("{", "}", "(", ")", "<", ">", "=", ";", ":", ".", "\"", "\t"),
    onSymbol: (String) -> Unit
) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        symbols.forEach { symbol ->
            TextButton(onClick = { onSymbol(symbol) }) {
                Text(if (symbol == "\t") "Tab" else symbol)
            }
        }
    }
}
