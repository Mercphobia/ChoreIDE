package com.vibe.choreide.system

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real-time logcat reader with Force-Close / error filtering,
 * used to diagnose SystemUI deployments directly from the IDE.
 */
class LogcatManager {

    data class LogLine(val text: String, val isError: Boolean = false)

    private val _lines = MutableStateFlow<List<LogLine>>(emptyList())
    val lines: StateFlow<List<LogLine>> = _lines.asStateFlow()

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private var job: Job? = null
    private var process: Process? = null

    /**
     * @param filterTag  optional tag filter (e.g. "SystemUI"), empty = all
     * @param errorsOnly when true, keeps only E/F level lines and FC patterns
     */
    fun start(
        scope: CoroutineScope,
        filterTag: String = "",
        errorsOnly: Boolean = false
    ) {
        stop()
        _active.value = true

        job = scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val cmd = mutableListOf("logcat", "-v", "threadtime")
                    if (errorsOnly) cmd.add("*:E")
                    if (filterTag.isNotBlank()) {
                        cmd.add("-s")
                        cmd.add(filterTag)
                    }
                    process = ProcessBuilder(cmd)
                        .redirectErrorStream(true)
                        .start()

                    process?.inputStream?.bufferedReader()?.forEachLine { line ->
                        val isFc = line.contains("FATAL EXCEPTION") ||
                                line.contains("AndroidRuntime") ||
                                line.contains("Force finishing activity")
                        val isErr = line.contains(" E ") || line.contains(" F ") || isFc
                        if (!errorsOnly || isErr) {
                            append(LogLine(line, isError = isErr))
                        }
                    }
                } catch (t: Throwable) {
                    append(LogLine("logcat failed: ${t.message}", isError = true))
                } finally {
                    _active.value = false
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        try {
            process?.destroy()
        } catch (t: Throwable) {
            // ignore
        }
        process = null
        _active.value = false
    }

    fun clear() {
        _lines.value = emptyList()
    }

    private fun append(line: LogLine) {
        _lines.value = (_lines.value + line).takeLast(800)
    }
}
