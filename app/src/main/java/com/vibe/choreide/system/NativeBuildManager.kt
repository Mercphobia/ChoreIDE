package com.vibe.choreide.system

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object NativeBuildManager {

    private fun ndkDir(context: Context): File = File(context.filesDir, "ndk")

    suspend fun ensureNdk(context: Context, onLog: (String) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val dir = ndkDir(context)
                if (!dir.exists()) dir.mkdirs()
                val tools = listOf("clang", "clang++", "lld", "cmake")
                var allPresent = true
                for (tool in tools) {
                    val target = File(dir, tool)
                    if (!target.exists()) {
                        try {
                            context.assets.open("ndk/$tool").use { input ->
                                target.outputStream().use { output -> input.copyTo(output) }
                            }
                            target.setExecutable(true)
                            onLog("extracted ndk/$tool")
                        } catch (t: Throwable) {
                            onLog("missing asset: ndk/$tool")
                            allPresent = false
                        }
                    }
                }
                allPresent
            } catch (t: Throwable) {
                onLog("ndk setup failed: ${t.message}")
                false
            }
        }

    suspend fun compileSharedLibrary(
        context: Context,
        sourceFile: File,
        outputSo: File,
        apiLevel: Int = 28,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val clang = File(ndkDir(context), "clang++")
            if (!clang.exists()) {
                onLog("[error] clang++ not bundled. Add ARM64 binaries under assets/ndk/.")
                return@withContext false
            }

            outputSo.parentFile?.mkdirs()

            val cmd = listOf(
                clang.absolutePath,
                "--target=aarch64-none-linux-android$apiLevel",
                "-fPIC",
                "-shared",
                "-O2",
                sourceFile.absolutePath,
                "-o", outputSo.absolutePath
            )

            onLog("[ndk] " + cmd.joinToString(" "))
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().forEachLine { onLog(it) }
            process.waitFor()

            val ok = process.exitValue() == 0 && outputSo.exists()
            onLog(if (ok) "[ndk] built ${outputSo.name}" else "[ndk] compile failed")
            ok
        } catch (t: Throwable) {
            onLog("[ndk] ${t.message}")
            false
        }
    }

    fun hasNativeSources(projectDir: File): Boolean {
        return try {
            projectDir.walkTopDown().any {
                it.isFile && (it.extension == "cpp" || it.extension == "cc" || it.extension == "c")
            }
        } catch (t: Throwable) {
            false
        }
    }

    fun hasBlueprintFiles(projectDir: File): Boolean {
        return try {
            projectDir.walkTopDown().any {
                it.isFile && (it.name == "Android.bp" || it.name == "Android.mk" || it.name == "CMakeLists.txt")
            }
        } catch (t: Throwable) {
            false
        }
    }
}
