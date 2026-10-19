package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Applies a backdrop blur effect to the content behind this modifier.
 * Note: Only supports Android 12 (API 31) and above.
 */
fun Modifier.backdropBlur(radius: Float = 10f): Modifier {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val blurEffect = RenderEffect.createBlurEffect(
            radius,
            radius,
            Shader.TileMode.DECAL
        )
        return this.graphicsLayer(renderEffect = blurEffect.asComposeRenderEffect())
    }
    return this
}
