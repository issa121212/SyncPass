// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
// Run/Debug starts local services through a dedicated task, never during Sync/tests/builds.
val localRunScript = layout.projectDirectory.asFile.parentFile.resolve("PREPARAR_RUN.ps1").absolutePath
val localRunDirectory = layout.projectDirectory.asFile.parentFile.absolutePath
val localRunJavaHome = System.getProperty("java.home")
val localSdkProperties = java.util.Properties().apply {
    providers.fileContents(layout.projectDirectory.file("local.properties")).asText.orNull
        ?.let { load(it.reader()) }
}
val localRunSdk = localSdkProperties.getProperty("sdk.dir")
    ?: providers.environmentVariable("ANDROID_HOME").orNull
    ?: providers.environmentVariable("ANDROID_SDK_ROOT").orNull.orEmpty()

tasks.register<Exec>("prepareSyncPassLocal") {
    group = "application"
    description = "Starts/reuses the local backend and prepares adb before Android Studio Run."
    workingDir(localRunDirectory)
    commandLine("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", localRunScript, "-JavaHome", localRunJavaHome)
    if (localRunSdk.isNotBlank()) args("-SdkPath", localRunSdk)
    timeout.set(java.time.Duration.ofMinutes(8))
}
