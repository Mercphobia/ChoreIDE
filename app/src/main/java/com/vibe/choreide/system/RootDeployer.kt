package com.vibe.choreide.system

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RootDeployer {

    const val BACKUP_ROOT = "/sdcard/ChoreBackup"

    data class DeployResult(
        val success: Boolean,
        val log: String
    )

    fun isRootAvailable(): Boolean = try {
        Shell.isAppGrantedRoot() == true
    } catch (t: Throwable) {
        false
    }

    suspend fun deployApk(
        sourceApk: File,
        targetSystemPath: String,
        targetPackage: String,
        onLog: (String) -> Unit
    ): DeployResult = withContext(Dispatchers.IO) {
        val log = StringBuilder()
        fun emit(line: String) {
            log.append(line).append("\n")
            onLog(line)
        }

        try {
            if (!isRootAvailable()) {
                emit("[error] Root access not granted")
                return@withContext DeployResult(false, log.toString())
            }
            if (!sourceApk.exists()) {
                emit("[error] Source APK not found: ${sourceApk.absolutePath}")
                return@withContext DeployResult(false, log.toString())
            }

            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val backupDir = "$BACKUP_ROOT/$timestamp"
            val targetFile = File(targetSystemPath)
            val targetName = targetFile.name

            // Anti-bootloop: backup existing target first
            emit("[1/5] Creating backup at $backupDir")
            Shell.cmd("mkdir -p $backupDir").exec()
            if (targetFile.exists()) {
                Shell.cmd("cp $targetSystemPath $backupDir/$targetName").exec()
                emit("      Backed up $targetName")
            } else {
                emit("      No existing target; fresh install")
            }

            // Write rescue script for bootloop recovery
            val rescueScript = File(backupDir, "restore.sh")
            emit("[2/5] Writing rescue script")
            Shell.cmd(
                "cat > ${rescueScript.absolutePath} << 'EOF'\n" +
                        "#!/system/bin/sh\n" +
                        "mount -o rw,remount /system\n" +
                        "cp $backupDir/$targetName $targetSystemPath\n" +
                        "chmod 644 $targetSystemPath\n" +
                        "killall $targetPackage\n" +
                        "echo Restored $targetName\n" +
                        "EOF"
            ).exec()
            Shell.cmd("chmod 755 ${rescueScript.absolutePath}").exec()

            emit("[3/5] Remounting /system as read-write")
            val mount = Shell.cmd("mount -o rw,remount /system").exec()
            if (!mount.isSuccess) {
                emit("[error] Failed to remount /system")
                return@withContext DeployResult(false, log.toString())
            }

            emit("[4/5] Pushing ${sourceApk.name} to $targetSystemPath")
            val push = Shell.cmd(
                "cp ${sourceApk.absolutePath} $targetSystemPath && chmod 644 $targetSystemPath"
            ).exec()
            if (!push.isSuccess) {
                emit("[error] Push failed: ${push.err.joinToString()}")
                return@withContext DeployResult(false, log.toString())
            }

            emit("[5/5] Restarting $targetPackage")
            Shell.cmd("killall $targetPackage").exec()

            emit("[done] Deployment complete. Rescue script: ${rescueScript.absolutePath}")
            DeployResult(true, log.toString())
        } catch (t: Throwable) {
            emit("[error] ${t.message}")
            DeployResult(false, log.toString())
        }
    }
}
