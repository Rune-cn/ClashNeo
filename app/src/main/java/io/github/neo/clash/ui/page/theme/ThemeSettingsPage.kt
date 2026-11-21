// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.theme

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.R
import io.github.neo.clash.ui.theme.isOs4PlatformSupported
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.MiuixSpinnerPreference
import io.github.neo.clash.ui.widget.MiuixSwitchWidget
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.SmallTitle

/**
 * Theme settings, ported from the reference app.
 *
 * The rows are grouped by what they affect rather than by implementation:
 *
 * - **Interface** — presentation family (OS3/OS4), blur, floating bar.
 * - **Colour** — custom palette, dynamic colour, palette style.
 * - **Navigation** — predictive-back animation and exit direction.
 *
 * Two rows are conditional on purpose. The OS4 switch is hidden when the platform cannot render
 * render effects, and the floating-bar switch is hidden while OS4 is active because OS4 forces the
 * floating bar on — showing a switch that does nothing is worse than not showing it.
 */
@Composable
fun ThemeSettingsPage(
    state: ThemeUiState,
    onStateChange: (ThemeUiState) -> Unit,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
) {
    val supportsOs4 = isOs4PlatformSupported

    MiuixPageScaffold(
        title = stringResource(R.string.settings_theme),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            SettingsSection(title = stringResource(R.string.theme_section_interface)) {
                ThemeModeRow(
                    current = state.themeMode,
                    onChange = { onStateChange(state.copy(themeMode = it)) },
                )

                if (supportsOs4) {
                    OsVersionRow(
                        useOs4 = state.useMiuixOs4,
                        onUseOs4Change = { onStateChange(state.copy(useMiuixOs4 = it)) },
                    )
                }

                if (!state.useMiuixOs4) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_floating_bar),
                        description = stringResource(R.string.theme_floating_bar_desc),
                        checked = state.useAppleFloatingBar,
                        onCheckedChange = {
                            onStateChange(state.copy(useAppleFloatingBar = it))
                        },
                    )
                }

                if (supportsOs4 && !state.useMiuixOs4) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_blur),
                        description = stringResource(R.string.theme_blur_desc),
                        checked = state.useBlur,
                        onCheckedChange = { onStateChange(state.copy(useBlur = it)) },
                    )
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.theme_section_colour)) {
                MiuixSwitchWidget(
                    title = stringResource(R.string.theme_custom_colors),
                    description = stringResource(R.string.theme_custom_colors_desc),
                    checked = state.useMiuixMonet,
                    onCheckedChange = { onStateChange(state.copy(useMiuixMonet = it)) },
                )

                // The dependent rows animate in and out so the section does not jump when the
                // parent switch flips; the timing matches the reference app.
                AnimatedVisibility(
                    visible = state.useMiuixMonet,
                    enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                        expandVertically(tween(400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(250, easing = FastOutSlowInEasing)) +
                        shrinkVertically(tween(350, easing = FastOutSlowInEasing)),
                ) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.theme_dynamic_color),
                        description = stringResource(R.string.theme_dynamic_color_desc),
                        checked = state.useDynamicColor,
                        onCheckedChange = { onStateChange(state.copy(useDynamicColor = it)) },
                    )
                }

                AnimatedVisibility(
                    visible = state.useMiuixMonet && !state.useDynamicColor,
                    enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                        expandVertically(tween(400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(250, easing = FastOutSlowInEasing)) +
                        shrinkVertically(tween(350, easing = FastOutSlowInEasing)),
                ) {
                    PaletteStyleRow(
                        current = state.paletteStyle,
                        onChange = { onStateChange(state.copy(paletteStyle = it)) },
                    )
                }
            }
        }

        // Predictive back needs the Android 14 progress API (34). Below that the whole section is
        // hidden rather than offering options the platform cannot honour.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            item {
                SettingsSection(title = stringResource(R.string.theme_section_navigation)) {
                    PredictiveBackAnimationRow(
                        current = state.predictiveBackAnimation,
                        onChange = { onStateChange(state.copy(predictiveBackAnimation = it)) },
                    )

                    // Only the Scale transition ever reads an exit direction: it is the one that
                    // animates the outgoing page along a chosen axis. AOSP/Miuix/Classic follow the
                    // gesture by construction and None has no transition at all, so showing the
                    // picker for them would be a control with no effect.
                    AnimatedVisibility(
                        visible = state.predictiveBackAnimation ==
                            PredictiveBackAnimation.SCALE,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        PredictiveBackDirectionRow(
                            current = state.predictiveBackExitDirection,
                            onChange = {
                                onStateChange(state.copy(predictiveBackExitDirection = it))
                            },
                        )
                    }
                }
            }
        }
    }
}

/* ---------- rows ---------- */

