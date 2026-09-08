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
    const val KEY_ENABLED_QS_PANEL = "enabled_qs_panel"
    const val KEY_ENABLED_LOCKSCREEN = "enabled_lockscreen"
    const val KEY_ENABLED_VOLUME_DIALOG = "enabled_volume_dialog"
    const val KEY_USE_NOISE_TINT = "use_noise_tint"
    const val KEY_NOISE_ALPHA = "noise_alpha"
    const val KEY_TINT_ALPHA = "tint_alpha"
    // Comma-separated package names the user picked for the coarse
    // "blur this whole app's window" option — see OtherAppsHooks.kt.
    const val KEY_OTHER_APPS_PACKAGES = "other_apps_packages"
    const val KEY_OTHER_APPS_BLUR_RADIUS = "other_apps_blur_radius"

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

    fun isEnabledQsPanel(prefs: SharedPreferences?) =
        prefs?.getBoolean(KEY_ENABLED_QS_PANEL, true) ?: true

    fun isEnabledLockscreen(prefs: SharedPreferences?) =
        prefs?.getBoolean(KEY_ENABLED_LOCKSCREEN, false) ?: false

    fun isEnabledVolumeDialog(prefs: SharedPreferences?) =
        prefs?.getBoolean(KEY_ENABLED_VOLUME_DIALOG, false) ?: false

    fun useNoiseTint(prefs: SharedPreferences?) =
        prefs?.getBoolean(KEY_USE_NOISE_TINT, false) ?: false

    fun noiseAlpha(prefs: SharedPreferences?) =
        prefs?.getFloat(KEY_NOISE_ALPHA, 0.05f) ?: 0.05f

    fun tintAlpha(prefs: SharedPreferences?) =
        prefs?.getFloat(KEY_TINT_ALPHA, 40f) ?: 40f

    /** Package names the user opted in to coarse whole-window blur for. */
    fun otherAppsPackages(prefs: SharedPreferences?): Set<String> =
        prefs?.getString(KEY_OTHER_APPS_PACKAGES, "")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: emptySet()

    fun otherAppsBlurRadius(prefs: SharedPreferences?) =
        prefs?.getFloat(KEY_OTHER_APPS_BLUR_RADIUS, 20f) ?: 20f
}
