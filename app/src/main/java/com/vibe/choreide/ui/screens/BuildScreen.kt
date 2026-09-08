package com.vibe.choreide.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.choreide.compiler.BuildPipelineManager
import com.vibe.choreide.workspace.ProjectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BuildScreen(
    projectManager: ProjectManager = viewModel()
) {
    val state by projectManager.state.collectAsState()
    val context = LocalContext.current
    var log by remember { mutableStateOf("") }
    var building by remember { mutableStateOf(false) }
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val scroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Build: " + (state.currentProject?.name ?: "no project selected"),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = {
                val project = state.currentProject ?: return@Button
                building = true
                log = ""
                scope.launch {
                    BuildPipelineManager.ensureToolchain(context) { log += it + "\n" }
                    val result = BuildPipelineManager.buildApk(context, project) { log += it + "\n" }
                    log += if (result.success) "[ok] build finished\n" else "[fail] build failed\n"
                    building = false
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            enabled = state.currentProject != null && !building
        ) {
            Text(if (building) "Building..." else "Build APK")
        }

        Text(
            log,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scroll)
        )
    }
}
