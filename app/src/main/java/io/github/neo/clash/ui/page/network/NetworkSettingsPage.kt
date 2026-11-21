// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.network

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.MiuixSwitchWidget
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * Network configuration, the Compose counterpart of `NetworkSettingsDesign`.
 *
 * The VPN switch is the master control: every service option below it is only meaningful while
 * tunnelling is enabled, so the dependent rows are disabled (not hidden) when it is off. Disabling
 * rather than hiding keeps the list from reflowing on every toggle and preserves the user's
 * mental map of where each option lives.
 */
@Immutable
data class NetworkSettingsUiState(
    val enableVpn: Boolean = false,
    val bypassPrivateNetwork: Boolean = true,
    val dnsHijacking: Boolean = true,
    val allowBypass: Boolean = false,
    val allowIpv6: Boolean = true,
    val systemProxy: Boolean = false,
)

/**
 * @param onOpenAccessControl opens the per-app rule editor; it is a separate screen because the
 *   package list can be long enough to deserve its own scroll position.
 */
@Composable
fun NetworkSettingsPage(
    state: NetworkSettingsUiState,
    onStateChange: (NetworkSettingsUiState) -> Unit,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onOpenAccessControl: () -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.settings_network),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            SettingsSection(title = stringResource(R.string.network_section_vpn)) {
                MiuixSwitchWidget(
                    title = stringResource(R.string.network_route_system_traffic),
                    description = stringResource(R.string.network_routing_via_vpn),
                    checked = state.enableVpn,
                    onCheckedChange = { onStateChange(state.copy(enableVpn = it)) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.network_section_vpn_options)) {
                // `enabled = state.enableVpn` on every dependent row: tunnelling options that the
                // service would ignore are shown as unavailable rather than silently dropped.
                MiuixSwitchWidget(
                    title = stringResource(R.string.network_bypass_private),
                    description = stringResource(R.string.network_bypass_private_summary),
                    checked = state.bypassPrivateNetwork,
                    enabled = state.enableVpn,
                    onCheckedChange = {
                        onStateChange(state.copy(bypassPrivateNetwork = it))
                    },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.network_dns_hijacking),
                    description = stringResource(R.string.network_dns_hijacking_summary),
                    checked = state.dnsHijacking,
                    enabled = state.enableVpn,
                    onCheckedChange = { onStateChange(state.copy(dnsHijacking = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.network_allow_bypass),
                    description = stringResource(R.string.network_allow_bypass_summary),
                    checked = state.allowBypass,
                    enabled = state.enableVpn,
                    onCheckedChange = { onStateChange(state.copy(allowBypass = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.network_allow_ipv6),
                    description = stringResource(R.string.network_allow_ipv6_summary),
                    checked = state.allowIpv6,
                    enabled = state.enableVpn,
                    onCheckedChange = { onStateChange(state.copy(allowIpv6 = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.network_system_proxy),
                    description = stringResource(R.string.network_system_proxy_summary),
                    checked = state.systemProxy,
                    enabled = state.enableVpn,
                    onCheckedChange = { onStateChange(state.copy(systemProxy = it)) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.network_section_control)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_access_control),
                    description = stringResource(R.string.settings_access_control_summary),
                    onClick = onOpenAccessControl,
                )
            }
        }
    }
}
