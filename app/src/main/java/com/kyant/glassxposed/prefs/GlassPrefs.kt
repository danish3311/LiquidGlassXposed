package com.kyant.glassxposed.prefs

import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

/**
 * Reads settings written by the settings UI (com.kyant.glassxposed.settings
 * .MainActivity / SettingsRepository — same app, same process as this
 * object when running normally, but a DIFFERENT process when this code
 * itself is loaded here — inside the hooked com.android.systemui process).
 *
 * BUG THIS FIXES: `Context.getSharedPreferences(name, MODE_WORLD_READABLE)`
 * throws `SecurityException: MODE_WORLD_READABLE no longer supported`
 * unconditionally since Android 7 — that flag is gone from the framework
 * entirely, LSPosed or not. It never worked on this device and never will.
 *
 * The actually-supported mechanism (see
 * https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences) is:
 *   1. This app's manifest declares
 *      <meta-data android:name="xposedsharedprefs" android:value="true" />
 *      so LSPosed keeps this app's shared_prefs file readable at the
 *      filesystem level whenever it changes.
 *   2. The WRITER side (SettingsRepository, running as this app normally)
 *      just uses plain Context.MODE_PRIVATE — nothing special.
 *   3. The READER side (here, running inside SystemUI) uses
 *      `XSharedPreferences`, which reads the prefs XML file directly off
 *      disk instead of going through ContextImpl's permission checks —
 *      MODE_WORLD_READABLE is not involved on either side anymore.
 */
object GlassPrefs {

    private const val MODULE_PACKAGE = "com.kyant.glassxposed"

    // Also used directly by SettingsRepository, since it's compiled into
    // this same app now — one source of truth for the prefs file name and
    // every key, so the writer and reader can never drift out of sync.
    const val PREFS_NAME = "glass_settings"

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

    @Volatile
    private var xPrefs: XSharedPreferences? = null

    private fun instance(): XSharedPreferences {
        xPrefs?.let { return it }
        synchronized(this) {
            xPrefs?.let { return it }
            val created = XSharedPreferences(MODULE_PACKAGE, PREFS_NAME)
            @Suppress("DEPRECATION")
            created.makeWorldReadable()
            xPrefs = created
            return created
        }
    }

    /** Returns null if the settings file doesn't exist yet or can't be read. */
    fun read(): SharedPreferences? {
        return try {
            val prefs = instance()
            if (!prefs.file.canRead()) {
                XposedBridge.log(
                    "GlassXposed: settings file not readable yet — open the Liquid Glass " +
                        "app once and toggle a setting so the file gets created, and make " +
                        "sure this module's own package is enabled in LSPosed Manager " +
                        "(it needs to run once, unhooked, to write its own prefs)."
                )
                return null
            }
            prefs.reload()
            prefs
        } catch (t: Throwable) {
            XposedBridge.log("GlassXposed: could not read settings via XSharedPreferences: $t")
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
