// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.override

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixDropdownPreference
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.MiuixSwitchWidget
import io.github.neo.clash.ui.widget.MiuixTextField
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * Port overrides, the Compose counterpart of `OverrideSettingsDesign`.
 *
 * Every value here defaults to "not overridden": an empty port string means the core's own
 * configuration wins. That is why ports are [String] rather than [Int] — an `Int` would have to
 * invent a sentinel, and `0` is a valid value the user might genuinely want to reject.
 */
@Immutable
data class OverrideSettingsUiState(
    val httpPort: String = "",
    val socksPort: String = "",
    val mixedPort: String = "",
    val redirectPort: String = "",
    val tproxyPort: String = "",
    val allowLan: Boolean = false,
    val allowIpv6: Boolean = false,
    val bindAddress: String = "*",
    /**
     * Comma-separated `user:password` entries.
     *
     * A [String], not a [Boolean]: the core models this as a credential list, so a switch had no
     * value it could write. Enabling it without credentials is not a state the core has.
     */
    val authentication: String = "",
    val externalController: String = "",
    val logLevelIndex: Int = 0,
    val modeIndex: Int = 0,
)

/**
 * @param logLevelIndex / [OverrideSettingsUiState.modeIndex] index into the option lists supplied by
 *   [logLevels] and [modes]; keeping them as indices avoids a second enum that would have to track
 *   the core's own vocabulary.
 */
@Composable
fun OverrideSettingsPage(
    state: OverrideSettingsUiState,
    onStateChange: (OverrideSettingsUiState) -> Unit,
    logLevels: List<String>,
    modes: List<String>,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.settings_override),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            SettingsSection(title = stringResource(R.string.override_section_ports)) {
                PortRow(
                    label = stringResource(R.string.override_http_port),
                    value = state.httpPort,
                    onChange = { onStateChange(state.copy(httpPort = it)) },
                )
                PortRow(
                    label = stringResource(R.string.override_socks_port),
                    value = state.socksPort,
                    onChange = { onStateChange(state.copy(socksPort = it)) },
                )
                PortRow(
                    label = stringResource(R.string.override_mixed_port),
                    value = state.mixedPort,
                    onChange = { onStateChange(state.copy(mixedPort = it)) },
                )
                PortRow(
                    label = stringResource(R.string.override_redirect_port),
                    value = state.redirectPort,
                    onChange = { onStateChange(state.copy(redirectPort = it)) },
                )
                PortRow(
                    label = stringResource(R.string.override_tproxy_port),
                    value = state.tproxyPort,
                    onChange = { onStateChange(state.copy(tproxyPort = it)) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.override_section_lan)) {
                MiuixSwitchWidget(
                    title = stringResource(R.string.override_allow_lan),
                    description = stringResource(R.string.override_allow_lan_summary),
                    checked = state.allowLan,
                    onCheckedChange = { onStateChange(state.copy(allowLan = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.override_ipv6),
                    description = stringResource(R.string.override_ipv6_summary),
                    checked = state.allowIpv6,
                    onCheckedChange = { onStateChange(state.copy(allowIpv6 = it)) },
                )
                PortRow(
                    label = stringResource(R.string.override_bind_address),
                    value = state.bindAddress,
                    onChange = { onStateChange(state.copy(bindAddress = it)) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.override_section_controller)) {
                PortRow(
                    label = stringResource(R.string.override_authentication),
                    value = state.authentication,
                    onChange = { onStateChange(state.copy(authentication = it)) },
                )
                PortRow(
                    label = stringResource(R.string.override_external_controller),
                    value = state.externalController,
                    onChange = { onStateChange(state.copy(externalController = it)) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.override_section_core)) {
                MiuixDropdownPreference(
                    title = stringResource(R.string.override_mode),
                    items = modes,
                    selectedIndex = state.modeIndex.coerceIn(0, (modes.size - 1).coerceAtLeast(0)),
                    onSelectedIndexChange = { onStateChange(state.copy(modeIndex = it)) },
                )
                MiuixDropdownPreference(
                    title = stringResource(R.string.override_log_level),
                    items = logLevels,
                    selectedIndex =
                        state.logLevelIndex.coerceIn(0, (logLevels.size - 1).coerceAtLeast(0)),
                    onSelectedIndexChange = { onStateChange(state.copy(logLevelIndex = it)) },
                )
            }
        }
    }
}

/** A labelled text field for one port/address value. */
@Composable
private fun PortRow(label: String, value: String, onChange: (String) -> Unit) {
    MiuixTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
