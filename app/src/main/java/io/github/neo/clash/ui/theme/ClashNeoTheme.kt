// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.neo.clash.ui.page.theme.PaletteStyle
import io.github.neo.clash.ui.page.theme.ThemeMode
import top.yukonga.miuix.kmp.material.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec as MiuixColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle as MiuixPaletteStyle

/**
 * Whether the OS4 glass presentation can actually be drawn on this device.
 *
 * This is the single source of truth for the platform half of the OS4 decision. Call sites combine
 * it with the user preference and never re-derive it: duplicating the check is what once let
 * different parts of the UI disagree about whether OS4 was active.
 */
val isOs4PlatformSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isRenderEffectSupported()

private val LocalIsDark = staticCompositionLocalOf { false }
private val LocalUseMiuixOs4 = staticCompositionLocalOf { false }

/**
 * Theme state shared by the Compose UI.
 *
 * The values are deliberately derived from one another so a nested widget can ask "is OS4 active?"
 * without the caller threading a `useOs4` parameter through every level.
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

/** Maps the settings-facing palette enum onto the Miuix controller's own enum. */
private fun PaletteStyle.toMiuix(): MiuixPaletteStyle = when (this) {
    PaletteStyle.TONAL_SPOT -> MiuixPaletteStyle.TonalSpot
    PaletteStyle.VIBRANT -> MiuixPaletteStyle.Vibrant
    PaletteStyle.EXPRESSIVE -> MiuixPaletteStyle.Expressive
    PaletteStyle.NEUTRAL -> MiuixPaletteStyle.Neutral
    PaletteStyle.MONOCHROME -> MiuixPaletteStyle.Monochrome
    PaletteStyle.FIDELITY -> MiuixPaletteStyle.Fidelity
    PaletteStyle.CONTENT -> MiuixPaletteStyle.Content
    PaletteStyle.RAINBOW -> MiuixPaletteStyle.Rainbow
    PaletteStyle.FRUIT_SALAD -> MiuixPaletteStyle.FruitSalad
}

/**
 * Root theme for the Compose surface.
 *
 * Colour follows the reference app's two-engine model, which is the part the previous
 * implementation got wrong:
 *
 * - **[useMiuixMonet] on** — the Material "monet" engine derives a full palette from a seed colour.
 *   [paletteStyle] selects the palette *family*, so the picker visibly changes the UI. When
 *   [useDynamicColor] is also on (API 31+), the seed comes from the system wallpaper instead of the
 *   fixed brand seed; the palette family still applies on top of it.
 * - **[useMiuixMonet] off** — a plain light/dark scheme with no derivation, so [paletteStyle] is
 *   irrelevant and the picker is hidden by the settings screen.
 *
 * The earlier version mapped [useDynamicColor] straight onto `MonetLight`/`MonetDark` and folded
 * the palette style into the same branch, which made the style picker a no-op whenever dynamic
 * colour was enabled and made dynamic colour itself resolve to the system accent regardless of the
 * chosen family.
 */
@Composable
fun ClashNeoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useOs4: Boolean = false,
    useMiuixMonet: Boolean = true,
    useDynamicColor: Boolean = true,
    paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    seedColor: Color = primaryLight,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val os4 = useOs4 && isOs4PlatformSupported

    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val systemSeed = systemAccentSeed()
    val keyColor = if (useDynamicColor && supportsDynamic) systemSeed else seedColor

    val controller = if (useMiuixMonet) {
        val mode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }
        remember(mode, keyColor, paletteStyle, darkTheme) {
            ThemeController(
                colorSchemeMode = mode,
                keyColor = keyColor,
                paletteStyle = paletteStyle.toMiuix(),
                colorSpec = MiuixColorSpec.Spec2025,
                isDark = darkTheme,
            )
        }
    } else {
        val mode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.System
            ThemeMode.LIGHT -> ColorSchemeMode.Light
            ThemeMode.DARK -> ColorSchemeMode.Dark
        }
        remember(mode, darkTheme) {
            ThemeController(
                colorSchemeMode = mode,
                isDark = darkTheme,
            )
        }
    }

    CompositionLocalProvider(
        LocalIsDark provides darkTheme,
        LocalUseMiuixOs4 provides os4,
    ) {
        MiuixTheme(controller = controller) {
            content()
        }
    }
}

/**
 * Seed colour taken from the platform's wallpaper palette.
 *
 * `android.R.color.system_accent1_500` is the same role the reference app uses; reading it through
 * the resources system is what lets the value track the wallpaper without requesting any permission.
 * Falls back to the brand seed before API 31, where the colour does not exist.
 */
@Composable
private fun systemAccentSeed(): Color {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return primaryLight
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        androidx.core.content.ContextCompat.getColor(context, android.R.color.system_accent1_500)
            .let { Color(it) }
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

/** Status-card colours for the home screen's simpler two-state variant. */
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
