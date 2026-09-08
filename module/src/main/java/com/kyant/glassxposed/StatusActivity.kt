package com.kyant.glassxposed

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

/**
 * This activity runs UNHOOKED — Xposed never injects into its own app
 * process, only into the target packages (com.android.systemui here). It
 * just tells you whether LSPosed even loaded the module, and reminds you
 * to use the companion app for actual settings.
 */
class StatusActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            textSize = 16f
            setPadding(48, 96, 48, 48)
            text = buildString {
                appendLine("Liquid Glass (Xposed module)\n")
                appendLine("If you're reading this normally (not via Xposed hook), that's expected —")
                appendLine("this module only activates inside com.android.systemui.\n")
                appendLine("Checklist:")
                appendLine("1. Enable this module in LSPosed Manager.")
                appendLine("2. Set its scope to include \"System UI\" (com.android.systemui).")
                appendLine("3. Reboot (SystemUI hooks need a fresh SystemUI process).")
                appendLine("4. Install the companion settings app to adjust blur/refraction.")
                appendLine("5. Check LSPosed Manager > Logs for lines starting with \"GlassXposed:\"")
                appendLine("   to see which SystemUI views were successfully hooked.")
            }
        }

        setContentView(LinearLayout(this).apply { addView(text) })
    }
}
