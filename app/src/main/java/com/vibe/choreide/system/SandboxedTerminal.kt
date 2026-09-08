package com.vibe.choreide.system

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fully sandboxed internal terminal.
 *
 * - Working directory is always inside filesDir/projects/[current project].
 * - PATH is injected with filesDir/bin first, so aapt2/zipalign resolve
 *   without Termux; dalvikvm is available from the Android runtime itself.
 * - stdout/stderr stream line-by-line into an observable StateFlow.
 */
class SandboxedTerminal(private val context: Context) {

    private fun projectsDir(): File =
        File(context.filesDir, "projects").apply { mkdirs() }

    data class TerminalLine(val text: String, val isError: Boolean = false)

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    var workingDir: File = projectsDir()
        private set

    fun setProject(projectName: String?) {
        workingDir = if (projectName.isNullOrBlank()) {
            projectsDir()
        } else {
            File(projectsDir(), projectName)
        }
        workingDir.mkdirs()
    }

    fun execute(command: String, scope: CoroutineScope) {
        val cmd = command.trim()
        if (cmd.isEmpty() || _running.value) return

        append(TerminalLine("$ $cmd"))

        // built-in commands
        when {
            cmd == "clear" -> {
                _lines.value = emptyList()
                return
            }
            cmd == "pwd" -> {
                append(TerminalLine(workingDir.absolutePath))
                return
            }
            cmd.startsWith("cd ") -> {
                changeDir(cmd.removePrefix("cd ").trim())
                return
            }
        }

        _running.value = true
        scope.launch {
            val exitCode = runProcess(cmd)
            append(TerminalLine("[exit $exitCode]"))
            _running.value = false
        }
    }

    private fun changeDir(target: String) {
        val resolved = if (target.startsWith("/")) File(target)
        else File(workingDir, target)
        val sandboxRoot = context.filesDir.canonicalFile
        try {
            val canonical = resolved.canonicalFile
            if (canonical.isDirectory && canonical.path.startsWith(sandboxRoot.path)) {
                workingDir = canonical
                append(TerminalLine(canonical.path))
            } else {
                append(TerminalLine("cd: outside sandbox or not a directory", isError = true))
            }
        } catch (t: Throwable) {
            append(TerminalLine("cd: ${t.message}", isError = true))
        }
    }

    private suspend fun runProcess(command: String): Int = withContext(Dispatchers.IO) {
        try {
            val pb = ProcessBuilder("sh", "-c", command)
            pb.directory(workingDir)

            // Environment: sandbox bin first on PATH, plus HOME inside sandbox
            val env = pb.environment()
            val basePath = env["PATH"].orEmpty()
            env["PATH"] = SandboxDownloader.binDir(context).absolutePath +
                    File.pathSeparator + basePath
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = File(context.filesDir, "tmp").apply { mkdirs() }.absolutePath
            env["LD_LIBRARY_PATH"] = SandboxDownloader.binDir(context).absolutePath

            pb.redirectErrorStream(false)
            val process = pb.start()

            val stdoutJob = launch {
                process.inputStream.bufferedReader().forEachLine {
                    append(TerminalLine(it))
                }
            }
            val stderrJob = launch {
                process.errorStream.bufferedReader().forEachLine {
                    append(TerminalLine(it, isError = true))
                }
            }

            process.waitFor()
            stdoutJob.join()
            stderrJob.join()
            process.exitValue()
        } catch (t: Throwable) {
            append(TerminalLine("error: ${t.message}", isError = true))
            -1
        }
    }

    private fun append(line: TerminalLine) {
        _lines.value = (_lines.value + line).takeLast(500)
    }
}
