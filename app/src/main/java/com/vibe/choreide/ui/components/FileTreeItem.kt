package com.vibe.choreide.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibe.choreide.workspace.FileRepository

@Composable
fun FileTreeItem(
    node: FileRepository.FileNode,
    depth: Int,
    onFileClick: (FileRepository.FileNode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !node.isDirectory) { onFileClick(node) }
            .padding(start = (depth * 16).dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (node.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
            contentDescription = null,
            tint = if (node.isDirectory) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            node.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (node.isDirectory) {
        node.children.forEach { child ->
            FileTreeItem(child, depth + 1, onFileClick)
        }
    }
}
