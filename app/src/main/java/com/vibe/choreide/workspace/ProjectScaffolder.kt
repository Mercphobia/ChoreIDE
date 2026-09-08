package com.vibe.choreide.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Generates the on-disk structure for a new project created by the wizard.
 * Pure file generation - no Android context required, fully testable.
 */
object ProjectScaffolder {

    const val PROJECTS_ROOT = "/sdcard/ChoreProjects"

    suspend fun scaffold(options: WizardOptions): Result<File> = withContext(Dispatchers.IO) {
        try {
            val safeName = options.projectName.trim()
                .replace(Regex("[^A-Za-z0-9_-]"), "_")
            require(safeName.isNotEmpty()) { "Project name must not be empty" }
            require(options.packageName.contains('.')) { "Package name must contain a dot" }

            val root = File(PROJECTS_ROOT, safeName)
            val packageDir = options.packageName.replace('.', '/')
            val srcDir = File(root, "app/src/main/java/$packageDir")
            val resDir = File(root, "app/src/main/res")

            srcDir.mkdirs()
            File(resDir, "layout").mkdirs()
            File(resDir, "values").mkdirs()

            // AndroidManifest.xml
            File(root, "app/src/main/AndroidManifest.xml")
                .writeText(manifest(options))

            // MainActivity in the chosen language
            val activityName = "MainActivity.${options.language.extension}"
            File(srcDir, activityName).writeText(mainActivity(options))

            // Layout + strings
            File(resDir, "layout/activity_main.xml").writeText(mainLayout(options))
            File(resDir, "values/strings.xml").writeText(stringsXml(options))

            // Gradle scripts in the chosen DSL
            File(root, "app/${options.dsl.fileName}").writeText(appGradle(options))
            File(root, "settings.gradle.kts").writeText(settingsGradle(safeName))

            Result.success(root)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun manifest(o: WizardOptions): String {
        val theme = when (o.kind) {
            ProjectKind.RRO_OVERLAY -> "@android:style/Theme.Translucent.NoTitleBar"
            else -> "@android:style/Theme.Material.NoActionBar"
        }
        return """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="$theme">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""
    }

    private fun mainActivity(o: WizardOptions): String = when (o.language) {
        SourceLanguage.KOTLIN -> """package ${o.packageName}

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
"""
        SourceLanguage.JAVA -> """package ${o.packageName};

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
"""
    }

    private fun mainLayout(o: WizardOptions): String = """<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="${o.projectName}"
        android:textSize="20sp" />
</FrameLayout>
"""

    private fun stringsXml(o: WizardOptions): String = """<resources>
    <string name="app_name">${o.projectName
    /** app-level build script with local libs/ classpath mapping. */
    private fun appGradle(o: WizardOptions): String = when (o.dsl) {
        BuildDsl.KOTLIN_DSL -> """plugins {
    id("com.android.application")
${if (o.language == SourceLanguage.KOTLIN) "    id(\"org.jetbrains.kotlin.android\")" else ""}
}

android {
    namespace = \"${o.packageName}\"
    compileSdk = ${o.targetSdk}

    defaultConfig {
        applicationId = \"${o.packageName}\"
        minSdk = ${o.minSdk}
        targetSdk = ${o.targetSdk}
        versionCode = 1
        versionName = \"1.0\"
    }
}

// Offline-first: resolve every dependency from the local libs/ folder
// injected by ChoreIDE (AndroidX + Material 3 aar/jar, android.jar stubs).
dependencies {
    implementation(fileTree(mapOf(\"dir\" to \"libs\", \"include\" to listOf(\"*.jar\", \"*.aar\"))))
}
"""
        BuildDsl.GROOVY -> """plugins {
    id 'com.android.application'
${if (o.language == SourceLanguage.KOTLIN) "    id 'org.jetbrains.kotlin.android'" else ""}
}

android {
    namespace '${o.packageName}'
    compileSdk ${o.targetSdk}

    defaultConfig {
        applicationId '${o.packageName}'
        minSdk ${o.minSdk}
        targetSdk ${o.targetSdk}
        versionCode 1
        versionName '1.0'
    }
}

// Offline-first: resolve every dependency from the local libs/ folder
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
}
"""
    }

    private fun settingsGradle(projectName: String): String = """pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = \"$projectName\"
include(\":app\")
"""
}

}