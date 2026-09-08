package com.kyant.glassxposed.hooks

import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.Shader
import com.kyant.glassxposed.prefs.GlassPrefs
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * READ THIS BEFORE ENABLING ANYTHING HERE.
 *
 * SystemUIHooks.kt can target specific known Views because we know
 * SystemUI's internal class names. Arbitrary third-party apps don't share
 * any common structure we can hook into the same way — there's no
 * "notification shade" class in, say, Chrome or WhatsApp.
 *
 * The only generic thing we CAN do to an app we don't control the source
 * of is apply blur (not refraction — refraction distorts text/layout,
 * blur just softens it) to its entire decorView. That blurs the WHOLE
 * app's UI uniformly, including text — this is a coarse "make this app's
 * window fuzzy" effect, not real theming/glass-ifying of specific
 * elements. It's really only useful for things like camera/media
 * backgrounds behind your own overlays, screenshots for a lock-screen-style
 * preview, or deliberate privacy blur — not for making a chat app "glassy".
 *
 * This only activates for packages the user explicitly lists in the
 * settings screen (GlassPrefs.KEY_OTHER_APPS_PACKAGES) AND that you've also
 * added to this module's scope in LSPosed Manager (LSPosed hooks nothing
 * outside its configured scope, regardless of what this code says).
 */
object OtherAppsHooks {

    fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Never touch system processes with this coarse path.
        if (lpparam.packageName == "android" ||
            lpparam.packageName == "com.android.systemui"
        ) {
            return
        }

        try {
            val activityClass = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(
                activityClass, "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        val pkg = activity.packageName ?: return

                        val prefs = GlassPrefs.read()
                        val targets = GlassPrefs.otherAppsPackages(prefs)
                        if (pkg !in targets) return

                        val radius = GlassPrefs.otherAppsBlurRadius(prefs)
                        if (radius <= 0f) return

                        try {
                            activity.window?.decorView?.setRenderEffect(
                                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                            )
                        } catch (t: Throwable) {
                            XposedBridge.log("GlassXposed: whole-app blur failed for $pkg: $t")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("GlassXposed: OtherAppsHooks install failed for ${lpparam.packageName}: $t")
        }
    }
}
