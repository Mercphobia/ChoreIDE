package com.vibe.choreide.workspace

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-level project creation entry point used by the wizard:
 * scaffolds the project tree, then injects offline dependencies.
 */
object ProjectFactory {

    suspend fun create(
        context: Context,
        options: WizardOptions,
        onLog: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val scaffoldResult = ProjectScaffolder.scaffold(options)
        scaffoldResult.onSuccess { root ->
            DependencyInjector.inject(context, root, onLog)
        }
        scaffoldResult
    }
}
