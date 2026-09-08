package com.kyant.glassxposed.hooks

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import com.kyant.glassxposed.prefs.GlassPrefs
import com.kyant.glassxposed.shader.GlassEffectFactory
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Class names below were verified directly against a real device dump
 * (System_UI_15.apk, crDroid 15 / Android 15 QPR SystemUI) by scanning its
 * classes.dex — see the diagnostic report this file was fixed from.
 *
 * What was actually wrong (both are fixed here, see comments below):
 *
 * 1. `NavigationBarView` moved package on this SystemUI build: it's now
 *    `com.android.systemui.navigationbar.views.NavigationBarView` (extra
 *    "views" segment), not `com.android.systemui.navigationbar.NavigationBarView`.
 *    That's why it threw ClassNotFoundException before.
 *
 * 2. Every OTHER failure (NotificationPanelView, QSPanel, KeyguardStatusView,
 *    VolumeDialogImpl$CustomDialog) was hooking a real, correctly-named
 *    class, but hooking `onAttachedToWindow` specifically — and on this
 *    SystemUI build, most of these classes do NOT override
 *    onAttachedToWindow() themselves (only PhoneStatusBarView and the fixed
 *    NavigationBarView do — which is exactly why those two were the only
 *    ones that ever worked). Some override `onFinishInflate` instead, some
 *    (NotificationPanelView) barely override anything since their real
 *    logic now lives in a separate *Controller class, not the View.
 *
 *    Rather than chase whichever lifecycle method happens to be overridden
 *    per-ROM (fragile — breaks again on the next SystemUI refactor), every
 *    hook below instead hooks the target's CONSTRUCTOR(S) — which always
 *    exist and are always declared directly on the class — and then uses
 *    the plain, stable `View.addOnAttachStateChangeListener` public API to
 *    find out when the view is actually attached. That API is implemented
 *    by View itself, so it works regardless of what the subclass overrides.
 *
 * SCOPE NOTE ("whole phone theming"): this file only reaches surfaces hosted
 * inside com.android.systemui — status bar, shade, QS, lock screen, volume
 * dialog, nav bar. It does NOT reach arbitrary third-party apps' own
 * internal UI — see OtherAppsHooks.kt for the coarser, opt-in option there.
 */
object SystemUIHooks {

    private const val SYSTEMUI_PACKAGE = "com.android.systemui"

