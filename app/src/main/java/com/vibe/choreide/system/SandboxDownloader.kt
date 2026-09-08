package com.vibe.choreide.system

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sandbox toolchain downloader.
 *
 * Downloads every build/RE tool directly into the app's private sandbox
 * (context.filesDir) on first launch. No Termux, no public storage.
 *
 * Layout produced:
 *   filesDir/bin/      aapt2 (ARM64), d8.jar, ecj.jar, apksigner.jar, zipalign
 *   filesDir/plugins/  apktool.jar, jadx-all.jar
 *   filesDir/libs/     android.jar (optional stub, not downloaded here)
 *
 * Sources:
 *  - Apktool : github.com/iBotPeaches/Apktool releases (latest)
 *  - Jadx    : github.com/skylot/jadx releases (latest, jadx-all)
 *  - ECJ     : Maven Central (org.eclipse.jdt:ecj)
 *  - d8      : Google Maven com.android.tools:r8 (latest, d8 is bundled inside)
 *  - aapt2   : Google Maven com.android.tools.build:aapt2 (linux-aarch64 jar,
 *              the native binary is extracted from inside that jar)
 */
object SandboxDownloader {

    // ------------------------------------------------------------------
    // Tool specifications
    // ------------------------------------------------------------------

    private const val APKTOOL_URL =
        "https://github.com/ibotpeaches/Apktool/releases/latest/download/apktool.jar"
    private const val JADX_URL =
        "https://github.com/skylot/jadx/releases/latest/download/jadx-all.jar"
    private const val ECJ_URL =
        "https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/3.33.0/ecj-3.33.0.jar"

    private const val R8_METADATA_URL =
        "https://dl.google.com/dl/android/maven2/com/android/tools/r8/maven-metadata.xml"
    private const val R8_BASE_URL =
        "https://dl.google.com/dl/android/maven2/com/android/tools/r8/"

    private const val AAPT2_METADATA_URL =
        "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/maven-metadata.xml"
    private const val AAPT2_BASE_URL =
        "https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/"

    data class DownloadReport(
        val downloaded: Int,
        val skipped: Int,
        val failed: List<String>
    )

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    fun isReady(context: Context): Boolean {
        val core = listOf(
            File(binDir(context), "ecj.jar"),
            File(binDir(context), "d8.jar"),
            File(binDir(context), "aapt2"),
            File(pluginsDir(context), "apktool.jar"),
            File(pluginsDir(context), "jadx-all.jar")
        )
        return core.all { it.exists() && it.length() > 0 }
    }

    /**
     * Downloads every missing tool. Emits (progress 0..1, status text) so the
     * startup wizard can drive a progress bar.
     */
    suspend fun downloadAll(
        context: Context,
        onProgress: (Float, String) -> Unit
    ): DownloadReport = withContext(Dispatchers.IO) {
        binDir(context).mkdirs()
        pluginsDir(context).mkdirs()

        var downloaded = 0
        var skipped = 0
        val failed = mutableListOf<String>()

        // Each step gets an equal share of the progress bar
        val steps: List<Pair<String, suspend () -> Boolean>> = listOf(
            "apktool.jar" to {
                fetch(APKTOOL_URL, File(pluginsDir(context), "apktool.jar"), onProgress)
            },
            "jadx-all.jar" to {
                fetch(JADX_URL, File(pluginsDir(context), "jadx-all.jar"), onProgress)
            },
            "ecj.jar" to {
                fetch(ECJ_URL, File(binDir(context), "ecj.jar"), onProgress)
            },
            "d8.jar (r8)" to {
                // Fallback mechanism: resolve the newest R8 version from Google
                // Maven metadata; d8 is shipped inside the r8 jar.
                val url = resolveLatestArtifact(R8_METADATA_URL, R8_BASE_URL) { v ->
                    "r8-$v.jar"
                }
                if (url != null) {
                    fetch(url, File(binDir(context), "d8.jar"), onProgress)
                } else {
                    onProgress(0f, "d8: could not resolve latest R8 version")
                    false
                }
            },
            "aapt2 (arm64)" to {
                // aapt2 ships as a jar containing the native binary; we download
                // the linux-aarch64 variant and extract the binary from it.
                downloadAapt2Arm64(context, onProgress)
            }
        )

        steps.forEachIndexed { index, (name, action) ->
            val base = index.toFloat() / steps.size
            onProgress(base, "checking $name")
            try {
                val ok = action()
                if (ok) downloaded++ else failed += name
            } catch (t: Throwable) {
                failed += "$name (${t.message})"
                onProgress(base, "$name failed: ${t.message}")
            }
            onProgress((index + 1).toFloat() / steps.size, "$name done")
        }

        onProgress(1f, "done: $downloaded ok, $skipped cached, ${failed.size} failed")
        DownloadReport(downloaded, skipped, failed)
    }

