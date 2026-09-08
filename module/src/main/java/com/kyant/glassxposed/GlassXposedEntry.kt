package com.kyant.glassxposed

import com.kyant.glassxposed.hooks.SystemUIHooks
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class GlassXposedEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        XposedBridge.log("GlassXposed: loaded into ${lpparam.packageName}")

        try {
            SystemUIHooks.install(lpparam)
        } catch (t: Throwable) {
            XposedBridge.log("GlassXposed: top-level hook install failed: $t")
        }
    }
}
