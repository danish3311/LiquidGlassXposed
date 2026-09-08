package com.kyant.glassxposed.companion

import android.content.Context
import android.content.SharedPreferences

/**
 * Writes to the SAME prefs file name/keys the module reads from
 * (module/.../prefs/GlassPrefs.kt). MODE_WORLD_READABLE is normally
 * removed on modern Android, but LSPosed re-enables it for modules that
 * declare xposedsharedprefs="true" — see:
 * https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences
 */
object SettingsRepository {

    private const val PREFS_NAME = "glass_settings"

    private fun prefs(context: Context): SharedPreferences {
        return try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    data class Settings(
        // SystemUI targets
        val enabledStatusBar: Boolean = true,
        val enabledShade: Boolean = true,
        val enabledNavBar: Boolean = false,
        val enabledQsPanel: Boolean = true,
        val enabledLockscreen: Boolean = false,
        val enabledVolumeDialog: Boolean = false,
        // Refraction (AndroidLiquidGlass)
        val blurRadius: Float = 24f,
        val refractionHeight: Float = 24f,
        val refractionAmount: Float = 16f,
        val depthEffect: Float = 0f,
        val chromaticAberration: Float = 0f,
        val useDispersion: Boolean = false,
        // Grain + tint (Haze)
        val useNoiseTint: Boolean = false,
        val noiseAlpha: Float = 0.05f,
        val tintAlpha: Float = 40f,
        // Coarse whole-app blur, opt-in per package
        val otherAppsPackages: String = "",
        val otherAppsBlurRadius: Float = 20f
    )

    fun load(context: Context): Settings {
        val p = prefs(context)
        return Settings(
            enabledStatusBar = p.getBoolean("enabled_status_bar", true),
            enabledShade = p.getBoolean("enabled_shade", true),
            enabledNavBar = p.getBoolean("enabled_nav_bar", false),
            enabledQsPanel = p.getBoolean("enabled_qs_panel", true),
            enabledLockscreen = p.getBoolean("enabled_lockscreen", false),
            enabledVolumeDialog = p.getBoolean("enabled_volume_dialog", false),
            blurRadius = p.getFloat("blur_radius", 24f),
            refractionHeight = p.getFloat("refraction_height", 24f),
            refractionAmount = p.getFloat("refraction_amount", 16f),
            depthEffect = p.getFloat("depth_effect", 0f),
            chromaticAberration = p.getFloat("chromatic_aberration", 0f),
            useDispersion = p.getBoolean("use_dispersion", false),
            useNoiseTint = p.getBoolean("use_noise_tint", false),
            noiseAlpha = p.getFloat("noise_alpha", 0.05f),
            tintAlpha = p.getFloat("tint_alpha", 40f),
            otherAppsPackages = p.getString("other_apps_packages", "") ?: "",
            otherAppsBlurRadius = p.getFloat("other_apps_blur_radius", 20f)
        )
    }

    fun save(context: Context, settings: Settings) {
        prefs(context).edit()
            .putBoolean("enabled_status_bar", settings.enabledStatusBar)
            .putBoolean("enabled_shade", settings.enabledShade)
            .putBoolean("enabled_nav_bar", settings.enabledNavBar)
            .putBoolean("enabled_qs_panel", settings.enabledQsPanel)
            .putBoolean("enabled_lockscreen", settings.enabledLockscreen)
            .putBoolean("enabled_volume_dialog", settings.enabledVolumeDialog)
            .putFloat("blur_radius", settings.blurRadius)
            .putFloat("refraction_height", settings.refractionHeight)
            .putFloat("refraction_amount", settings.refractionAmount)
            .putFloat("depth_effect", settings.depthEffect)
            .putFloat("chromatic_aberration", settings.chromaticAberration)
            .putBoolean("use_dispersion", settings.useDispersion)
            .putBoolean("use_noise_tint", settings.useNoiseTint)
            .putFloat("noise_alpha", settings.noiseAlpha)
            .putFloat("tint_alpha", settings.tintAlpha)
            .putString("other_apps_packages", settings.otherAppsPackages)
            .putFloat("other_apps_blur_radius", settings.otherAppsBlurRadius)
            .apply()
    }
}