    // ------------------------------------------------------------------
    // aapt2 special handling (jar containing native binary)
    // ------------------------------------------------------------------

    private suspend fun downloadAapt2Arm64(
        context: Context,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        val target = File(binDir(context), "aapt2")
        if (target.exists() && target.length() > 0) return true

        val url = resolveLatestArtifact(AAPT2_METADATA_URL, AAPT2_BASE_URL) { v ->
            "aapt2-$v-linux-aarch64.jar"
        } ?: return false

        // Download the jar into a temp location, then pull the binary out
        val tmpJar = File(context.cacheDir, "aapt2-arm64.jar.part")
        if (!fetch(url, tmpJar, onProgress, keepPartialName = true)) return false

        return try {
            java.util.zip.ZipFile(tmpJar).use { zip ->
                // The binary inside is named "aapt2" at the jar root
                val entry = zip.entries().asSequence()
                    .firstOrNull { it.name == "aapt2" }
                    ?: return false
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            target.setExecutable(true, false)
            target.setReadable(true, false)
            tmpJar.delete()
            true
        } catch (t: Throwable) {
            target.delete()
            false
        }
    }

    // ------------------------------------------------------------------
    // Google Maven latest-version resolver
    // ------------------------------------------------------------------

    /**
     * Parses maven-metadata.xml and builds a full artifact URL for the newest
     * release (falls back to "latest" version tag when release is absent).
     */
    private fun resolveLatestArtifact(
        metadataUrl: String,
        baseUrl: String,
        artifactName: (String) -> String
    ): String? {
        return try {
            val xml = httpGet(metadataUrl) ?: return null
            val factory = XmlPullParserFactory.newInstance()
            val parser: XmlPullParser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var version: String? = null
            var text: String? = null
            var tag: String? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> tag = parser.name
                    XmlPullParser.TEXT -> text = parser.text?.trim()
                    XmlPullParser.END_TAG -> {
                        if (tag == "release" || tag == "latest") {
                            if (!text.isNullOrEmpty()) version = text
                        }
                        tag = null
                    }
                }
                event = parser.next()
            }

            version?.let { v ->
                baseUrl.trimEnd('/') + "/" + v + "/" + artifactName(v)
            }
        } catch (t: Throwable) {
            null
        }
    }

    // ------------------------------------------------------------------
    // HTTP primitives
    // ------------------------------------------------------------------

    private fun httpGet(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            body
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Streams a URL to disk atomically: writes to "<name>.part" and renames on
     * success, so interrupted downloads never leave corrupt files behind.
     */
    private fun fetch(
        url: String,
        target: File,
        onProgress: (Float, String) -> Unit,
        keepPartialName: Boolean = false
    ): Boolean {
        if (target.exists() && target.length() > 0 && !keepPartialName) return true

        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return false
            }

            val total = conn.contentLengthLong
            val tmp = if (keepPartialName) target
            else File(target.parentFile, target.name + ".part")

            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var acc = 0L
                    var lastPct = -1
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        acc += read
                        if (total > 0) {
                            val pct = (acc * 100 / total).toInt()
                            if (pct != lastPct && pct % 10 == 0) {
                                lastPct = pct
                                onProgress(pct / 100f, "${target.name}: $pct%")
                            }
                        }
                    }
                }
            }
            conn.disconnect()

            if (!keepPartialName) {
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) return false
            }
            true
        } catch (t: Throwable) {
            // Never leave a corrupt partial file in the sandbox
            if (!keepPartialName) {
                File(target.parentFile, target.name + ".part").delete()
            }
            target.delete()
            false
        }
    }

    // ------------------------------------------------------------------
    // Paths
    // ------------------------------------------------------------------

    fun binDir(context: Context): File = File(context.filesDir, "bin")
    fun pluginsDir(context: Context): File = File(context.filesDir, "plugins")
    fun libsDir(context: Context): File = File(context.filesDir, "libs")
}
