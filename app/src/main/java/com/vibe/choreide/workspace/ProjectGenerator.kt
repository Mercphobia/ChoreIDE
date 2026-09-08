package com.vibe.choreide.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ProjectGenerator {

    const val PROJECTS_ROOT = "/sdcard/ChoreProjects"

    suspend fun generate(projectName: String, template: AospTemplate): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val safeName = projectName.trim().replace(Regex("[^A-Za-z0-9_-]"), "_")
                require(safeName.isNotEmpty()) { "Project name must not be empty" }

                val projectDir = File(PROJECTS_ROOT, safeName)
                val layoutDir = File(projectDir, "res/layout")
                layoutDir.mkdirs()

                val target = File(layoutDir, template.fileName)
                target.writeText(template.xml)

                Result.success(projectDir)
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
}
