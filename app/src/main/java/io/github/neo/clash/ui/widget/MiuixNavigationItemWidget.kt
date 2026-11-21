// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.widget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * A settings row that navigates to a secondary page.
 *
 * Built on `ArrowPreference` rather than a hand-rolled row so the trailing chevron, the press
 * ripple and the row height all come from Miuix and stay consistent with the rest of the system
 * settings look.
 *
 * @param title The main title text of the row.
 * @param description Supporting text shown under the title.
 * @param onClick Invoked when the row is tapped.
 */
@Composable
fun MiuixNavigationItemWidget(
    title: String,
    description: String,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        summary = description,
        insideMargin = insideMargin,
        onClick = onClick,
    )
}
