// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Ambient values shared by every Miuix widget inside the current UI family.
 *
 * A widget can therefore pick its OS4 (glass) or OS3 (classic) presentation without each caller
 * threading a `useOs4` flag down the tree. That keeps the two styles swappable at the root while
 * keeping widget signatures small.
 */
data class MiuixComponentScope(
    val useOs4: Boolean = false,
    val contentBackdrop: LayerBackdrop? = null,
    val pageBackdrop: LayerBackdrop? = null,
    /**
     * Whether OS4 popups (dropdowns, menus) should render inside the root scaffold.
     *
     * Keeping them in the root keeps a popup above page content and unaffected by page scrolling,
     * but it also means the popup is not clipped by the page's own layout.
     */
    val popupInRootScaffold: Boolean = true,
) {
    /** Sample surface for content that sits under a scrolling page. */
    val contentBackdropModifier: Modifier
        get() = contentBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier

    /** Sample surface for pages that own their own scroll container. */
    val pageBackdropModifier: Modifier
        get() = pageBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier
}

internal val LocalMiuixComponentScope = compositionLocalOf { MiuixComponentScope() }

/** Backdrop recorded by a progressively blurred OS4 top app bar, for nested icon buttons. */
internal val LocalMiuixTopBarBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Root Miuix surface for both UI styles.
 *
 * This host deliberately does not install a `Scaffold`: it only publishes the ambient scope and the
 * surface colour. Bars (top and bottom) belong to the screen that owns them, and stacking a
 * bar-less Scaffold in the middle of the tree only consumed insets without rendering anything.
 *
 * @param useOs4 Render the OS4 (glass) presentation when the platform supports render effects.
 * @param useBlur Enable the OS3 backdrop blur path.
 */
@Composable
fun MiuixComponentHost(
    useOs4: Boolean,
    useBlur: Boolean,
    modifier: Modifier = Modifier,
    popupInRootScaffold: Boolean = true,
    containerColor: Color = MiuixTheme.colorScheme.surface,
    content: @Composable (MiuixComponentScope) -> Unit,
) {
    // useOs4 already carries the platform guard from the theme layer; the effect check here is a
    // local defence so a mis-wired caller can never sample a surface it cannot draw.
    val useGlass = useOs4 && isRenderEffectSupported()
    val contentBackdrop = rememberMiuixLayerBackdrop(useGlass || useBlur)
    val pageBackdrop = rememberMiuixLayerBackdrop(useGlass)
    val scope = remember(useGlass, contentBackdrop, pageBackdrop, popupInRootScaffold) {
        MiuixComponentScope(useGlass, contentBackdrop, pageBackdrop, popupInRootScaffold)
    }

    CompositionLocalProvider(LocalMiuixComponentScope provides scope) {
        // A Box, not a Scaffold: this host only publishes the ambient scope. Wrapping the tree in a
        // Scaffold here added one more layer that consumed window insets and contributed no bars,
        // so its content padding was silently ignored while still shortening nothing — and it made
        // the page's own Scaffold the third one in the chain. The screen hosts provide the real
        // bars.
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(containerColor),
        ) {
            content(scope)
        }
    }
}

/**
 * Remember a backdrop that clears its sampled scene with the current surface colour.
 *
 * Without the initial `drawRect` the recorded layer stays transparent, and blurring it produces
 * alpha-blending artifacts over the window background.
 */
@Composable
private fun rememberMiuixLayerBackdrop(enabled: Boolean): LayerBackdrop? {
    if (!enabled || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/** The active component scope, for widgets that need the backdrop explicitly. */
@Composable
internal fun currentMiuixComponentScope(): MiuixComponentScope = LocalMiuixComponentScope.current
