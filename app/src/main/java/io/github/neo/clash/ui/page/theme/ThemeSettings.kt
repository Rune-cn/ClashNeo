// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.theme

import androidx.compose.runtime.Immutable

/**
 * Theme mode, mirroring the reference app's three-way choice.
 *
 * [SYSTEM] is the default: following the device is what most users expect, and an explicit choice
 * is the exception they opt into.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

/**
 * Miuix presentation style.
 *
 * OS4 is the glass family; it can only be drawn on API 33+ with render effects available, so the
 * picker is hidden on devices that cannot render it rather than offering a dead option.
 */
enum class MiuixOsVersion {
    OS3,
    OS4,
}

/**
 * Predictive-back transition style.
 *
 * These correspond to the transitions the reference navigation layer ships. Keeping them as an
 * enum (rather than free-form parameters) means the picker and the navigator cannot drift.
 */
enum class PredictiveBackAnimation {
    NONE,
    AOSP,
    MIUIX,
    SCALE,
    CLASSIC,
}

/** Direction the outgoing page leaves in during a predictive-back gesture. */
enum class PredictiveBackExitDirection {
    FOLLOW_GESTURE,
    ALWAYS_RIGHT,
    ALWAYS_LEFT,
}

/** Material palette style used to derive the colour scheme from the seed colour. */
enum class PaletteStyle(val key: String) {
    TONAL_SPOT("tonal_spot"),
    VIBRANT("vibrant"),
    EXPRESSIVE("expressive"),
    NEUTRAL("neutral"),
    MONOCHROME("monochrome"),
    FIDELITY("fidelity"),
    CONTENT("content"),
    RAINBOW("rainbow"),
    FRUIT_SALAD("fruit_salad"),
}

/**
 * Complete theme configuration.
 *
 * A single immutable value (rather than a bag of independent `mutableStateOf` fields) so the screen
 * can diff one object, and so a later move to persisted storage only has to serialise this.
 */
@Immutable
data class ThemeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useMiuixOs4: Boolean = false,
    val useAppleFloatingBar: Boolean = false,
    val useBlur: Boolean = true,
    val useMiuixMonet: Boolean = true,
    val useDynamicColor: Boolean = true,
    val paletteStyle: PaletteStyle = PaletteStyle.TONAL_SPOT,
    val predictiveBackAnimation: PredictiveBackAnimation = PredictiveBackAnimation.MIUIX,
    val predictiveBackExitDirection: PredictiveBackExitDirection =
        PredictiveBackExitDirection.FOLLOW_GESTURE,
)
