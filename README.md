# ChoreIDE

> Hybrid mobile IDE for Android app development and AOSP system-level UI prototyping.

ChoreIDE (named after `android.view.Choreographer`) is an on-device IDE that supports both standard Android app projects and root-required AOSP UI prototyping with live XML preview and safe `/system` deployment.

## Features

- **Dark Glassmorphism UI** — JetBrains Darcula-inspired palette with glass panels, built on Jetpack Compose Material 3.
- **Project Wizard** — scaffold AOSP mockup templates: SystemUI, Settings, Launcher, Empty.
- **Workspace** — recursive file tree under `/sdcard/ChoreProjects/`, storage permission handling, MVVM + StateFlow.
- **Code Editor** — Sora-Editor with Darcula syntax theme and save support.
- **AOSP Mockup Engine** — raw XML string preview via custom `LayoutInflater.Factory2`; unknown system views (e.g. `PhoneStatusBarView`) are intercepted and replaced with labeled mock views instead of crashing.
- **XML Resource Interceptor** — regex pre-processor that neutralizes private `@*android:` color/dimen/drawable/style pointers before inflation.
- **Hidden API Bypass** — LSPosed `HiddenApiBypass` initialized at startup.
- **Root Deployment** — libsu-based deployer with anti-bootloop protection: automatic backup to `/sdcard/ChoreBackup/`, generated `restore.sh` rescue script, remount, push, chmod 644, and process restart.
- **Terminal** — root-aware shell with streaming output.
- **Build Pipeline** — local toolchain manager (aapt2 / ecj / d8 / apksigner) extracted to `filesDir/bin` with chmod 755; streams compiler output into the Build screen.
- **Native NDK Manager** — embedded clang/clang++/lld/cmake wrapper for `.so` compilation targeting `aarch64-none-linux-android`.

## Architecture

```
com.vibe.choreide/
├── MainActivity.kt          # Navigation, libsu config, HiddenApiBypass init
├── ui/
│   ├── theme/               # Darcula colors, Material 3 theme, glassmorphism modifier
│   ├── components/          # GlassPanel, BottomNavBar, SymbolBar, FileTreeItem
│   └── screens/             # Editor, Project, Terminal, Mockup, Wizard, Build
├── editor/                  # Sora-Editor Compose wrapper, syntax theme
├── workspace/               # FileRepository, ProjectGenerator, ProjectManager (StateFlow)
├── system/                  # RootDeployer, AospMockupInflater, XmlResourceInterceptor, NativeBuildManager
└── compiler/                # BuildPipelineManager (aapt2/ecj/d8/apksigner)
```

## Safety

- All I/O, shell, and process work runs on `Dispatchers.IO` wrapped in try-catch.
- Root deployment always creates a timestamped backup and rescue script before touching `/system`.
- AOSP preview never crashes on unknown views or private resources.

## Build

CI builds the debug APK on every push (GitHub Actions, artifact: `choreide-debug-apk`).

```bash
./gradlew :app:assembleDebug
```

## Requirements

- Android 8.0+ (API 28)
- Root (Magisk) only for `/system` deployment features
- All-files storage permission for workspace access
