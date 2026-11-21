// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.widget

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.R
import io.github.neo.clash.ui.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.theme.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack

/**
 * Standardised back button.
 *
 * Follows the active Miuix UI version automatically: OS4 renders a glass icon button that samples
 * the top app bar band, OS3 renders the classic icon button. Callers do not pass a style flag, so
 * both presentations stay in sync from one switch.
 */
@Composable
fun MiuixBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack,
    iconTint: Color = LocalContentColor.current,
    contentDescription: String = stringResource(id = R.string.back),
) {
    if (LocalMiuixComponentScope.current.useOs4 || AppTheme.useMiuixOs4) {
        MiuixTopBarIconButton(onClick = onClick, modifier = modifier) {
            // OS4 pairs the glass button with the glass icon set; the classic arrow keeps the OS3
            // glyph set so the two styles stay visually distinct.
            Icon(
                imageVector = MiuixIcons.Glass.ChevronBackward,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
            )
        }
    }
}
