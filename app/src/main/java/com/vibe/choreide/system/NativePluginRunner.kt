package com.vibe.choreide.system

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Executes on-device toolchain binaries and java-based RE plugins,
 * streaming stdout/stderr in real time into the IDE terminal.
 */
object NativePluginRunner {

    data class RunResult(
        val exitCode: Int,
        val success: Boolean
    )

    /** Run a native binary (aapt2, d8, clang, ...) extracted to filesDir/bin. */
    suspend fun runBinary(
        context: Context,
        binaryName: String,
        args: List<String>,
        workDir: File? = null,
        env: Map<String, String> = emptyMap(),
        onLog: (String) -> Unit
    ): RunResult = withContext(Dispatchers.IO) {
        val binary = AssetManagerHelper.tool(context, binaryName)
        if (!binary.exists()) {
            onLog("[runner] missing binary: ${binary.absolutePath}")
            return@withContext RunResult(-1, false)
        }
        binary.setExecutable(true, false)
        exec(listOf(binary.absolutePath) + args, workDir, env, onLog)
    }

    /**
     * Run a .jar plugin (apktool.jar, jadx.jar, ecj.jar, apksigner.jar)
     * through Android's built-in dalvikvm so no host JVM is required.
     */
    suspend fun runJar(
        context: Context,
        jarFile: File,
        args: List<String>,
        workDir: File? = null,
        onLog: (String) -> Unit
    ): RunResult = withContext(Dispatchers.IO) {
        if (!jarFile.exists()) {
            onLog("[runner] missing jar: ${jarFile.absolutePath}")
            return@withContext RunResult(-1, false)
        }
        val cmd = listOf(
            "dalvikvm",
            "-Xmx512m",
            "-cp", jarFile.absolutePath,
            mainClassFor(jarFile.name)
        ) + args
        exec(cmd, workDir, emptyMap(), onLog)
    }

    /** Well-known main classes for bundled plugins. */
    private fun mainClassFor(jarName: String): String = when {
        jarName.startsWith("apktool") -> "brut.apktool.Main"
        jarName.startsWith("jadx") -> "jadx.cli.JadxCLI"
        jarName.startsWith("ecj") -> "org.eclipse.jdt.internal.compiler.batch.Main"
        jarName.startsWith("apksigner") -> "com.android.apksigner.ApkSignerTool"
        jarName.startsWith("d8") -> "com.android.tools.r8.D8"
        else -> "Main"
    }

    private fun exec(
        cmd: List<String>,
        workDir: File?,
        env: Map<String, String>,
        onLog: (String) -> Unit
    ): RunResult {
        return try {
            onLog("$ " + cmd.joinToString(" "))
            val pb = ProcessBuilder(cmd)
            workDir?.let { pb.directory(it) }
            pb.environment().putAll(env)
            pb.redirectErrorStream(false)
            val process = pb.start()

            // Stream stdout and stderr concurrently to avoid pipe deadlock
            val stdoutThread = Thread {
                process.inputStream.bufferedReader().forEachLine { onLog(it) }
            }
            val stderrThread = Thread {
                process.errorStream.bufferedReader().forEachLine { onLog("[err] $it") }
            }
            stdoutThread.start()
            stderrThread.start()
            process.waitFor()
            stdoutThread.join(2000)
            stderrThread.join(2000)

            val code = process.exitValue()
            onLog("[exit $code]")
            RunResult(code, code == 0)
        } catch (t: Throwable) {
            onLog("[runner] ${t.message}")
            RunResult(-1, false)
        }
    }

    // ---- Convenience wrappers -------------------------------------------------

    suspend fun aapt2(
        context: Context, args: List<String>, workDir: File? = null, onLog: (String) -> Unit
    ) = runBinary(context, "aapt2", args, workDir, onLog = onLog)

    suspend fun d8(
        context: Context, args: List<String>, workDir: File? = null, onLog: (String) -> Unit
    ) = runJar(context, AssetManagerHelper.tool(context, "d8.jar"), args, workDir, onLog)

    suspend fun apktool(
        context: Context, args: List<String>, workDir: File? = null, onLog: (String) -> Unit
    ) = runJar(context, AssetManagerHelper.plugin(context, "apktool.jar"), args, workDir, onLog)

    suspend fun jadx(
        context: Context, args: List<String>, workDir: File? = null, onLog: (String) -> Unit
    ) = runJar(context, AssetManagerHelper.plugin(context, "jadx.jar"), args, workDir, onLog)
}
