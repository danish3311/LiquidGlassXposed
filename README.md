# Liquid Glass for LSPosed

One app, `app/` — both the LSPosed/Xposed module (hooks `com.android.systemui`)
and its own settings UI (Compose), merged into a single APK. LSPosed never
injects into an app's own process, only into whatever packages you add to
its scope (normally just "System UI"), so one APK can safely be both the
hook and the settings screen for that hook.

It attaches a real `android.graphics.RenderEffect` chain — blur, then an
AGSL refraction shader ported from **Kyant0/AndroidLiquidGlass**, then an
optional grain+tint post-process shader ported from **chrisbanes/haze** —
to the status bar, notification shade, QS panel, lock screen, volume
dialog, and nav bar Views. It also has an experimental, opt-in, coarse
whole-window blur for arbitrary third-party apps (see "How far 'whole
phone theming' actually goes" below).

## What was fixed (from the crDroid 15 diagnostic log + a real SystemUI.apk dump)

The class names below were checked directly against an actual device
`SystemUI.apk` (crDroid 15 / Android 15 QPR), not guessed:

1. **`NavigationBarView` — `ClassNotFoundException`.** It moved package on
   this build: it's `com.android.systemui.navigationbar.views.NavigationBarView`
   now (extra `views` segment), not `com.android.systemui.navigationbar.NavigationBarView`.
   Fixed in `SystemUIHooks.kt`.

2. **`NotificationPanelView`, `QSPanel`, `KeyguardStatusView`,
   `VolumeDialogImpl$CustomDialog` — all `NoSuchMethodError` on
   `onAttachedToWindow`.** These classes were found fine — the problem was
   that most of them don't override `onAttachedToWindow()` on this SystemUI
   build (only `PhoneStatusBarView` and `NavigationBarView` do, which is
   exactly why those were the only two that ever worked). Some override
   `onFinishInflate` instead; `NotificationPanelView` barely overrides
   anything since its logic now lives in a separate `*Controller` class.
   Fixed by no longer depending on any specific lifecycle method: every
   View target now has ALL of its constructors hooked (constructors always
   exist), and once an instance exists we register a plain
   `View.addOnAttachStateChangeListener` — a stable public API implemented
   by `View` itself regardless of what the subclass overrides. The volume
   dialog (a `Dialog`, not a `View`) hooks `onStart()` instead, which IS
   declared directly on `CustomDialog`, and reaches its decor view from
   there.

3. **~173 repeats of `SecurityException: MODE_WORLD_READABLE no longer
   supported`.** `Context.getSharedPreferences(name, MODE_WORLD_READABLE)`
   has thrown that unconditionally since Android 7 — LSPosed or not, that
   flag is simply gone from the framework. The actually-supported bridge
   (per [LSPosed's own docs](https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences))
   is: the settings UI writes with plain `MODE_PRIVATE`
   (`settings/SettingsRepository.kt`), and the hook reads with
   `XSharedPreferences` (`prefs/GlassPrefs.kt`), which reads the prefs XML
   straight off disk instead of going through `ContextImpl`'s permission
   checks. The `xposedsharedprefs` manifest meta-data (already present) is
   what makes LSPosed keep that file readable at the filesystem level.

4. **Settings UI: text hidden under the status bar / nav bar.** `targetSdk
   35` enforces edge-to-edge by default, so without explicit inset handling
   content draws underneath the system bars instead of being padded clear
   of them. Fixed in `settings/MainActivity.kt` with `enableEdgeToEdge()`
   plus `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` on the
   scrolling content.

5. **Two separate apps (`module` + `companion`) with duplicated prefs key
   strings** that could silently drift apart. Merged into one `app` module;
   the settings UI now references `GlassPrefs`'s key constants directly
   instead of keeping its own copies.

## Where each piece came from

| File | Ported from | What it adds |
|---|---|---|
| `shader/GlassShaders.kt` | Kyant0/AndroidLiquidGlass, `backdrop/.../internal/Shaders.kt` | Rounded-rect refraction (+ optional chromatic dispersion) — the "liquid" bending look |
| `shader/NoiseTintShader.kt` + `res/drawable-nodpi/glass_noise.webp` | chrisbanes/haze, `haze-blur/.../BlurRenderEffect.android.kt` | Grain + tint post-process — this is what makes blur read as "frosted glass material" instead of a flat blurred screenshot |
| `shader/GlassEffectFactory.kt` | New, chains the two above with plain `RenderEffect.createBlurEffect` | The actual pipeline: blur → refraction → grain/tint, bindable to any View |

Both source libraries are Apache-2.0; both are Compose-only in their original
form, which is why the shader files don't import Compose — everything is
re-expressed against plain `android.graphics.RuntimeShader`/`RenderEffect`
so it can bind to SystemUI's real (non-Compose) Views from a hook.

## How far "whole phone theming" actually goes

1. **SystemUI surfaces (`SystemUIHooks.kt`)** — status bar, shade, QS panel,
   lock screen, volume dialog, nav bar. Verified against a real device dump;
   applies the *full* glass pipeline (refraction, dispersion, grain)
   because we're targeting specific background/container views, not text.
2. **Launcher** — same idea is possible (dock, search bar, widget
   backgrounds) if you say which launcher you run, so its real class names
   can be looked up — not included yet.
3. **Arbitrary third-party apps (`OtherAppsHooks.kt`)** — there's no shared
   class across apps you don't control the source of, so the only generic
   hook available is "blur this app's entire decorView", text included.
   Opt-in per package, off by default.

## Build

1. Open the `LiquidGlassXposed/` folder in Android Studio (needs network
   access to Google's Maven repo, Maven Central, and `https://api.xposed.info/`
   — already configured in `settings.gradle.kts`).
2. Build > Build APK, or Run with `app` as the run configuration.

## Install & test

1. Install the APK, enable it in LSPosed Manager, set its scope to include
   **System UI**.
2. Reboot (SystemUI hooks need a fresh process — restarting SystemUI from
   Magisk/LSPosed Manager may also work).
3. Launch "Liquid Glass" from your app drawer to adjust sliders/toggles.
4. Check LSPosed Manager's log viewer for `GlassXposed:` lines to see which
   hooks landed and which failed — each one still logs independently and
   never crashes SystemUI on failure.

## Known risk

Hooking SystemUI wrong can crash it and put you in a boot loop of restarts.
Test with the module scoped ONLY to System UI (not system-wide), keep
Magisk/LSPosed's safe-mode escape hatch in mind (per LSPosed docs: boot loop
→ hold volume down during boot to enter LSPosed safe mode and disable
modules), and don't test this on a device you can't easily recover.
