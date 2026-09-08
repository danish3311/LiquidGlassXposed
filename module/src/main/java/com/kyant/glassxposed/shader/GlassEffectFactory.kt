package com.kyant.glassxposed.shader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import com.kyant.glassxposed.R

/**
 * Builds a plain android.graphics.RenderEffect chain that can be attached
 * directly to any View via `view.setRenderEffect(effect)`. No Compose
 * involved anywhere in this file.
 *
 * Pipeline: blur (backdrop) -> refraction (AndroidLiquidGlass shader,
 * ported) -> noise grain + tint (Haze shader, ported). Each stage is
 * optional/skippable via params so you can go from "just blur" up to
 * "full liquid glass with grain".
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU) // 33, required for RuntimeShader/AGSL
object GlassEffectFactory {

    data class GlassParams(
        val widthPx: Float,
        val heightPx: Float,
        val cornerRadiusPx: Float = 0f,
        // -- blur (from AndroidLiquidGlass / plain RenderEffect) --
        val blurRadiusPx: Float = 24f,
        // -- refraction (ported from Kyant0/AndroidLiquidGlass) --
        val refractionHeightPx: Float = 24f,
        val refractionAmountPx: Float = 16f,
        val depthEffect: Float = 0f,
        val chromaticAberration: Float = 0f,
        val useDispersion: Boolean = false,
        val useRefraction: Boolean = true,
        // -- grain + tint (ported from chrisbanes/haze) --
        val useNoiseTint: Boolean = false,
        val noiseAlpha: Float = 0.05f,
        @ColorInt val tintColor: Int = Color.argb(40, 255, 255, 255),
        val noiseScale: Float = 1f
    )

    private var cachedNoiseBitmap: Bitmap? = null

    private fun noiseBitmap(context: Context): Bitmap {
        cachedNoiseBitmap?.let { if (!it.isRecycled) return it }
        val decoded = BitmapFactory.decodeResource(context.resources, R.drawable.glass_noise)
        cachedNoiseBitmap = decoded
        return decoded
    }

    fun build(context: Context, params: GlassParams): RenderEffect {
        var effect: RenderEffect? = null

        // 1. Blur the backdrop.
        if (params.blurRadiusPx > 0f) {
            effect = RenderEffect.createBlurEffect(
                params.blurRadiusPx, params.blurRadiusPx, Shader.TileMode.CLAMP
            )
        }

        // 2. Refraction (bend light near the edges — the "liquid" look).
        if (params.useRefraction) {
            val shaderSource =
                if (params.useDispersion) GlassShaders.ROUNDED_RECT_REFRACTION_DISPERSION
                else GlassShaders.ROUNDED_RECT_REFRACTION

            val shader = RuntimeShader(shaderSource)
            shader.setFloatUniform("size", params.widthPx, params.heightPx)
            shader.setFloatUniform("offset", 0f, 0f)
            shader.setFloatUniform(
                "cornerRadii",
                params.cornerRadiusPx, params.cornerRadiusPx,
                params.cornerRadiusPx, params.cornerRadiusPx
            )
            shader.setFloatUniform("refractionHeight", params.refractionHeightPx.coerceAtLeast(0.01f))
            shader.setFloatUniform("refractionAmount", params.refractionAmountPx)
            shader.setFloatUniform("depthEffect", params.depthEffect)
            if (params.useDispersion) {
                shader.setFloatUniform("chromaticAberration", params.chromaticAberration)
            }

            val refractionEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
            effect = if (effect != null) {
                RenderEffect.createChainEffect(refractionEffect, effect)
            } else {
                refractionEffect
            }
        }

        // 3. Grain + tint — this is what makes it read as "frosted glass"
        // material rather than a plain blurred screenshot.
        if (params.useNoiseTint) {
            val noiseShader = BitmapShader(
                noiseBitmap(context), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT
            )
            val postShader = RuntimeShader(NoiseTintShader.SOURCE)
            postShader.setInputShader("noise", noiseShader)
            postShader.setFloatUniform("noiseAlpha", params.noiseAlpha.coerceIn(0f, 1f))
            postShader.setColorUniform("tintColor", params.tintColor)

            val noiseTintEffect = RenderEffect.createRuntimeShaderEffect(postShader, "content")
            effect = if (effect != null) {
                RenderEffect.createChainEffect(noiseTintEffect, effect)
            } else {
                noiseTintEffect
            }
        }

        // Nothing enabled — return a harmless near-no-op blur rather than
        // null, since RenderEffect itself can't represent "identity".
        return effect ?: RenderEffect.createBlurEffect(0.01f, 0.01f, Shader.TileMode.CLAMP)
    }
}
