// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.appsettings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.design.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixDropdownPreference
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.MiuixSwitchWidget
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * Application-level preferences, the Compose counterpart of `AppSettingsDesign`.
 *
 * Two switches carry a side effect the XML screen handled by restarting every activity:
 * [hideFromRecents] and the dark-mode choice change how the task looks, and Android only applies
 * that on (re)creation. The page therefore reports them through [onRequireRecreate] instead of
 * pretending a plain state write is enough.
 */
@Immutable
data class AppSettingsUiState(
    val autoRestart: Boolean = true,
    val darkModeIndex: Int = 0,
    val hideAppIcon: Boolean = false,
    val hideFromRecents: Boolean = false,
    val dynamicNotification: Boolean = true,
    /** Tunnelling state; the traffic switch is meaningless while the service is not running. */
    val serviceRunning: Boolean = false,
)

/**
 * @param onRequireRecreate invoked when a change only takes effect after the UI is recreated.
 * @param onHideIconChange invoked because hiding the launcher icon is applied to the component
 *   state, not to a preference the UI can render.
 */
@Composable
fun AppSettingsPage(
    state: AppSettingsUiState,
    onStateChange: (AppSettingsUiState) -> Unit,
    darkModeLabels: List<String>,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onRequireRecreate: () -> Unit = {},
    onHideIconChange: (Boolean) -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.app),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            SettingsSection(title = stringResource(R.string.behavior)) {
                MiuixSwitchWidget(
                    title = stringResource(R.string.auto_restart),
                    description = stringResource(R.string.allow_clash_auto_restart),
                    checked = state.autoRestart,
                    onCheckedChange = { onStateChange(state.copy(autoRestart = it)) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.interface_)) {
                MiuixDropdownPreference(
                    title = stringResource(R.string.dark_mode),
                    items = darkModeLabels,
                    selectedIndex = state.darkModeIndex.coerceIn(
                        0,
                        (darkModeLabels.size - 1).coerceAtLeast(0),
                    ),
                    onSelectedIndexChange = { index ->
                        onStateChange(state.copy(darkModeIndex = index))
                        onRequireRecreate()
                    },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.hide_app_icon_title),
                    description = stringResource(R.string.hide_app_icon_desc),
                    checked = state.hideAppIcon,
                    onCheckedChange = { onHideIconChange(it) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.hide_from_recents_title),
                    description = stringResource(R.string.hide_from_recents_desc),
                    checked = state.hideFromRecents,
                    onCheckedChange = {
                        onStateChange(state.copy(hideFromRecents = it))
                        onRequireRecreate()
                    },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.service)) {
                MiuixSwitchWidget(
                    title = stringResource(R.string.show_traffic),
                    description = stringResource(R.string.show_traffic_summary),
                    checked = state.dynamicNotification,
                    // The core owns the notification while running; changing it mid-flight would
                    // race the service, so the row is only editable when stopped (as in the XML UI).
                    enabled = !state.serviceRunning,
                    onCheckedChange = { onStateChange(state.copy(dynamicNotification = it)) },
                )
            }
        }
    }
}
