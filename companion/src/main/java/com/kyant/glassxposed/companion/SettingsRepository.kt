package com.kyant.glassxposed.companion

import android.content.Context
import android.content.SharedPreferences

/**
 * Writes to the SAME prefs file name/keys the module reads from
 * (module/.../prefs/GlassPrefs.kt). MODE_WORLD_READABLE is normally
 * removed on modern Android, but LSPosed specifically re-enables it for
 * modules that declare xposedsharedprefs="true" — see:
 * https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences
 *
 * If this app is launched WITHOUT the module active/enabled in LSPosed for
 * this app's own scope, MODE_WORLD_READABLE will just throw or fall back to
 * MODE_PRIVATE-like behavior on stock Android — this is caught below and we
 * fall back to a normal private-mode file so the settings UI still works
 * (LSPosed's hook is really only needed on the SystemUI side to *read* it).
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
        val enabledStatusBar: Boolean = true,
        val enabledShade: Boolean = true,
        val enabledNavBar: Boolean = false,
        val blurRadius: Float = 24f,
        val refractionHeight: Float = 24f,
        val refractionAmount: Float = 16f,
        val depthEffect: Float = 0f,
        val chromaticAberration: Float = 0f,
        val useDispersion: Boolean = false
    )

    fun load(context: Context): Settings {
        val p = prefs(context)
        return Settings(
            enabledStatusBar = p.getBoolean("enabled_status_bar", true),
            enabledShade = p.getBoolean("enabled_shade", true),
            enabledNavBar = p.getBoolean("enabled_nav_bar", false),
            blurRadius = p.getFloat("blur_radius", 24f),
            refractionHeight = p.getFloat("refraction_height", 24f),
            refractionAmount = p.getFloat("refraction_amount", 16f),
            depthEffect = p.getFloat("depth_effect", 0f),
            chromaticAberration = p.getFloat("chromatic_aberration", 0f),
            useDispersion = p.getBoolean("use_dispersion", false)
        )
    }

    fun save(context: Context, settings: Settings) {
        prefs(context).edit()
            .putBoolean("enabled_status_bar", settings.enabledStatusBar)
            .putBoolean("enabled_shade", settings.enabledShade)
            .putBoolean("enabled_nav_bar", settings.enabledNavBar)
            .putFloat("blur_radius", settings.blurRadius)
            .putFloat("refraction_height", settings.refractionHeight)
            .putFloat("refraction_amount", settings.refractionAmount)
            .putFloat("depth_effect", settings.depthEffect)
            .putFloat("chromatic_aberration", settings.chromaticAberration)
            .putBoolean("use_dispersion", settings.useDispersion)
            .apply()
    }
}
