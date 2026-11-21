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
 * degrades to the classic OS3 presentation instead of drawing nothing. [darkTheme] is supplied by
 * the caller rather than defaulted here because the app's dark-mode preference is an enum
 * (`DarkMode.Auto` / `ForceLight` / `ForceDark`), not a boolean.
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
        // `MiuixTheme(colors)` takes the palette positionally; there is no `colorScheme` parameter,
        // and a named `colors =` argument does not resolve against the current overloads.
        MiuixTheme(
            colors = if (darkTheme) darkColorScheme() else lightColorScheme(),
        ) {
            content()
        }
    }
}

/**
 * Status palette for the control screen.
 *
 * Kept as plain colours rather than theme tokens because they intentionally bypass the dynamic
 * colour scheme: "running" (green), "starting" (amber) and "stopped" (red) have to stay
 * recognisable as tunnel states even when the user's wallpaper-derived palette is orange.
 */
internal object StatusColors {
    val runningContainerLight = Color(0xFFDDF7E3)
    val runningContainerDark = Color(0xFF16301F)
    val runningAccentLight = Color(0xFF2E7D32)
    val runningAccentDark = Color(0xFF7BD88F)

    val stoppedContainerLight = Color(0xFFFAEDED)
    val stoppedContainerDark = Color(0xFF351A1A)
    val stoppedAccentLight = Color(0xFFC62828)
    val stoppedAccentDark = Color(0xFFF08A8A)

    val busyContainerLight = Color(0xFFFDF3DC)
    val busyContainerDark = Color(0xFF35290F)
    val busyAccentLight = Color(0xFFB26A00)
    val busyAccentDark = Color(0xFFFFC46B)
}
