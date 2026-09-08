package com.vibe.choreide.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.vibe.choreide.editor.SoraEditorWrapper
import com.vibe.choreide.workspace.ProjectManager

@Composable
fun EditorScreen(
    projectManager: ProjectManager = viewModel()
) {
    val state by projectManager.state.collectAsState()
    var localText by remember(state.currentFile) { mutableStateOf(state.currentFileContent) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.currentFile?.name ?: "No file open",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { projectManager.saveCurrentFile(localText) },
                enabled = state.currentFile != null
            ) {
                Text("Save")
            }
        }

        SoraEditorWrapper(
            text = localText,
            onTextChanged = { localText = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
