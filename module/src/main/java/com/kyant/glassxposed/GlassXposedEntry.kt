package com.kyant.glassxposed

import com.kyant.glassxposed.hooks.OtherAppsHooks
import com.kyant.glassxposed.hooks.SystemUIHooks
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class GlassXposedEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            if (lpparam.packageName == "com.android.systemui") {
                XposedBridge.log("GlassXposed: loaded into ${lpparam.packageName}")
                SystemUIHooks.install(lpparam)
            } else {
                // Only does anything for packages the user explicitly opted
                // into via the companion app's "other apps" list — see
                // OtherAppsHooks.kt for why this is coarse/experimental.
                OtherAppsHooks.install(lpparam)
            }
        } catch (t: Throwable) {
            XposedBridge.log("GlassXposed: top-level hook install failed for ${lpparam.packageName}: $t")
        }
    }
}
