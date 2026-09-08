package com.vibe.choreide.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.choreide.ui.components.FileTreeItem
import com.vibe.choreide.ui.components.GlassPanel
import com.vibe.choreide.workspace.FileRepository
import com.vibe.choreide.workspace.ProjectManager

@Composable
fun ProjectScreen(
    onOpenFile: () -> Unit = {},
    projectManager: ProjectManager = viewModel()
) {
    val state by projectManager.state.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasStoragePermission()) }
    var tree by remember { mutableStateOf<FileRepository.FileNode?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasPermission = hasStoragePermission() }

    LaunchedEffect(hasPermission) {
        if (hasPermission) projectManager.refreshProjects()
    }

    LaunchedEffect(state.currentProject) {
        val project = state.currentProject ?: return@LaunchedEffect
        tree = FileRepository().loadTree(project)
    }

    if (!hasPermission) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Storage access is required to manage projects.",
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = {
                    try {
                        val intent = if (Build.VERSION.SDK_INT >= 30) {
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        }
                        permissionLauncher.launch(intent)
                    } catch (t: Throwable) {
                        // ignore
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Grant storage permission")
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Projects",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.projects) { project ->
                val selected = project == state.currentProject
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { projectManager.openProject(project) }
                ) {
                    Text(
                        project.name,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        tree?.let { root ->
            Text(
                "Files",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                root.children.forEach { node ->
                    FileTreeItem(node, depth = 0) { clicked ->
                        projectManager.openFile(clicked.file)
                        onOpenFile()
                    }
                }
            }
        }
    }
}

private fun hasStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= 30) {
        Environment.isExternalStorageManager()
    } else true
}
