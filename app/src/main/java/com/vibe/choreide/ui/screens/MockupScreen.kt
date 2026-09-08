package com.vibe.choreide.ui.screens

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.choreide.system.AospMockupInflater
import com.vibe.choreide.workspace.ProjectManager

@Composable
fun MockupScreen(
    projectManager: ProjectManager = viewModel()
) {
    val state by projectManager.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            state.currentFile?.name ?: "Open an XML layout to preview",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (state.currentFileContent.isBlank()) {
            Text(
                "No content. Select an XML file from the Project tab.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context -> FrameLayout(context) },
                update = { container ->
                    container.removeAllViews()
                    try {
                        val view = AospMockupInflater.inflate(
                            container.context,
                            state.currentFileContent
                        )
                        container.addView(view)
                    } catch (t: Throwable) {
                        val errorView = android.widget.TextView(container.context)
                        errorView.text = "XML error: ${t.message}"
                        errorView.setTextColor(android.graphics.Color.RED)
                        container.addView(errorView)
                    }
                }
            )
        }
    }
}
