package com.vibe.choreide.system

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Root-level system deployer built on libsu.
 *
 * Safety contract (anti-bootloop):
 *  1. Verify root first - refuse silently when not granted.
 *  2. Always back up the existing target into /sdcard/ChoreBackup/<ts>/
 *     together with an executable restore.sh rescue script.
 *  3. Only then remount /system rw, push, chmod 644, and restart the target.
 */
object RootDeployer {

    const val BACKUP_ROOT = "/sdcard/ChoreBackup"

    data class DeployResult(val success: Boolean, val log: String)

    fun isRootAvailable(): Boolean = try {
        Shell.isAppGrantedRoot() == true
    } catch (t: Throwable) {
        false
    }

    suspend fun deployApk(
        sourceApk: File,
        targetSystemPath: String,
        targetPackage: String,
        softReboot: Boolean = false,
        onLog: (String) -> Unit
    ): DeployResult = withContext(Dispatchers.IO) {
        val log = StringBuilder()
        fun emit(line: String) {
            log.append(line).append("\n")
            onLog(line)
        }

        try {
            if (!isRootAvailable()) {
                emit("[error] root access not granted")
                return@withContext DeployResult(false, log.toString())
            }
            if (!sourceApk.exists()) {
                emit("[error] source not found: ${sourceApk.absolutePath}")
                return@withContext DeployResult(false, log.toString())
            }

            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val backupDir = "$BACKUP_ROOT/$timestamp"
            val targetFile = File(targetSystemPath)
            val targetName = targetFile.name

            emit("[1/6] backup -> $backupDir")
            Shell.cmd("mkdir -p $backupDir").exec()
            if (targetFile.exists()) {
                Shell.cmd("cp $targetSystemPath $backupDir/$targetName").exec()
                emit("      saved $targetName")
            } else {
                emit("      fresh install (no existing target)")
            }

            emit("[2/6] writing rescue script")
            Shell.cmd(
                "printf '%s\n' '#!/system/bin/sh' " +
                        "'mount -o rw,remount /system' " +
                        "'cp $backupDir/$targetName $targetSystemPath' " +
                        "'chmod 644 $targetSystemPath' " +
                        "'killall $targetPackage' " +
                        "'echo restored' > $backupDir/restore.sh"
            ).exec()
            Shell.cmd("chmod 755 $backupDir/restore.sh").exec()

            emit("[3/6] remount /system rw")
            val mount = Shell.cmd("mount -o rw,remount /system").exec()
            if (!mount.isSuccess) {
                emit("[error] remount failed: ${mount.err.joinToString()}")
                return@withContext DeployResult(false, log.toString())
            }

            emit("[4/6] push ${sourceApk.name}")
            val push = Shell.cmd(
                "cp ${sourceApk.absolutePath} $targetSystemPath && chmod 644 $targetSystemPath"
            ).exec()
            if (!push.isSuccess) {
                emit("[error] push failed: ${push.err.joinToString()}")
                return@withContext DeployResult(false, log.toString())
            }

            emit("[5/6] restart $targetPackage")
            Shell.cmd("killall $targetPackage").exec()

            if (softReboot) {
                emit("[6/6] soft reboot (zygote restart)")
                Shell.cmd("setprop ctl.restart zygote").exec()
            } else {
                emit("[6/6] soft reboot skipped")
            }

            emit("[done] deployed. rescue: $backupDir/restore.sh")
            DeployResult(true, log.toString())
        } catch (t: Throwable) {
            emit("[error] ${t.message}")
            DeployResult(false, log.toString())
        }
    }

    /** Trigger a soft reboot (restart zygote) without deploying anything. */
    suspend fun softReboot(onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isRootAvailable()) {
                onLog("[error] root not granted")
                return@withContext false
            }
            onLog("[root] restarting zygote (soft reboot)")
            Shell.cmd("setprop ctl.restart zygote").exec()
            true
        } catch (t: Throwable) {
            onLog("[error] ${t.message}")
            false
        }
    }
}
