package com.vibe.choreide.workspace

/** Project type offered by the wizard. */
enum class ProjectKind(val label: String) {
    STANDARD_APP("Standard App"),
    SYSTEMUI_MOD("SystemUI Mod"),
    RRO_OVERLAY("RRO Overlay")
}

/** Source language for generated code. */
enum class SourceLanguage(val label: String, val extension: String) {
    KOTLIN("Kotlin", "kt"),
    JAVA("Java", "java")
}

/** Gradle build script DSL. */
enum class BuildDsl(val label: String, val fileName: String) {
    KOTLIN_DSL("Kotlin DSL", "build.gradle.kts"),
    GROOVY("Groovy", "build.gradle")
}

data class WizardOptions(
    val projectName: String,
    val packageName: String,
    val kind: ProjectKind = ProjectKind.STANDARD_APP,
    val language: SourceLanguage = SourceLanguage.KOTLIN,
    val dsl: BuildDsl = BuildDsl.KOTLIN_DSL,
    val minSdk: Int = 31,
    val targetSdk: Int = 34
)
