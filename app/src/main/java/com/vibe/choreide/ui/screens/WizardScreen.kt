package com.vibe.choreide.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.choreide.ui.components.GlassPanel
import com.vibe.choreide.workspace.AospTemplate
import com.vibe.choreide.workspace.ProjectManager

@Composable
fun WizardScreen(
    onProjectCreated: (String) -> Unit,
    projectManager: ProjectManager = viewModel()
) {
    val state by projectManager.state.collectAsState()
    var projectName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<AospTemplate>(AospTemplate.SystemUI) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "New Project",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = projectName,
            onValueChange = { projectName = it },
            label = { Text("Project name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            "Choose a template",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AospTemplate.all) { template ->
                val selected = template.id == selectedTemplate.id
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTemplate = template }
                ) {
                    Text(
                        template.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            onClick = {
                projectManager.createProject(projectName, selectedTemplate) { result ->
                    result.onSuccess { onProjectCreated(it.absolutePath) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = projectName.isNotBlank() && !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Create Project")
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
