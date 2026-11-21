// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixCheckboxWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.MiuixTextField
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One selectable app row. */
@Immutable
data class AccessApp(
    val packageName: String,
    val label: String,
    val system: Boolean = false,
)

/** Mode chosen for the list; mirrors the service's three-way access control. */
enum class AccessMode {
    ACCEPT,
    REJECT,
}

@Immutable
data class AccessControlUiState(
    val apps: List<AccessApp> = emptyList(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val mode: AccessMode = AccessMode.ACCEPT,
    val loading: Boolean = false,
)

/**
 * Per-app proxy rules, the Compose counterpart of `AccessControlDesign`.
 *
 * The list is filtered in the composable rather than by the caller: the query is transient UI
 * state, and re-querying the package manager on every keystroke would be both slower and racy
 * against the load that is still in flight on a cold start.
 */
@Composable
fun AccessControlPage(
    state: AccessControlUiState,
    onStateChange: (AccessControlUiState) -> Unit,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
) {
    val visible = remember(state.apps, state.query) {
        val needle = state.query.trim()
        if (needle.isEmpty()) {
            state.apps
        } else {
            state.apps.filter {
                it.label.contains(needle, ignoreCase = true) ||
                    it.packageName.contains(needle, ignoreCase = true)
            }
        }
    }

    MiuixPageScaffold(
        title = stringResource(R.string.settings_access_control),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            MiuixTextField(
                value = state.query,
                onValueChange = { onStateChange(state.copy(query = it)) },
                label = stringResource(R.string.access_search_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 4.dp),
            )
        }

        item {
            SettingsSection(title = stringResource(R.string.access_section_selection)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BulkAction(
                        label = stringResource(R.string.access_select_all),
                        onClick = {
                            onStateChange(
                                state.copy(selected = state.selected + visible.map { it.packageName }),
                            )
                        },
                    )
                    BulkAction(
                        label = stringResource(R.string.access_select_none),
                        onClick = { onStateChange(state.copy(selected = emptySet())) },
                    )
                    BulkAction(
                        label = stringResource(R.string.access_select_invert),
                        onClick = {
                            val visibleNames = visible.map { it.packageName }.toSet()
                            onStateChange(
                                state.copy(
                                    selected = state.selected - visibleNames +
                                        (visibleNames - state.selected),
                                ),
                            )
                        },
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (state.loading) R.string.access_loading else R.string.access_empty,
                        ),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        } else {
            items(visible, key = { it.packageName }) { app ->
                val checked = app.packageName in state.selected
                MiuixCheckboxWidget(
                    title = app.label,
                    description = app.packageName,
                    checked = checked,
                    onCheckedChange = { on ->
                        onStateChange(
                            state.copy(
                                selected = if (on) {
                                    state.selected + app.packageName
                                } else {
                                    state.selected - app.packageName
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

/** Tappable label used for the bulk-selection actions. */
@Composable
private fun BulkAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
