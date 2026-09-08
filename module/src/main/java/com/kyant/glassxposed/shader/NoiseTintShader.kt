package com.kyant.glassxposed.shader

/**
 * Ported from chrisbanes/haze:
 *   haze-blur/src/androidMain/kotlin/dev/chrisbanes/haze/blur/BlurRenderEffect.android.kt
 *   (constant COMBINED_NOISE_TINT_SKSL)
 *
 * This is what actually sells the "frosted glass" look — blur alone looks
 * flat/plasticky; a soft-light-blended grain texture plus a tint color is
 * what real glass/material blur implementations (iOS, Haze, this) add on
 * top of the blur. Same deal as the refraction shader: pure AGSL/SKSL, no
 * Compose dependency, so it binds straight to a RuntimeShader here.
 *
 * License: Apache-2.0, original copyright Chris Banes and the Haze project
 * contributors.
 */
internal object NoiseTintShader {
    val SOURCE = """
uniform shader content;
uniform shader noise;
uniform float noiseAlpha;
layout(color) uniform half4 tintColor;

half guardedDivide(half numerator, half denominator) {
    return denominator == 0.0 ? 0.0 : numerator / denominator;
}

half softLightComponent(half2 source, half2 destination) {
    if (2.0 * source.x <= source.y) {
        return guardedDivide(
            destination.x * destination.x * (source.y - 2.0 * source.x),
            destination.y
        ) + (1.0 - destination.y) * source.x
            + destination.x * (-source.y + 2.0 * source.x + 1.0);
    } else if (4.0 * destination.x <= destination.y) {
        half destinationSquared = destination.x * destination.x;
        half destinationCubed = destinationSquared * destination.x;
        half destinationAlphaSquared = destination.y * destination.y;
        half destinationAlphaCubed = destinationAlphaSquared * destination.y;
        return guardedDivide(
            destinationAlphaSquared
                * (source.x - destination.x * (3.0 * source.y - 6.0 * source.x - 1.0))
                + 12.0 * destination.y * destinationSquared * (source.y - 2.0 * source.x)
                - 16.0 * destinationCubed * (source.y - 2.0 * source.x)
                - destinationAlphaCubed * source.x,
            destinationAlphaSquared
        );
    } else {
        return destination.x * (source.y - 2.0 * source.x + 1.0)
            + source.x
            - sqrt(destination.y * destination.x) * (source.y - 2.0 * source.x)
            - destination.y * source.x;
    }
}

half4 softLight(half4 source, half4 destination) {
    if (destination.a == 0.0) {
        return source;
    }
    return half4(
        softLightComponent(source.ra, destination.ra),
        softLightComponent(source.ga, destination.ga),
        softLightComponent(source.ba, destination.ba),
        source.a + (1.0 - source.a) * destination.a
    );
}

half4 main(float2 coord) {
    half4 blurred = content.eval(coord);
    half4 noiseColor = noise.eval(coord) * noiseAlpha;
    half4 withNoise = softLight(noiseColor, blurred);
    half4 tint = tintColor.rgb1 * tintColor.a;
    return tint + withNoise * (1.0 - tintColor.a);
}"""
}
