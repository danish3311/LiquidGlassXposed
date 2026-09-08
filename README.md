# Liquid Glass for LSPosed — starter project

Two apps:

- **`module/`** — the actual LSPosed/Xposed module. Hooks `com.android.systemui`
  and attaches a real `android.graphics.RenderEffect` (blur + AGSL refraction
  shader) to the status bar, notification shade, and nav bar Views.
- **`companion/`** — a normal (unhooked) settings app, built with Compose and
  the real `Kyant0/AndroidLiquidGlass` library, that writes the effect
  parameters the module reads.

## What's real vs. what needs your verification

**Solid / verified against the library source:**
- `module/.../shader/GlassShaders.kt` — the AGSL shader text, copied straight
  from `backdrop/src/commonMain/.../internal/Shaders.kt` in the original repo.
  It's pure AGSL with no Compose dependency, so it's a faithful port.
- `module/.../shader/GlassEffectFactory.kt` — builds the same blur→refraction
  `RenderEffect` chain the library builds internally, using plain
  `android.graphics.RuntimeShader`/`RenderEffect`, so it can be attached to
  any View via `view.setRenderEffect(...)`.
- The `xposedsharedprefs` / `MODE_WORLD_READABLE` settings bridge, per
  LSPosed's own docs (https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences).

**Needs YOUR verification — will not work as-is:**
- `module/.../hooks/SystemUIHooks.kt` — the three class names
  (`CLASS_STATUS_BAR`, `CLASS_SHADE`, `CLASS_NAV_BAR`) are the typical AOSP
  names for Android 12-15. crDroid forks SystemUI and may rename these.
  **Before testing**, pull SystemUI.apk off your device and check the real
  class names in jadx-gui (instructions are in a comment at the top of that
  file). Each hook fails independently and logs to `XposedBridge.log`
  ("GlassXposed: FAILED to hook ...") rather than crashing SystemUI, so you
  can iterate on the class names safely.

## Build

I couldn't compile this in my own sandbox (no Android SDK / no access to
Google's Maven repo from here), so this is unbuilt source, not an APK.
On your machine:

1. Open the `LiquidGlassXposed/` folder in Android Studio.
2. Let it sync (needs the classic Xposed API from `https://api.xposed.info/`,
   already added to `settings.gradle.kts`, and `io.github.kyant0:backdrop`
   from Maven Central).
3. Build > Build APK(s) for both `module` and `companion`, or just hit Run
   with each as the selected run configuration.

## Install & test

1. Install `module`'s APK, enable it in LSPosed Manager, set its scope to
   include **System UI**.
2. Reboot (SystemUI hooks need a fresh process — a "soft reboot"/restart
   SystemUI from Magisk or LSPosed Manager may also work).
3. Install `companion`'s APK, adjust sliders.
4. Check LSPosed Manager's log viewer for `GlassXposed:` lines to see which
   hooks landed and which failed.

## Known risk

Hooking SystemUI wrong can crash it and put you in a boot loop of restarts.
Test with the module scoped ONLY to System UI (not system-wide), keep Magisk/
LSPosed's safe-mode escape hatch in mind (per LSPosed docs: boot loop → hold
volume down during boot to enter LSPosed safe mode and disable modules), and
don't test this on a device you can't easily recover.
