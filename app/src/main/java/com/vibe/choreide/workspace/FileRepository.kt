package com.vibe.choreide.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository {

    data class FileNode(
        val file: File,
        val name: String,
        val isDirectory: Boolean,
        val children: List<FileNode> = emptyList()
    )

    suspend fun listProjects(): List<File> = withContext(Dispatchers.IO) {
        try {
            File(ProjectGenerator.PROJECTS_ROOT).listFiles()
                ?.filter { it.isDirectory }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } catch (t: Throwable) {
            emptyList()
        }
    }

    suspend fun loadTree(root: File): FileNode = withContext(Dispatchers.IO) {
        buildNode(root)
    }

    private fun buildNode(file: File): FileNode {
        val children = if (file.isDirectory) {
            file.listFiles()
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?.map { buildNode(it) }
                ?: emptyList()
        } else emptyList()
        return FileNode(file, file.name, file.isDirectory, children)
    }

    suspend fun readFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(file.readText())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun writeFile(file: File, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
