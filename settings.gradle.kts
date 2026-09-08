pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Classic Xposed API (IXposedHookLoadPackage etc.) — LSPosed supports this API
        maven("https://api.xposed.info/")
    }
}

rootProject.name = "LiquidGlassXposed"
include(":app")
