plugins {
    // AGP 8.11+ is required to compile against compileSdk 36 (used by the
    // settings UI's Compose dependencies). AGP 8.11 needs Gradle 8.13+
    // (see gradle/wrapper/gradle-wrapper.properties).
    id("com.android.application") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}
