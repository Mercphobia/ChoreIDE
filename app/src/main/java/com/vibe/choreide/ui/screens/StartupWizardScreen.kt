package com.vibe.choreide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vibe.choreide.system.SandboxDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * First-launch setup: downloads the on-device toolchain into the private
 * sandbox. Shown once; skipped automatically when the sandbox is ready.
 */
@Composable
fun StartupWizardScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }

    var status by remember { mutableStateOf("Preparing sandbox environment...") }
    var progress by remember { mutableStateOf(0f) }
    var running by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ChoreIDE Setup", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Downloading the on-device compiler, RE plugins and support libraries " +
                "into the private app sandbox. This only happens once.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Button(
            onClick = {
                if (running) return@Button
                running = true
                scope.launch {
                    val report = SandboxDownloader.downloadAll(context) { pct, msg ->
                        status = msg
                        progress = pct
                    }
                    failed = report.failed
                    running = false
                    if (report.failed.isEmpty()) {
                        onFinished()
                    } else {
                        status = "Some tools failed: " + report.failed.joinToString()
                    }
                }
            },
            enabled = !running
        ) {
            Text(if (running) "Downloading..." else "Start Download")
        }

        if (failed.isNotEmpty()) {
            Button(
                onClick = onFinished,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Continue anyway")
            }
        }
    }
}
