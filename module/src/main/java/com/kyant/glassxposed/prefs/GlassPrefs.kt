package com.kyant.glassxposed.prefs

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * Reads settings written by the companion app (module/../companion), using
 * LSPosed's world-readable SharedPreferences support:
 * https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences
 *
 * This only works because our AndroidManifest declares
 * <meta-data android:name="xposedsharedprefs" android:value="true" />
 * and the companion app writes to the SAME prefs file name using
 * Context.MODE_WORLD_READABLE (see companion/.../SettingsRepository.kt).
 */
object GlassPrefs {

    private const val PREFS_NAME = "glass_settings"

    // Keys — keep in sync with the companion app.
    const val KEY_ENABLED_STATUS_BAR = "enabled_status_bar"
    const val KEY_ENABLED_SHADE = "enabled_shade"
    const val KEY_ENABLED_NAV_BAR = "enabled_nav_bar"
    const val KEY_BLUR_RADIUS = "blur_radius"
    const val KEY_REFRACTION_HEIGHT = "refraction_height"
    const val KEY_REFRACTION_AMOUNT = "refraction_amount"
    const val KEY_DEPTH_EFFECT = "depth_effect"
    const val KEY_CHROMATIC_ABERRATION = "chromatic_aberration"
    const val KEY_USE_DISPERSION = "use_dispersion"

    @Suppress("DEPRECATION")
    fun read(hookedAppContext: Context): SharedPreferences? {
        return try {
            hookedAppContext.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (e: SecurityException) {
            // LSPosed's world-readable-prefs feature isn't active, or the
            // module isn't actually being loaded by LSPosed for this process.
            XposedBridge.log("GlassXposed: could not read shared prefs world-readable: $e")
            null
        }
    }

    fun isEnabledStatusBar(prefs: SharedPreferences?) =
        prefs?.getBoolean(KEY_ENABLED_STATUS_BAR, true) ?: true

    fun isEnabledShade(prefs: SharedPreferences?) =
        prefs?.getBoolean(KEY_ENABLED_SHADE, true) ?: true

    fun isEnabledNavBar(prefs: SharedPreferences?) =
        prefs?.getBoolean(KEY_ENABLED_NAV_BAR, false) ?: false

    fun blurRadius(prefs: SharedPreferences?) =
        prefs?.getFloat(KEY_BLUR_RADIUS, 24f) ?: 24f

    fun refractionHeight(prefs: SharedPreferences?) =
        prefs?.getFloat(KEY_REFRACTION_HEIGHT, 24f) ?: 24f

    fun refractionAmount(prefs: SharedPreferences?) =
        prefs?.getFloat(KEY_REFRACTION_AMOUNT, 16f) ?: 16f

    fun depthEffect(prefs: SharedPreferences?) =
        prefs?.getFloat(KEY_DEPTH_EFFECT, 0f) ?: 0f

    fun chromaticAberration(prefs: SharedPreferences?) =
        prefs?.getFloat(KEY_CHROMATIC_ABERRATION, 0f) ?: 0f

    fun useDispersion(prefs: SharedPreferences?) =
        prefs?.getBoolean(KEY_USE_DISPERSION, false) ?: false
}
