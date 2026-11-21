// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.proxy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.ui.ProxyGroupUiState
import io.github.neo.clash.ui.ProxyUiState
import io.github.neo.clash.ui.widget.SettingsEntry
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The 代理 (Proxy) tab.
 *
 * Renders the proxy groups exactly as the core reported them. Two deliberate choices:
 *
 * - Only `Selector` groups are interactive. The core rejects `patchSelector` on the other adapter
 *   types, so rendering them as buttons would produce taps that silently fail.
 * - An empty list is only claimed to be empty once loading has finished; otherwise a cold start
 *   would flash "no proxies" for the second the query takes.
 */
@Composable
fun ProxyPage(
    state: ProxyUiState,
    modifier: Modifier = Modifier,
    onSelect: (group: String, node: String) -> Unit,
) {
    val scrollModifier = modifier.fillMaxSize()

    when {
        state.error != null -> MessageState(
            title = "Proxies unavailable",
            body = state.error,
            modifier = scrollModifier,
        )

        state.isEmpty && state.loading -> MessageState(
            title = "Loading proxies…",
            body = "Waiting for the core to report its groups.",
            modifier = scrollModifier,
        )

        state.isEmpty -> MessageState(
            title = "No proxies",
            body = "Start the tunnel to load proxy groups.",
            modifier = scrollModifier,
        )

        else -> LazyColumn(
            modifier = scrollModifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.groups, key = { it.name }) { group ->
                ProxyGroupSection(group = group, onSelect = onSelect)
            }
        }
    }
}

/**
 * One group: its name, and every node with the active one marked.
 *
 * A group that failed to load shows its error in place of the nodes rather than disappearing, so a
 * single broken selector does not make the whole screen look like it has fewer groups than it does.
 */
@Composable
private fun ProxyGroupSection(
    group: ProxyGroupUiState,
    onSelect: (group: String, node: String) -> Unit,
) {
    val payload = group.group

    SettingsSection(title = group.name) {
        if (payload == null) {
            SettingsEntry(
                title = "Failed to load",
                summary = group.error ?: "Unknown error",
            )
            return@SettingsSection
        }

        if (payload.proxies.isEmpty()) {
            SettingsEntry(title = "No nodes")
            return@SettingsSection
        }

        payload.proxies.forEach { proxy ->
            val selected = proxy.name == payload.now
            SettingsEntry(
                title = proxy.title.ifBlank { proxy.name },
                summary = proxy.subtitle.takeIf { it.isNotBlank() },
                enabled = group.selectable,
                onClick = if (group.selectable) {
                    { onSelect(group.name, proxy.name) }
                } else {
                    null
                },
                trailing = if (selected) {
                    { SelectedMark() }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun SelectedMark() {
    Icon(
        imageVector = Icons.Rounded.Check,
        tint = MiuixTheme.colorScheme.primary,
        contentDescription = null,
    )
}

/**
 * Full-screen empty / error / loading placeholder.
 *
 * Kept local to this file: each tab words its own empty state, and a shared component would invite
 * callers to pass strings that no longer match the situation.
 */
@Composable
private fun MessageState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
