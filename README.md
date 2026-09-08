# Liquid Glass for LSPosed — starter project

Two apps:

- **`module/`** — the actual LSPosed/Xposed module. Hooks `com.android.systemui`
  and attaches a real `android.graphics.RenderEffect` chain — blur, then an
  AGSL refraction shader ported from **Kyant0/AndroidLiquidGlass**, then an
  optional grain+tint post-process shader ported from **chrisbanes/haze** —
  to the status bar, notification shade, QS panel, lock screen, volume
  dialog, and nav bar Views. Also has an experimental, opt-in, coarse
  whole-window blur for arbitrary third-party apps (see "How far 'whole
  phone theming' actually goes" below).
- **`companion/`** — a normal (unhooked) settings app, built with Compose and
  the real `Kyant0/AndroidLiquidGlass` library, that writes the effect
  parameters the module reads.

## Where each piece came from

| File | Ported from | What it adds |
|---|---|---|
| `shader/GlassShaders.kt` | Kyant0/AndroidLiquidGlass, `backdrop/.../internal/Shaders.kt` | Rounded-rect refraction (+ optional chromatic dispersion) — the "liquid" bending look |
| `shader/NoiseTintShader.kt` + `res/drawable-nodpi/glass_noise.webp` | chrisbanes/haze, `haze-blur/.../BlurRenderEffect.android.kt` | Grain + tint post-process — this is what makes blur read as "frosted glass material" instead of a flat blurred screenshot |
| `shader/GlassEffectFactory.kt` | New, chains the two above with plain `RenderEffect.createBlurEffect` | The actual pipeline: blur → refraction → grain/tint, bindable to any View |

Both source libraries are Apache-2.0; both are Compose-only in their original
form, which is why nothing here imports Compose — everything was re-expressed
against plain `android.graphics.RuntimeShader`/`RenderEffect` so it can bind
to SystemUI's real (non-Compose) Views from a hook.

## How far "whole phone theming" actually goes

Three tiers, in order of how well they actually work:

1. **SystemUI surfaces (`SystemUIHooks.kt`)** — status bar, shade, QS panel,
   lock screen, volume dialog, nav bar. We know these classes' names (AOSP-ish,
   verify against your ROM) and can apply the *full* glass pipeline —
   refraction, dispersion, grain — cleanly, because we're targeting specific
   background/container views, not text.
2. **Launcher** — same idea as #1 is possible (dock, search bar, widget
   backgrounds) if you tell me which launcher you're running (crDroid's
   default, Lawnchair, Nova, etc.) so I can look up its actual class names —
   not included yet, ask if you want it.
3. **Arbitrary third-party apps (`OtherAppsHooks.kt`)** — there's no shared
   class across apps you don't control the source of, so the only generic
   hook available is "blur this app's entire decorView", which blurs
   everything the app draws, text included. That's not real theming of
   specific UI elements — it's a blanket fuzz toggle, opt-in per package,
   and honestly mostly a novelty rather than something you'd want on a
   messaging or browser app. I've wired it up (companion app has a package
   list + blur slider for it) but kept it clearly separate and off by default.

If what you actually want is "make every app look glassy", the realistic
path is tier 1 + 2 (everything the OS itself draws) rather than tier 3 —
that's what other system-wide theming projects (Substratum, HyperCeiler-style
mods) actually do too; none of them "glass-ify" arbitrary third-party app
internals either, for the same structural reason.

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
   from Maven Central — Haze's code was ported into module source directly,
   so the `dev.chrisbanes.haze` library itself is NOT a dependency, only its
   ported shader + the noise texture asset).
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
