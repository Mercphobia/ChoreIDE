package com.vibe.choreide.compiler

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BuildPipelineManager {

    data class BuildResult(
        val success: Boolean,
        val apkFile: File?,
        val log: String
    )

    private fun binDir(context: Context): File = File(context.filesDir, "bin")

    suspend fun ensureToolchain(context: Context, onLog: (String) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val dir = binDir(context)
                if (!dir.exists()) dir.mkdirs()

                val tools = listOf("aapt2", "d8.jar", "ecj.jar", "apksigner.jar", "debug.keystore")
                for (tool in tools) {
                    val target = File(dir, tool)
                    if (!target.exists()) {
                        try {
                            context.assets.open("bin/$tool").use { input ->
                                target.outputStream().use { output -> input.copyTo(output) }
                            }
                            target.setExecutable(true)
                            onLog("extracted $tool")
                        } catch (t: Throwable) {
                            onLog("missing asset: bin/$tool (toolchain incomplete)")
                        }
                    }
                }
                true
            } catch (t: Throwable) {
                onLog("toolchain setup failed: ${t.message}")
                false
            }
        }

    suspend fun buildApk(
        context: Context,
        projectDir: File,
        onLog: (String) -> Unit
    ): BuildResult = withContext(Dispatchers.IO) {
        val log = StringBuilder()
        fun emit(line: String) {
            log.append(line).append("\n")
            onLog(line)
        }

        try {
            emit("[build] project: ${projectDir.name}")

            val resDir = File(projectDir, "res")
            val srcDir = File(projectDir, "src")
            val manifest = File(projectDir, "AndroidManifest.xml")

            if (!manifest.exists()) {
                emit("[skip] no AndroidManifest.xml - not a buildable app project")
                return@withContext BuildResult(false, null, log.toString())
            }

            val outDir = File(projectDir, "build")
            outDir.mkdirs()

            val bin = binDir(context)
            val aapt2 = File(bin, "aapt2")

            if (!aapt2.exists()) {
                emit("[error] aapt2 not available. Toolchain assets not bundled yet.")
                emit("[info] bundle pre-built aapt2/d8/ecj/apksigner under assets/bin/ to enable local builds")
                return@withContext BuildResult(false, null, log.toString())
            }

            // Step 1: aapt2 compile
            emit("[1/4] aapt2 compile")
            val compileOut = File(outDir, "compiled-res")
            compileOut.mkdirs()
            val resFiles = resDir.walkTopDown().filter { it.isFile }.toList()
            if (resFiles.isNotEmpty()) {
                val compileCmd = mutableListOf(aapt2.absolutePath, "compile", "--dir",
                    resDir.absolutePath, "-o", File(compileOut, "res.zip").absolutePath)
                val p1 = ProcessBuilder(compileCmd).redirectErrorStream(true).start()
                p1.inputStream.bufferedReader().forEachLine { emit(it) }
                p1.waitFor()
            }

            // Step 2: aapt2 link
            emit("[2/4] aapt2 link")
            val baseApk = File(outDir, "base.apk")
            val linkCmd = listOf(aapt2.absolutePath, "link",
                "-o", baseApk.absolutePath,
                "--manifest", manifest.absolutePath,
                File(compileOut, "res.zip").absolutePath)
            val p2 = ProcessBuilder(linkCmd).redirectErrorStream(true).start()
            p2.inputStream.bufferedReader().forEachLine { emit(it) }
            p2.waitFor()

            // Step 3+4 placeholder until ecj/d8 assets are bundled
            emit("[3/4] ecj compile (skipped: toolchain assets pending)")
            emit("[4/4] d8 + sign (skipped: toolchain assets pending)")

            if (baseApk.exists()) {
                emit("[done] base apk: ${baseApk.absolutePath}")
                BuildResult(true, baseApk, log.toString())
            } else {
                emit("[error] build produced no apk")
                BuildResult(false, null, log.toString())
            }
        } catch (t: Throwable) {
            emit("[error] ${t.message}")
            BuildResult(false, null, log.toString())
        }
    }
}
