// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Remember a [LayerBackdrop] with a solid background to prevent alpha-blending artifacts.
 *
 * Returns `null` when blur is disabled or the platform cannot render effects, so callers can use
 * the nullability itself as the "is blur active" signal instead of threading a second flag.
 */
@Composable
fun rememberMiuixBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * App bar background colour for a given backdrop.
 *
 * Transparent when a backdrop is active (the blur supplies the colour), otherwise the plain
 * surface colour.
 */
@Composable
fun LayerBackdrop?.getMiuixAppBarColor(): Color =
    this?.let { Color.Transparent } ?: MiuixTheme.colorScheme.surface

/**
 * Standard glassmorphism blur using a Miuix backdrop.
 *
 * A no-op when [enabled] is false or no backdrop was supplied, which keeps call sites free of
 * conditionals.
 */
@Composable
fun Modifier.miuixBlurEffect(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    shape: Shape = RectangleShape,
): Modifier {
    if (!enabled || backdrop == null) return this

    val blendColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.8f)

    return this.then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(
                blendColors = listOf(
                    BlendColorEntry(color = blendColor),
                ),
            ),
        ),
    )
}
