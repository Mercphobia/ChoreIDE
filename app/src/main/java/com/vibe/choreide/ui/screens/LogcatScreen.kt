package com.vibe.choreide.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.choreide.system.LogcatManager
import kotlinx.coroutines.rememberCoroutineScope

@Composable
fun LogcatScreen() {
    val manager = remember { LogcatManager() }
    val lines by manager.lines.collectAsState()
    val active by manager.active.collectAsState()
    var errorsOnly by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    DisposableEffect(Unit) {
        onDispose { manager.stop() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = errorsOnly,
                onClick = { errorsOnly = !errorsOnly },
                label = { Text("Errors/FC only") }
            )
            Button(
                onClick = {
                    if (active) manager.stop()
                    else manager.start(scope, filterTag = "", errorsOnly = errorsOnly)
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(if (active) "Stop" else "Start")
            }
            Button(
                onClick = { manager.clear() },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Clear")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            items(lines) { line ->
                Text(
                    line.text,
                    color = if (line.isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}
