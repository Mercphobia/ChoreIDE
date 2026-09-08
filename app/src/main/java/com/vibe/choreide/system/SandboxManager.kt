package com.vibe.choreide.system

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-contained sandbox environment manager.
 *
 * ChoreIDE never touches public storage or Termux. Every tool lives under
 * the app's private filesDir:
 *
 *   filesDir/bin/      aapt2, d8.jar, ecj.jar, apksigner.jar, zipalign
 *   filesDir/plugins/  apktool.jar, jadx.jar
 *   filesDir/libs/     android.jar, androidx + material3 aar/jar
 *   filesDir/projects/ user workspaces
 *
 * Downloads happen once, on demand, at first startup.
 */
object SandboxManager {

    data class ToolSpec(
        val fileName: String,
        val url: String,
        val executable: Boolean,
        val required: Boolean = true
    )

    /** Mirror links for the ARM64 on-device toolchain. */
    private val binTools = listOf(
        ToolSpec("aapt2",
            "https://github.com/Mercphobia/choreide-tools/releases/download/v1/aapt2-arm64",
            executable = true),
        ToolSpec("d8.jar",
            "https://github.com/Mercphobia/choreide-tools/releases/download/v1/d8.jar",
            executable = false),
        ToolSpec("ecj.jar",
            "https://github.com/Mercphobia/choreide-tools/releases/download/v1/ecj.jar",
            executable = false),
        ToolSpec("apksigner.jar",
            "https://github.com/Mercphobia/choreide-tools/releases/download/v1/apksigner.jar",
            executable = false),
        ToolSpec("zipalign",
            "https://github.com/Mercphobia/choreide-tools/releases/download/v1/zipalign-arm64",
            executable = true, required = false)
    )

    private val pluginTools = listOf(
        ToolSpec("apktool.jar",
            "https://github.com/Mercphobia/choreide-tools/releases/download/v1/apktool.jar",
            executable = false),
        ToolSpec("jadx.jar",
            "https://github.com/Mercphobia/choreide-tools/releases/download/v1/jadx.jar",
            executable = false)
    )

    private val libTools = listOf(
        ToolSpec("android.jar",
            "https://github.com/Mercphobia/choreide-tools/releases/download/v1/android-34.jar",
            executable = false)
    )

    fun binDir(context: Context) = File(context.filesDir, "bin")
    fun pluginsDir(context: Context) = File(context.filesDir, "plugins")
    fun libsDir(context: Context) = File(context.filesDir, "libs")
    fun projectsDir(context: Context) = File(context.filesDir, "projects")

    data class SetupReport(
        val downloaded: Int,
        val skipped: Int,
        val failed: List<String>
    )

    fun isSandboxReady(context: Context): Boolean {
        val coreTools = binTools.filter { it.required }
        return coreTools.all { File(binDir(context), it.fileName).exists() }
    }

    suspend fun setup(
        context: Context,
        onProgress: (String, Float) -> Unit
    ): SetupReport = withContext(Dispatchers.IO) {
        binDir(context).mkdirs()
        pluginsDir(context).mkdirs()
        libsDir(context).mkdirs()
        projectsDir(context).mkdirs()

        var downloaded = 0
        var skipped = 0
        val failed = mutableListOf<String>()

        val jobs = listOf(
            Triple(binTools, binDir(context), "bin"),
            Triple(pluginTools, pluginsDir(context), "plugins"),
            Triple(libTools, libsDir(context), "libs")
        )

        val totalFiles = jobs.sumOf { it.first.size }.toFloat().coerceAtLeast(1f)
        var processed = 0

        for ((tools, dir, label) in jobs) {
            for (tool in tools) {
                val target = File(dir, tool.fileName)
                val step = 1f / totalFiles
                if (target.exists() && target.length() > 0) {
                    skipped++
                    processed++
                    onProgress("[$label] ${tool.fileName} already present", processed / totalFiles)
                    continue
                }
                onProgress("[$label] downloading ${tool.fileName}...", processed / totalFiles)
                val ok = download(tool.url, target) { msg ->
                    onProgress("[$label] ${tool.fileName}: $msg", processed / totalFiles)
                }
                if (ok) {
                    if (tool.executable) {
                        target.setExecutable(true, false)
                        target.setReadable(true, false)
                    }
                    downloaded++
                } else {
                    if (tool.required) failed += tool.fileName
                    target.delete()
                }
                processed++
                onProgress("[$label] ${tool.fileName} done", (processed * step).coerceAtMost(1f))
            }
        }

        onProgress("sandbox ready: $downloaded downloaded, $skipped cached, ${failed.size} failed", 1f)
        SetupReport(downloaded, skipped, failed)
    }

    private fun download(url: String, target: File, onProgress: (String) -> Unit): Boolean {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return false
            }
            val total = conn.contentLengthLong
            val tmp = File(target.parentFile, target.name + ".part")
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var acc = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        acc += read
                        if (total > 0) {
                            val pct = (acc * 100 / total).toInt()
                            if (pct % 25 == 0) onProgress("$pct%")
                        }
                    }
                }
            }
            conn.disconnect()
            tmp.renameTo(target)
            true
        } catch (t: Throwable) {
            onProgress("error: ${t.message}")
            false
        }
    }
}
