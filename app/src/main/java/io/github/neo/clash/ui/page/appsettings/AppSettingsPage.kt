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
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.MiuixSwitchWidget
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * Application-level preferences, the Compose counterpart of `AppSettingsDesign`.
 *
 * Dark mode is deliberately absent: it now lives in Theme settings together with the palette, so
 * having a second control here meant two switches writing the same preference. [hideFromRecents]
 * still reports through [onRequireRecreate], because Android only applies that on (re)creation.
 */
@Immutable
data class AppSettingsUiState(
    val autoRestart: Boolean = true,
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
