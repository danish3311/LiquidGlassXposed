plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kyant.glassxposed"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kyant.glassxposed"
        // minSdk 33 (Tiramisu) because android.graphics.RuntimeShader / AGSL
        // requires API 33, and we hook real RenderEffect on real Views.
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Classic Xposed API — compileOnly because LSPosed provides the real
    // implementation at runtime in the hooked process.
    compileOnly("de.robv.android.xposed:api:82")

    // @ColorInt / @RequiresApi used in shader/GlassEffectFactory.kt
    implementation("androidx.annotation:annotation:1.9.1")
}
