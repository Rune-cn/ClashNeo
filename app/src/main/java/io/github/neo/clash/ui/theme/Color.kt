// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colours that intentionally sit outside the dynamic palette.
 *
 * The status card's green/red pair has to stay recognisable as "running" / "stopped" even when the
 * user's wallpaper-derived palette is orange, and the sheet colours anchor the OS3 presentation
 * before a palette is available.
 */
val primaryLight = Color(0xFF4A672D)
val primaryDark = Color(0xFFAFD18C)

val sheetColorLight = Color(0xFFF7F7F7)
val sheetColorDark = Color(0xFF242424)
val sheetCardColorDark = Color(0xFF434343)

val statusCardRunning = Color(0xFF36D167)
val statusCardAbnormal = Color(0xFFD13636)
