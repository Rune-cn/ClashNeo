// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Segmented-list corner radii.
 *
 * MIUI/HyperOS lists round the outer corners of a group and keep the inner edges almost square, so
 * a run of rows reads as one card. [ConnectionRadius] is what makes adjacent rows look joined.
 */
val CornerRadius = 16.dp
val ConnectionRadius = 5.dp

val topShape = RoundedCornerShape(
    topStart = CornerRadius,
    topEnd = CornerRadius,
    bottomStart = ConnectionRadius,
    bottomEnd = ConnectionRadius,
)
val middleShape = RoundedCornerShape(ConnectionRadius)
val bottomShape = RoundedCornerShape(
    topStart = ConnectionRadius,
    topEnd = ConnectionRadius,
    bottomStart = CornerRadius,
    bottomEnd = CornerRadius,
)
val singleShape = RoundedCornerShape(CornerRadius)
