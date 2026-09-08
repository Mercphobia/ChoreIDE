package com.vibe.choreide.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vibe.choreide.workspace.BuildDsl
import com.vibe.choreide.workspace.ProjectFactory
import com.vibe.choreide.workspace.ProjectKind
import com.vibe.choreide.workspace.SourceLanguage
import com.vibe.choreide.workspace.WizardOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android Studio-style new project wizard.
 */
@Composable
fun ProjectWizardScreen(
    onProjectCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }

    var projectName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("com.example.myapp") }
    var kind by remember { mutableStateOf(ProjectKind.STANDARD_APP) }
    var language by remember { mutableStateOf(SourceLanguage.KOTLIN) }
    var dsl by remember { mutableStateOf(BuildDsl.KOTLIN_DSL) }
    var minSdk by remember { mutableStateOf(31) }
    var targetSdk by remember { mutableStateOf(34) }
    var busy by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("New Project", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = projectName,
            onValueChange = { projectName = it },
            label = { Text("Project name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = packageName,
            onValueChange = { packageName = it },
            label = { Text("Package name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        WizardChoice(
            title = "Template",
            options = ProjectKind.entries.map { it.label },
            selected = kind.label
        ) { label -> kind = ProjectKind.entries.first { it.label == label } }

        WizardChoice(
            title = "Language",
            options = SourceLanguage.entries.map { it.label },
            selected = language.label
        ) { label -> language = SourceLanguage.entries.first { it.label == label } }

        WizardChoice(
            title = "Build DSL",
            options = BuildDsl.entries.map { it.label },
            selected = dsl.label
        ) { label -> dsl = BuildDsl.entries.first { it.label == label } }

        WizardChoice(
            title = "Min SDK",
            options = listOf("28", "29", "30", "31", "32", "33", "34"),
            selected = minSdk.toString()
        ) { minSdk = it.toInt() }

        WizardChoice(
            title = "Target SDK",
            options = listOf("31", "32", "33", "34", "35"),
            selected = targetSdk.toString()
        ) { targetSdk = it.toInt() }

        Button(
            onClick = {
                busy = true
                log = ""
                val options = WizardOptions(
                    projectName = projectName,
                    packageName = packageName,
                    kind = kind,
                    language = language,
                    dsl = dsl,
                    minSdk = minSdk,
                    targetSdk = targetSdk
                )
                scope.launch {
                    val result = ProjectFactory.create(context, options) { line ->
                        log += line + "\n"
                    }
                    busy = false
                    result
                        .onSuccess { onProjectCreated(it.absolutePath) }
                        .onFailure { log += "error: ${it.message}\n" }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = projectName.isNotBlank() && packageName.contains('.') && !busy
        ) {
            if (busy) CircularProgressIndicator() else Text("Create Project")
        }

        if (log.isNotEmpty()) {
            Text(
                log,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WizardChoice(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}
