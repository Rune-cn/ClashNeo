// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.store

import android.content.Context
import io.github.neo.clash.ui.page.theme.PaletteStyle
import io.github.neo.clash.ui.page.theme.PredictiveBackAnimation
import io.github.neo.clash.ui.page.theme.PredictiveBackExitDirection
import io.github.neo.clash.ui.page.theme.ThemeMode
import io.github.neo.clash.ui.page.theme.ThemeUiState

/**
 * Persists the Compose theme settings.
 *
 * Without this the theme screen was decorative across launches: `ThemeUiState` lived in
 * `remember { mutableStateOf(...) }`, so every cold start reset the palette, the OS version and the
 * predictive-back style back to defaults.
 *
 * Uses plain `SharedPreferences` rather than the `design` module's typed-store helpers: those live
 * in the legacy UI module, and importing them here would tie the new UI to the code that is being
 * removed.
 *
 * Enums are stored by name and resolved with a fallback, so removing a constant in a future version
 * degrades to the default instead of crashing on a stale value.
 */
class ThemeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ThemeUiState = ThemeUiState(
        themeMode = enum(prefs.getString(KEY_THEME_MODE, null), ThemeMode.SYSTEM),
        useMiuixOs4 = prefs.getBoolean(KEY_OS4, false),
        useAppleFloatingBar = prefs.getBoolean(KEY_FLOATING_BAR, false),
        useBlur = prefs.getBoolean(KEY_BLUR, true),
        useMiuixMonet = prefs.getBoolean(KEY_MONET, true),
        useDynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        paletteStyle = enum(prefs.getString(KEY_PALETTE, null), PaletteStyle.TONAL_SPOT),
        predictiveBackAnimation = enum(
            prefs.getString(KEY_BACK_ANIMATION, null),
            PredictiveBackAnimation.MIUIX,
        ),
        predictiveBackExitDirection = enum(
            prefs.getString(KEY_BACK_DIRECTION, null),
            PredictiveBackExitDirection.FOLLOW_GESTURE,
        ),
    )

    fun save(state: ThemeUiState) {
        prefs.edit()
            .putString(KEY_THEME_MODE, state.themeMode.name)
            .putBoolean(KEY_OS4, state.useMiuixOs4)
            .putBoolean(KEY_FLOATING_BAR, state.useAppleFloatingBar)
            .putBoolean(KEY_BLUR, state.useBlur)
            .putBoolean(KEY_MONET, state.useMiuixMonet)
            .putBoolean(KEY_DYNAMIC_COLOR, state.useDynamicColor)
            .putString(KEY_PALETTE, state.paletteStyle.name)
            .putString(KEY_BACK_ANIMATION, state.predictiveBackAnimation.name)
            .putString(KEY_BACK_DIRECTION, state.predictiveBackExitDirection.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enum(name: String?, fallback: T): T =
        name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: fallback

    private companion object {
        const val PREFERENCES_NAME = "theme"

        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_OS4 = "use_os4"
        const val KEY_FLOATING_BAR = "floating_bar"
        const val KEY_BLUR = "use_blur"
        const val KEY_MONET = "use_monet"
        const val KEY_DYNAMIC_COLOR = "use_dynamic_color"
        const val KEY_PALETTE = "palette_style"
        const val KEY_BACK_ANIMATION = "predictive_back_animation"
        const val KEY_BACK_DIRECTION = "predictive_back_exit_direction"
    }
}