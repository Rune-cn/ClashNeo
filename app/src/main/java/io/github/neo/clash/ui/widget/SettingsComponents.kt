// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A titled group of settings rows, matching MIUI/HyperOS sectioning.
 *
 * Rows are supplied as a slot so callers compose the exact mix of switches and navigation entries
 * they need; this keeps the container free of every possible row variant.
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * A settings row that navigates somewhere.
 *
 * [summary] is optional detail text under the title; [trailing] renders the end affordance
 * (usually a chevron or a value).
 */
@Composable
fun SettingsEntry(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * A settings row backed by a boolean.
 *
 * [onCheckedChange] is only invoked for user interaction, never during composition, so callers can
 * pass a plain state writer without guarding against feedback loops.
 */
@Composable
fun SettingsSwitchEntry(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SettingsEntry(
        title = title,
        summary = summary,
        enabled = enabled,
        modifier = modifier.clickable(enabled = enabled) { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
                enabled = enabled,
            )
        },
    )
}