@Composable
private fun ThemeModeRow(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val items = ThemeMode.entries.map { mode ->
        DropdownItem(title = stringResource(mode.labelRes))
    }

    MiuixSpinnerPreference(
        title = stringResource(R.string.theme_mode),
        items = items,
        selectedIndex = ThemeMode.entries.indexOf(current).coerceAtLeast(0),
        onSelectedIndexChange = { index ->
            val next = ThemeMode.entries[index]
            if (next != current) onChange(next)
        },
    )
}

@Composable
private fun OsVersionRow(useOs4: Boolean, onUseOs4Change: (Boolean) -> Unit) {
    MiuixSpinnerPreference(
        title = stringResource(R.string.theme_os_version),
        items = listOf(
            DropdownItem(title = stringResource(R.string.theme_os3)),
            DropdownItem(title = stringResource(R.string.theme_os4)),
        ),
        selectedIndex = if (useOs4) 1 else 0,
        onSelectedIndexChange = { index ->
            val next = index == 1
            if (next != useOs4) onUseOs4Change(next)
        },
    )
}

@Composable
private fun PaletteStyleRow(current: PaletteStyle, onChange: (PaletteStyle) -> Unit) {
    val options = PaletteStyle.entries
    val items = options.map { DropdownItem(title = stringResource(it.labelRes)) }

    MiuixSpinnerPreference(
        title = stringResource(R.string.theme_palette_style),
        items = items,
        selectedIndex = options.indexOf(current).coerceAtLeast(0),
        onSelectedIndexChange = { index ->
            val next = options[index]
            if (next != current) onChange(next)
        },
    )
}

@Composable
private fun PredictiveBackAnimationRow(
    current: PredictiveBackAnimation,
    onChange: (PredictiveBackAnimation) -> Unit,
) {
    val options = PredictiveBackAnimation.entries
    val items = options.map { DropdownItem(title = stringResource(it.labelRes)) }

    MiuixSpinnerPreference(
        title = stringResource(R.string.theme_predictive_back_animation),
        items = items,
        selectedIndex = options.indexOf(current).coerceAtLeast(0),
        onSelectedIndexChange = { index ->
            val next = options[index]
            if (next != current) onChange(next)
        },
    )
}

@Composable
private fun PredictiveBackDirectionRow(
    current: PredictiveBackExitDirection,
    onChange: (PredictiveBackExitDirection) -> Unit,
) {
    val options = PredictiveBackExitDirection.entries
    val items = options.map { DropdownItem(title = stringResource(it.labelRes)) }

    MiuixSpinnerPreference(
        title = stringResource(R.string.theme_predictive_back_direction),
        items = items,
        selectedIndex = options.indexOf(current).coerceAtLeast(0),
        onSelectedIndexChange = { index ->
            val next = options[index]
            if (next != current) onChange(next)
        },
    )
}

/* ---------- label mapping ---------- */

/** Label resource for each theme mode; kept next to the enum's only consumer. */
private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
        ThemeMode.SYSTEM -> R.string.theme_mode_system
    }

private val PaletteStyle.labelRes: Int
    get() = when (this) {
        PaletteStyle.TONAL_SPOT -> R.string.theme_palette_tonal_spot
        PaletteStyle.VIBRANT -> R.string.theme_palette_vibrant
        PaletteStyle.EXPRESSIVE -> R.string.theme_palette_expressive
        PaletteStyle.NEUTRAL -> R.string.theme_palette_neutral
        PaletteStyle.MONOCHROME -> R.string.theme_palette_monochrome
        PaletteStyle.FIDELITY -> R.string.theme_palette_fidelity
        PaletteStyle.CONTENT -> R.string.theme_palette_content
        PaletteStyle.RAINBOW -> R.string.theme_palette_rainbow
        PaletteStyle.FRUIT_SALAD -> R.string.theme_palette_fruit_salad
    }

private val PredictiveBackAnimation.labelRes: Int
    get() = when (this) {
        PredictiveBackAnimation.NONE -> R.string.theme_back_none
        PredictiveBackAnimation.AOSP -> R.string.theme_back_aosp
        PredictiveBackAnimation.MIUIX -> R.string.theme_back_miuix
        PredictiveBackAnimation.SCALE -> R.string.theme_back_scale
        PredictiveBackAnimation.CLASSIC -> R.string.theme_back_classic
    }

private val PredictiveBackExitDirection.labelRes: Int
    get() = when (this) {
        PredictiveBackExitDirection.FOLLOW_GESTURE -> R.string.theme_back_follow_gesture
        PredictiveBackExitDirection.ALWAYS_RIGHT -> R.string.theme_back_always_right
        PredictiveBackExitDirection.ALWAYS_LEFT -> R.string.theme_back_always_left
    }
