package com.vibe.choreide.workspace

import android.content.Context
import com.vibe.choreide.system.AssetManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offline-first dependency injection.
 *
 * Copies the pre-bundled support libraries (AndroidX + Material 3
 * expressive .aar/.jar files and android.jar / AOSP system stubs) from the
 * app's private libs directory into the new project's app/libs/ folder so
 * the on-device compiler (ecj/aapt2) resolves everything without network.
 */
object DependencyInjector {

    data class InjectReport(
        val copiedLibs: Int,
        val copiedStubs: Int,
        val errors: List<String>
    )

    suspend fun inject(
        context: Context,
        projectRoot: File,
        onLog: (String) -> Unit = {}
    ): InjectReport = withContext(Dispatchers.IO) {
        var libs = 0
        var stubs = 0
        val errors = mutableListOf<String>()

        try {
            // Make sure first-launch extraction already happened
            AssetManagerHelper.ensureExtracted(context, onLog)

            val sourceLibs = AssetManagerHelper.libsDir(context)
            val targetLibs = File(projectRoot, "app/libs")
            targetLibs.mkdirs()

            // 1. Copy every .aar/.jar support library
            sourceLibs.listFiles()
                ?.filter { it.isFile && (it.extension == "aar" || it.extension == "jar") }
                ?.forEach { lib ->
                    try {
                        val target = File(targetLibs, lib.name)
                        if (!target.exists()) {
                            lib.copyTo(target)
                            libs++
                            onLog("[inject] lib ${lib.name}")
                        }
                    } catch (t: Throwable) {
                        errors += "${lib.name}: ${t.message}"
                    }
                }

            // 2. Copy android.jar / AOSP stubs to a stable per-project location
            val stubSource = File(sourceLibs, "android.jar")
            if (stubSource.exists()) {
                val stubTarget = File(projectRoot, "app/libs/android.jar")
                if (!stubTarget.exists()) {
                    stubSource.copyTo(stubTarget)
                    stubs++
                    onLog("[inject] android.jar stub")
                }
            } else {
                onLog("[inject] android.jar stub not bundled; aapt2 will use --auto-add-overlay or platform jar")
            }

            onLog("[inject] libs=$libs stubs=$stubs errors=${errors.size}")
        } catch (t: Throwable) {
            errors += t.message.orEmpty()
            onLog("[inject] failed: ${t.message}")
        }

        InjectReport(libs, stubs, errors)
    }
}
