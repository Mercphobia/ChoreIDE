package com.vibe.choreide.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TerminalScreen() {
    var output by remember { mutableStateOf("ChoreIDE shell. Root: " +
            (try { Shell.isAppGrantedRoot() == true } catch (t: Throwable) { false }) + "\n") }
    var command by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val scroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            output,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scroll)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("command") }
            )
            Button(
                onClick = {
                    val cmd = command.trim()
                    if (cmd.isEmpty() || running) return@Button
                    running = true
                    output += "$ $cmd\n"
                    command = ""
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            try {
                                Shell.cmd(cmd).exec()
                            } catch (t: Throwable) {
                                null
                            }
                        }
                        output += when {
                            result == null -> "[error] shell failed\n"
                            result.out.isNotEmpty() -> result.out.joinToString("\n") + "\n"
                            result.err.isNotEmpty() -> result.err.joinToString("\n") + "\n"
                            else -> "[exit ${result.code}]\n"
                        }
                        running = false
                    }
                },
                modifier = Modifier.padding(start = 8.dp),
                enabled = !running
            ) {
                Text("Run")
            }
        }
    }
}
