// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * Whether the OS4 glass presentation can actually be drawn on this device.
 *
 * This is the single source of truth for the platform half of the OS4 decision. Call sites
 * combine it with the user preference and never re-derive it: duplicating the check is what once
 * let different parts of the upstream UI disagree about whether OS4 was active.
 */
val isOs4PlatformSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isRenderEffectSupported()

private val LocalIsDark = staticCompositionLocalOf { false }
private val LocalUseMiuixOs4 = staticCompositionLocalOf { false }

/**
 * Theme state shared by the Compose UI.
 *
 * The values are deliberately derived from one another so a nested widget can ask
 * "is OS4 active?" without the caller threading a `useOs4` parameter through every level.
 */
object AppTheme {
    /** Whether the Miuix UI family is currently rendering its OS4 (glass) presentation. */
    val useMiuixOs4: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalUseMiuixOs4.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDark.current
}

/**
 * Root theme for the Compose surface.
 *
 * [useOs4] is honoured only when the platform can render glass effects, so an unsupported device
 * degrades to the classic OS3 presentation instead of drawing nothing.
 */
@Composable
fun ClashNeoTheme(
    useOs4: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val os4 = useOs4 && isOs4PlatformSupported

    CompositionLocalProvider(
        LocalIsDark provides darkTheme,
        LocalUseMiuixOs4 provides os4,
    ) {
        // `MiuixTheme(colors)` takes the palette positionally; there is no `colorScheme` parameter.
        MiuixTheme(
            colors = if (darkTheme) darkColorScheme() else lightColorScheme(),
        ) {
            content()
        }
    }
}

/**
 * Status-card colours for the home page.
 *
 * Kept as plain functions rather than theme tokens because they intentionally bypass the dynamic
 * colour scheme: the green/red pair has to stay recognisable as "healthy" / "abnormal" even when
 * the user's wallpaper-derived palette is orange.
 */
internal object StatusCardColors {
    val runningLight = Color(0xFFDFFAE4)
    val runningDark = Color(0xFF1A3825)
    val abnormalLight = Color(0xFFFAEEEE)
    val abnormalDark = Color(0xFF381A1A)

    val runningAccentLight = Color(0xFF2E7D32)
    val runningAccentDark = Color(0xFF7BD88F)
    val abnormalAccentLight = Color(0xFFC62828)
    val abnormalAccentDark = Color(0xFFF08A8A)
}
