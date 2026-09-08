package com.kyant.glassxposed.hooks

import android.content.Context
import android.graphics.Color
import android.view.View
import com.kyant.glassxposed.prefs.GlassPrefs
import com.kyant.glassxposed.shader.GlassEffectFactory
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * IMPORTANT — READ THIS FIRST:
 *
 * The class names below are the "usual" AOSP SystemUI internal class names
 * as of Android 12-15. crDroid forks SystemUI and *can* rename, merge, or
 * restructure these. Before this will actually attach anything, verify the
 * real class/view names on YOUR crDroid 15 build:
 *
 *   1. Pull /system_ext/priv-app/SystemUI/SystemUI.apk off the device.
 *   2. Open it in jadx-gui, search for "StatusBarView", "NotificationPanel",
 *      "NavigationBarView", "QSPanel", "KeyguardStatusView", "VolumeDialog".
 *   3. Update the CLASS_* constants below.
 *
 * Every hook is wrapped in its own try/catch and logs failures individually,
 * so a wrong class name for one target won't crash the others or SystemUI.
 *
 * SCOPE NOTE ("whole phone theming"): this file only reaches surfaces hosted
 * inside com.android.systemui — status bar, shade, QS, lock screen, volume
 * dialog, nav bar. That covers most of what people mean by "system theming".
 * It does NOT reach arbitrary third-party apps' own internal UI (Chrome,
 * Settings, etc.) — there's no shared class to target there. See
 * OtherAppsHooks.kt for the coarser, opt-in option for that.
 */
object SystemUIHooks {

    private const val SYSTEMUI_PACKAGE = "com.android.systemui"

    // --- Adjust these to match your ROM's actual SystemUI internals ---
    private const val CLASS_STATUS_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBarView"
    private const val CLASS_SHADE = "com.android.systemui.shade.NotificationPanelView"
    private const val CLASS_NAV_BAR = "com.android.systemui.navigationbar.NavigationBarView"
    private const val CLASS_QS_PANEL = "com.android.systemui.qs.QSPanel"
    private const val CLASS_LOCKSCREEN = "com.android.keyguard.KeyguardStatusView"
    private const val CLASS_VOLUME_DIALOG = "com.android.systemui.volume.VolumeDialogImpl\$CustomDialog"
    // --------------------------------------------------------------

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != SYSTEMUI_PACKAGE) return

        XposedBridge.log("GlassXposed: hooking into $SYSTEMUI_PACKAGE")

        hookRootViewOnAttach(lpparam, CLASS_STATUS_BAR, "status_bar",
            isEnabled = { GlassPrefs.isEnabledStatusBar(it) }, cornerRadiusPx = 0f)

        hookRootViewOnAttach(lpparam, CLASS_SHADE, "shade",
            isEnabled = { GlassPrefs.isEnabledShade(it) }, cornerRadiusPx = 48f)

        hookRootViewOnAttach(lpparam, CLASS_NAV_BAR, "nav_bar",
            isEnabled = { GlassPrefs.isEnabledNavBar(it) }, cornerRadiusPx = 0f)

        hookRootViewOnAttach(lpparam, CLASS_QS_PANEL, "qs_panel",
            isEnabled = { GlassPrefs.isEnabledQsPanel(it) }, cornerRadiusPx = 32f)

        hookRootViewOnAttach(lpparam, CLASS_LOCKSCREEN, "lockscreen",
            isEnabled = { GlassPrefs.isEnabledLockscreen(it) }, cornerRadiusPx = 0f)

        hookRootViewOnAttach(lpparam, CLASS_VOLUME_DIALOG, "volume_dialog",
            isEnabled = { GlassPrefs.isEnabledVolumeDialog(it) }, cornerRadiusPx = 28f)
    }

    /**
     * Generic strategy: hook the target View's onAttachedToWindow and apply
     * setRenderEffect once the View has real dimensions, re-applied on every
     * layout change since the shader's `size` uniform must match bounds.
     */
    private fun hookRootViewOnAttach(
        lpparam: XC_LoadPackage.LoadPackageParam,
        className: String,
        targetTag: String,
        isEnabled: (android.content.SharedPreferences?) -> Boolean,
        cornerRadiusPx: Float
    ) {
        try {
            val clazz = XposedHelpers.findClass(className, lpparam.classLoader)

            XposedHelpers.findAndHookMethod(
                clazz, "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        applyEffectOnLayout(view, targetTag, isEnabled, cornerRadiusPx)
                    }
                }
            )

            XposedBridge.log("GlassXposed: hooked $className ($targetTag) successfully")
        } catch (t: Throwable) {
            XposedBridge.log(
                "GlassXposed: FAILED to hook $className ($targetTag) — " +
                    "this class probably doesn't exist under this name on your ROM. " +
                    "See the comment at the top of SystemUIHooks.kt. Error: $t"
            )
        }
    }

    private fun applyEffectOnLayout(
        view: View,
        targetTag: String,
        isEnabled: (android.content.SharedPreferences?) -> Boolean,
        cornerRadiusPx: Float
    ) {
        view.addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
            try {
                val width = (right - left).toFloat()
                val height = (bottom - top).toFloat()
                if (width <= 0f || height <= 0f) return@addOnLayoutChangeListener

                val context: Context = v.context.applicationContext ?: v.context
                val prefs = GlassPrefs.read(context)
                if (!isEnabled(prefs)) {
                    v.setRenderEffect(null)
                    return@addOnLayoutChangeListener
                }

                val tintAlphaByte = GlassPrefs.tintAlpha(prefs).toInt().coerceIn(0, 255)
                val params = GlassEffectFactory.GlassParams(
                    widthPx = width,
                    heightPx = height,
                    cornerRadiusPx = cornerRadiusPx,
                    blurRadiusPx = GlassPrefs.blurRadius(prefs),
                    refractionHeightPx = GlassPrefs.refractionHeight(prefs),
                    refractionAmountPx = GlassPrefs.refractionAmount(prefs),
                    depthEffect = GlassPrefs.depthEffect(prefs),
                    chromaticAberration = GlassPrefs.chromaticAberration(prefs),
                    useDispersion = GlassPrefs.useDispersion(prefs),
                    useNoiseTint = GlassPrefs.useNoiseTint(prefs),
                    noiseAlpha = GlassPrefs.noiseAlpha(prefs),
                    tintColor = Color.argb(tintAlphaByte, 255, 255, 255)
                )
                v.setRenderEffect(GlassEffectFactory.build(context, params))
            } catch (t: Throwable) {
                XposedBridge.log("GlassXposed: failed to apply effect to $targetTag: $t")
            }
        }
    }
}
