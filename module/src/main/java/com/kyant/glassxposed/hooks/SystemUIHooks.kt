package com.kyant.glassxposed.hooks

import android.content.Context
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
 * The class names below (PhoneStatusBarView, NotificationPanelViewController,
 * NavigationBarView, etc.) are the "usual" AOSP SystemUI internal class names
 * as of Android 12-15. crDroid forks SystemUI and *can* rename, merge, or
 * restructure these. Before this will actually attach anything, you need to
 * verify the real class/view names on YOUR crDroid 15 build:
 *
 *   1. Pull /system_ext/priv-app/SystemUI/SystemUI.apk (or wherever your
 *      ROM ships it) off the device (adb pull, or use a root file manager).
 *   2. Open it in jadx-gui and search for "StatusBarView", "NotificationPanel",
 *      "NavigationBarView" to find the real class names in your build.
 *   3. Update CLASS_STATUS_BAR / CLASS_SHADE / CLASS_NAV_BAR below.
 *
 * Every hook is wrapped in its own try/catch and logs failures individually,
 * so a wrong class name for one target won't crash the others or crash
 * SystemUI itself.
 */
object SystemUIHooks {

    private const val SYSTEMUI_PACKAGE = "com.android.systemui"

    // --- Adjust these to match your ROM's actual SystemUI internals ---
    private const val CLASS_STATUS_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBarView"
    private const val CLASS_SHADE = "com.android.systemui.shade.NotificationPanelView"
    private const val CLASS_NAV_BAR = "com.android.systemui.navigationbar.NavigationBarView"
    // --------------------------------------------------------------

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != SYSTEMUI_PACKAGE) return

        XposedBridge.log("GlassXposed: hooking into $SYSTEMUI_PACKAGE")

        hookRootViewOnAttach(
            lpparam,
            className = CLASS_STATUS_BAR,
            targetTag = "status_bar",
            isEnabled = { prefs -> GlassPrefs.isEnabledStatusBar(prefs) },
            cornerRadiusPx = 0f
        )

        hookRootViewOnAttach(
            lpparam,
            className = CLASS_SHADE,
            targetTag = "shade",
            isEnabled = { prefs -> GlassPrefs.isEnabledShade(prefs) },
            cornerRadiusPx = 48f
        )

        hookRootViewOnAttach(
            lpparam,
            className = CLASS_NAV_BAR,
            targetTag = "nav_bar",
            isEnabled = { prefs -> GlassPrefs.isEnabledNavBar(prefs) },
            cornerRadiusPx = 0f
        )
    }

    /**
     * Generic strategy: hook the target View's onAttachedToWindow (or
     * onFinishInflate, whichever exists) and apply setRenderEffect once the
     * View has real dimensions. We re-apply on every layout change since
     * the shader's `size` uniform needs to match the current bounds.
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

                val prefs = GlassPrefs.read(v.context.applicationContext ?: v.context)
                if (!isEnabled(prefs)) {
                    v.setRenderEffect(null)
                    return@addOnLayoutChangeListener
                }

                val params = GlassEffectFactory.GlassParams(
                    widthPx = width,
                    heightPx = height,
                    cornerRadiusPx = cornerRadiusPx,
                    blurRadiusPx = GlassPrefs.blurRadius(prefs),
                    refractionHeightPx = GlassPrefs.refractionHeight(prefs),
                    refractionAmountPx = GlassPrefs.refractionAmount(prefs),
                    depthEffect = GlassPrefs.depthEffect(prefs),
                    chromaticAberration = GlassPrefs.chromaticAberration(prefs),
                    useDispersion = GlassPrefs.useDispersion(prefs)
                )
                v.setRenderEffect(GlassEffectFactory.build(params))
            } catch (t: Throwable) {
                XposedBridge.log("GlassXposed: failed to apply effect to $targetTag: $t")
            }
        }
    }
}
