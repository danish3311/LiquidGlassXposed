# Third-party code

This project's LSPosed module (`module/`) contains AGSL/SKSL shader source
ported (structure and math preserved, Compose dependency removed) from two
Apache-2.0 licensed projects:

- **Kyant0/AndroidLiquidGlass** — https://github.com/Kyant0/AndroidLiquidGlass
  Copyright 2025 Kyant. Rounded-rect refraction + chromatic dispersion
  shaders (`module/.../shader/GlassShaders.kt`).

- **chrisbanes/haze** — https://github.com/chrisbanes/haze
  Copyright 2023-2024 Christopher Banes and the Haze project contributors.
  Grain + tint post-process shader and noise texture asset
  (`module/.../shader/NoiseTintShader.kt`,
  `module/.../res/drawable-nodpi/glass_noise.webp`).

Both are licensed under the Apache License, Version 2.0:
https://www.apache.org/licenses/LICENSE-2.0

If you publish this module, keep this file and the license headers in the
ported source files intact.
