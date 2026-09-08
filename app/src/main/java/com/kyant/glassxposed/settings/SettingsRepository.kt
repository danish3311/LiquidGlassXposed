package com.kyant.glassxposed.settings

import android.content.Context
import android.content.SharedPreferences
import com.kyant.glassxposed.prefs.GlassPrefs

/**
 * Writes to the prefs file the Xposed hook (GlassPrefs, running inside
 * com.android.systemui) reads via XSharedPreferences. Plain MODE_PRIVATE —
 * see the big comment in GlassPrefs.kt for why MODE_WORLD_READABLE is not
 * needed (or usable) here at all.
 */
object SettingsRepository {

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(GlassPrefs.PREFS_NAME, Context.MODE_PRIVATE)

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
            enabledStatusBar = p.getBoolean(GlassPrefs.KEY_ENABLED_STATUS_BAR, true),
            enabledShade = p.getBoolean(GlassPrefs.KEY_ENABLED_SHADE, true),
            enabledNavBar = p.getBoolean(GlassPrefs.KEY_ENABLED_NAV_BAR, false),
            enabledQsPanel = p.getBoolean(GlassPrefs.KEY_ENABLED_QS_PANEL, true),
            enabledLockscreen = p.getBoolean(GlassPrefs.KEY_ENABLED_LOCKSCREEN, false),
            enabledVolumeDialog = p.getBoolean(GlassPrefs.KEY_ENABLED_VOLUME_DIALOG, false),
            blurRadius = p.getFloat(GlassPrefs.KEY_BLUR_RADIUS, 24f),
            refractionHeight = p.getFloat(GlassPrefs.KEY_REFRACTION_HEIGHT, 24f),
            refractionAmount = p.getFloat(GlassPrefs.KEY_REFRACTION_AMOUNT, 16f),
            depthEffect = p.getFloat(GlassPrefs.KEY_DEPTH_EFFECT, 0f),
            chromaticAberration = p.getFloat(GlassPrefs.KEY_CHROMATIC_ABERRATION, 0f),
            useDispersion = p.getBoolean(GlassPrefs.KEY_USE_DISPERSION, false),
            useNoiseTint = p.getBoolean(GlassPrefs.KEY_USE_NOISE_TINT, false),
            noiseAlpha = p.getFloat(GlassPrefs.KEY_NOISE_ALPHA, 0.05f),
            tintAlpha = p.getFloat(GlassPrefs.KEY_TINT_ALPHA, 40f),
            otherAppsPackages = p.getString(GlassPrefs.KEY_OTHER_APPS_PACKAGES, "") ?: "",
            otherAppsBlurRadius = p.getFloat(GlassPrefs.KEY_OTHER_APPS_BLUR_RADIUS, 20f)
        )
    }

    fun save(context: Context, settings: Settings) {
        prefs(context).edit()
            .putBoolean(GlassPrefs.KEY_ENABLED_STATUS_BAR, settings.enabledStatusBar)
            .putBoolean(GlassPrefs.KEY_ENABLED_SHADE, settings.enabledShade)
            .putBoolean(GlassPrefs.KEY_ENABLED_NAV_BAR, settings.enabledNavBar)
            .putBoolean(GlassPrefs.KEY_ENABLED_QS_PANEL, settings.enabledQsPanel)
            .putBoolean(GlassPrefs.KEY_ENABLED_LOCKSCREEN, settings.enabledLockscreen)
            .putBoolean(GlassPrefs.KEY_ENABLED_VOLUME_DIALOG, settings.enabledVolumeDialog)
            .putFloat(GlassPrefs.KEY_BLUR_RADIUS, settings.blurRadius)
            .putFloat(GlassPrefs.KEY_REFRACTION_HEIGHT, settings.refractionHeight)
            .putFloat(GlassPrefs.KEY_REFRACTION_AMOUNT, settings.refractionAmount)
            .putFloat(GlassPrefs.KEY_DEPTH_EFFECT, settings.depthEffect)
            .putFloat(GlassPrefs.KEY_CHROMATIC_ABERRATION, settings.chromaticAberration)
            .putBoolean(GlassPrefs.KEY_USE_DISPERSION, settings.useDispersion)
            .putBoolean(GlassPrefs.KEY_USE_NOISE_TINT, settings.useNoiseTint)
            .putFloat(GlassPrefs.KEY_NOISE_ALPHA, settings.noiseAlpha)
            .putFloat(GlassPrefs.KEY_TINT_ALPHA, settings.tintAlpha)
            .putString(GlassPrefs.KEY_OTHER_APPS_PACKAGES, settings.otherAppsPackages)
            .putFloat(GlassPrefs.KEY_OTHER_APPS_BLUR_RADIUS, settings.otherAppsBlurRadius)
            .apply()
    }
}