    // --- Verified 2026-09 against a real SystemUI.apk dump. Re-check these
    //     with the same dex-scan approach if you move to a different ROM. ---
    private const val CLASS_STATUS_BAR = "com.android.systemui.statusbar.phone.PhoneStatusBarView"
    private const val CLASS_SHADE = "com.android.systemui.shade.NotificationPanelView"
    private const val CLASS_NAV_BAR = "com.android.systemui.navigationbar.views.NavigationBarView"
    private const val CLASS_QS_PANEL = "com.android.systemui.qs.QSPanel"
    private const val CLASS_LOCKSCREEN = "com.android.keyguard.KeyguardStatusView"
    private const val CLASS_VOLUME_DIALOG = "com.android.systemui.volume.VolumeDialogImpl\$CustomDialog"
    // --------------------------------------------------------------

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != SYSTEMUI_PACKAGE) return

        XposedBridge.log("GlassXposed: hooking into $SYSTEMUI_PACKAGE")

        hookViewClass(lpparam, CLASS_STATUS_BAR, "status_bar",
            isEnabled = { GlassPrefs.isEnabledStatusBar(it) }, cornerRadiusPx = 0f)

        hookViewClass(lpparam, CLASS_SHADE, "shade",
            isEnabled = { GlassPrefs.isEnabledShade(it) }, cornerRadiusPx = 48f)

        hookViewClass(lpparam, CLASS_NAV_BAR, "nav_bar",
            isEnabled = { GlassPrefs.isEnabledNavBar(it) }, cornerRadiusPx = 0f)

        hookViewClass(lpparam, CLASS_QS_PANEL, "qs_panel",
            isEnabled = { GlassPrefs.isEnabledQsPanel(it) }, cornerRadiusPx = 32f)

        hookViewClass(lpparam, CLASS_LOCKSCREEN, "lockscreen",
            isEnabled = { GlassPrefs.isEnabledLockscreen(it) }, cornerRadiusPx = 0f)

        hookVolumeDialog(lpparam)
    }

    /**
     * Generic strategy: hook EVERY declared constructor of the target View
     * class (constructors always exist, unlike onAttachedToWindow/
     * onFinishInflate which may or may not be overridden), and once an
     * instance exists, register a plain View.OnAttachStateChangeListener.
     */
    private fun hookViewClass(
        lpparam: XC_LoadPackage.LoadPackageParam,
        className: String,
        targetTag: String,
        isEnabled: (SharedPreferences?) -> Boolean,
        cornerRadiusPx: Float
    ) {
        try {
            val clazz = XposedHelpers.findClass(className, lpparam.classLoader)
            val ctors = clazz.declaredConstructors
            if (ctors.isEmpty()) {
                throw NoSuchMethodException("no declared constructors on $className")
            }

            val hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as? View ?: return
                    registerAttachListener(view, targetTag, isEnabled, cornerRadiusPx)
                }
            }

            var hookedCount = 0
            for (ctor in ctors) {
                try {
                    XposedBridge.hookMethod(ctor, hook)
                    hookedCount++
                } catch (t: Throwable) {
                    XposedBridge.log(
                        "GlassXposed: could not hook one constructor overload of $className: $t"
                    )
                }
            }
            if (hookedCount == 0) {
                throw IllegalStateException("hooked 0 of ${ctors.size} constructors")
            }

            XposedBridge.log(
                "GlassXposed: hooked $className ($targetTag) successfully " +
                    "($hookedCount/${ctors.size} constructor overloads)"
            )
        } catch (t: Throwable) {
            XposedBridge.log(
                "GlassXposed: FAILED to hook $className ($targetTag) — " +
                    "this class/constructor doesn't match this ROM's SystemUI. Error: $t"
            )
        }
    }

    /**
     * The volume dialog is a Dialog, not a View, so it needs its own path.
     * `onStart()` is declared directly on VolumeDialogImpl$CustomDialog and
     * runs just before the dialog's decor view is added to the window
     * manager, so registering an attach listener there is still timely.
     */
    private fun hookVolumeDialog(lpparam: XC_LoadPackage.LoadPackageParam) {
        val className = CLASS_VOLUME_DIALOG
        val targetTag = "volume_dialog"
        try {
            val clazz = XposedHelpers.findClass(className, lpparam.classLoader)

            XposedHelpers.findAndHookMethod(
                clazz, "onStart",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val dialog = param.thisObject as? Dialog ?: return
                        val decorView = dialog.window?.decorView ?: return
                        registerAttachListener(
                            decorView, targetTag,
                            { GlassPrefs.isEnabledVolumeDialog(it) }, 28f
                        )
                    }
                }
            )

            XposedBridge.log("GlassXposed: hooked $className ($targetTag) successfully")
        } catch (t: Throwable) {
            XposedBridge.log(
                "GlassXposed: FAILED to hook $className ($targetTag) — " +
                    "this class/method doesn't match this ROM's SystemUI. Error: $t"
            )
        }
    }

    private fun registerAttachListener(
        view: View,
        targetTag: String,
        isEnabled: (SharedPreferences?) -> Boolean,
        cornerRadiusPx: Float
    ) {
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                applyEffectOnLayout(v, targetTag, isEnabled, cornerRadiusPx)
            }

            override fun onViewDetachedFromWindow(v: View) {
                try {
                    v.setRenderEffect(null)
                } catch (_: Throwable) {
                }
            }
        })
        // Cheap safety net: if the view is somehow already attached by the
        // time we get here, don't wait for a detach/reattach cycle.
        if (view.isAttachedToWindow) {
            applyEffectOnLayout(view, targetTag, isEnabled, cornerRadiusPx)
        }
    }

    private fun applyEffectOnLayout(
        view: View,
        targetTag: String,
        isEnabled: (SharedPreferences?) -> Boolean,
        cornerRadiusPx: Float
    ) {
        view.addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
            try {
                val width = (right - left).toFloat()
                val height = (bottom - top).toFloat()
                if (width <= 0f || height <= 0f) return@addOnLayoutChangeListener

                val context: Context = v.context.applicationContext ?: v.context
                val prefs = GlassPrefs.read()
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
