// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.proxy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.R
import io.github.neo.clash.ui.ProxyGroupUiState
import io.github.neo.clash.ui.ProxyUiState
import io.github.neo.clash.ui.widget.MiuixPageScaffold
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
    outerPadding: PaddingValues = PaddingValues(),
    onSelect: (group: String, node: String) -> Unit,
) {
    when {
        state.error != null -> MessageState(
            title = stringResource(R.string.proxy_unavailable),
            body = state.error,
            modifier = modifier.fillMaxSize(),
        )

        state.isEmpty && state.loading -> MessageState(
            title = stringResource(R.string.proxy_loading),
            body = stringResource(R.string.proxy_loading_body),
            modifier = modifier.fillMaxSize(),
        )

        state.isEmpty -> MessageState(
            title = stringResource(R.string.proxy_empty),
            body = stringResource(R.string.proxy_empty_body),
            modifier = modifier.fillMaxSize(),
        )

        else -> MiuixPageScaffold(
            title = stringResource(R.string.tab_proxy),
            modifier = modifier,
            outerPadding = outerPadding,
            horizontalPadding = 12.dp,
            verticalPadding = 12.dp,
        ) {
            state.groups.forEach { group ->
                item(key = group.name) {
                    ProxyGroupSection(group = group, onSelect = onSelect)
                }
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

    Box(modifier = Modifier.padding(bottom = 4.dp)) {
        SettingsSection(title = group.name) {
            if (payload == null) {
                SettingsEntry(
                    title = stringResource(R.string.proxy_group_failed),
                    summary = group.error ?: stringResource(R.string.proxy_group_unknown_error),
                )
                return@SettingsSection
            }

            if (payload.proxies.isEmpty()) {
                SettingsEntry(title = stringResource(R.string.proxy_group_no_nodes))
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
                    trailing = {
                        // The tick scales in, so switching a node reads as a change rather than a
                        // redraw of the whole list.
                        AnimatedVisibility(
                            visible = selected,
                            enter = scaleIn(),
                            exit = scaleOut(),
                        ) {
                            SelectedMark()
                        }
                    },
                )
            }
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
