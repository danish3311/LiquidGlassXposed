plugins {
    // AGP 8.11+ is required to compile against compileSdk 36, which the
    // companion module's dependencies (backdrop 1.0.6, shapes-android 1.2.0)
    // require. AGP 8.11 needs Gradle 8.13+ (see gradle/wrapper/gradle-wrapper.properties).
    id("com.android.application") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}
