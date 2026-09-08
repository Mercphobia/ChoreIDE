package com.vibe.choreide.system

import android.content.Context
import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Zero-Download First Launch asset manager.
 *
 * All essential binaries and plugins are pre-bundled inside the APK assets:
 *   assets/bin/     -> aapt2, d8.jar, ecj.jar, apksigner.jar, debug.keystore
 *   assets/plugins/ -> apktool.jar, jadx.jar
 *   assets/libs/    -> androidx/material3 .aar/.jar, android.jar stubs
 *
 * On first launch the entire tree is mirrored into the app's private
 * storage (filesDir) and every executable bit is set so binaries can be
 * invoked directly via ProcessBuilder without any network access.
 */
object AssetManagerHelper {

    private const val PREFS = "chore_assets"
    private const val KEY_VERSION = "assets_version"

    /** Bump this when bundled assets change to force re-extraction. */
    const val ASSETS_VERSION = 1

    data class ExtractReport(
        val extracted: Int,
        val skipped: Int,
        val errors: List<String>
    )

    /** Directories mirrored from assets into private storage. */
    private val assetRoots = listOf("bin", "plugins", "libs")

    suspend fun ensureExtracted(
        context: Context,
        onLog: (String) -> Unit = {}
    ): ExtractReport = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val done = prefs.getInt(KEY_VERSION, 0) >= ASSETS_VERSION

        var extracted = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        if (done) {
            onLog("[assets] up to date (v$ASSETS_VERSION)")
            return@withContext ExtractReport(0, 0, emptyList())
        }

        for (root in assetRoots) {
            try {
                val result = copyAssetTree(
                    context.assets,
                    root,
                    File(context.filesDir, root),
                    onLog
                )
                extracted += result.first
                skipped += result.second
            } catch (t: Throwable) {
                errors += "$root: ${t.message}"
                onLog("[assets] $root failed: ${t.message}")
            }
        }

        if (errors.isEmpty()) {
            prefs.edit().putInt(KEY_VERSION, ASSETS_VERSION).apply()
        }
        onLog("[assets] extracted=$extracted skipped=$skipped errors=${errors.size}")
        ExtractReport(extracted, skipped, errors)
    }

    private fun copyAssetTree(
        assets: AssetManager,
        path: String,
        dest: File,
        onLog: (String) -> Unit
    ): Pair<Int, Int> {
        var extracted = 0
        var skipped = 0
        val children = assets.list(path) ?: return 0 to 0

        if (children.isEmpty()) {
            // Leaf file
            if (!dest.exists() || dest.length() == 0L) {
                dest.parentFile?.mkdirs()
                assets.open(path).use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                // Toolchain binaries and shell scripts must be executable
                if (isExecutableAsset(path)) {
                    dest.setExecutable(true, false)
                    dest.setReadable(true, false)
                }
                onLog("[assets] + $path")
                extracted++
            } else {
                skipped++
            }
            return extracted to skipped
        }

        dest.mkdirs()
        for (child in children) {
            val (e, s) = copyAssetTree(assets, "$path/$child", File(dest, child), onLog)
            extracted += e
            skipped += s
        }
        return extracted to skipped
    }

    private fun isExecutableAsset(path: String): Boolean {
        val name = path.substringAfterLast('/')
        // jars/keystores are invoked via dalvikvm, no exec bit needed
        if (name.endsWith(".jar") || name.endsWith(".keystore")) return false
        if (name.endsWith(".aar")) return false
        // aapt2, clang, d8, shell wrappers etc.
        return !name.contains('.') || name.endsWith(".sh")
    }

    /** Absolute path helpers used by the toolchain runner. */
    fun binDir(context: Context): File = File(context.filesDir, "bin")
    fun pluginsDir(context: Context): File = File(context.filesDir, "plugins")
    fun libsDir(context: Context): File = File(context.filesDir, "libs")

    fun tool(context: Context, name: String): File = File(binDir(context), name)
    fun plugin(context: Context, name: String): File = File(pluginsDir(context), name)
}
