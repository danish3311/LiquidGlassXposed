package com.kyant.glassxposed.shader

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Builds a plain android.graphics.RenderEffect chain (blur -> refraction
 * shader) that can be attached directly to any View via
 * `view.setRenderEffect(effect)`. No Compose involved — this is what makes
 * it possible to apply the effect to SystemUI's own (non-Compose) Views
 * from an Xposed hook.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU) // 33, required for RuntimeShader/AGSL
object GlassEffectFactory {

    data class GlassParams(
        val widthPx: Float,
        val heightPx: Float,
        val cornerRadiusPx: Float = 0f,
        val blurRadiusPx: Float = 24f,
        val refractionHeightPx: Float = 24f,
        val refractionAmountPx: Float = 16f,
        val depthEffect: Float = 0f,
        val chromaticAberration: Float = 0f,
        val useDispersion: Boolean = false
    )

    fun build(params: GlassParams): RenderEffect {
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

        // Blur the backdrop first, then let the refraction shader sample
        // the blurred result as its "content" input.
        val blurEffect: RenderEffect? =
            if (params.blurRadiusPx > 0f) {
                RenderEffect.createBlurEffect(
                    params.blurRadiusPx, params.blurRadiusPx, Shader.TileMode.CLAMP
                )
            } else null

        val refractionEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")

        return if (blurEffect != null) {
            RenderEffect.createChainEffect(refractionEffect, blurEffect)
        } else {
            refractionEffect
        }
    }
}
