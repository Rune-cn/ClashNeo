// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.metafeatures

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.design.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixDropdownPreference
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.MiuixSwitchWidget
import io.github.neo.clash.ui.widget.MiuixTextField
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * Kernel feature overrides, the Compose counterpart of `MetaFeatureSettingsDesign`.
 *
 * The XML screen had 27 rows spanning four concerns: connection behaviour, sniffing, GEO data
 * sources and the rule-matching filters. They are grouped the same way here, because the settings
 * only make sense in that structure — a flat list of 27 switches is not navigable.
 *
 * Port lists are [String] (comma-separated) rather than `List<Int>`: the core accepts a range
 * expression and the XML screen passed the raw text through, so parsing here would only lose
 * syntax the core understands.
 */
@Immutable
data class MetaFeatureUiState(
    // Connection
    val unifiedDelay: Boolean = false,
    val tcpConcurrent: Boolean = false,
    val findProcessMode: Int = 0,
    // Sniffing
    val sniffHttpPorts: String = "",
    val sniffHttpOverrideDestination: Boolean = false,
    val sniffTlsPorts: String = "",
    val sniffTlsOverrideDestination: Boolean = false,
    val sniffQuicPorts: String = "",
    val sniffQuicOverrideDestination: Boolean = false,
    // Geo
    val geodataMode: Int = 0,
    val geoxGeoip: Boolean = false,
    val geoxMmdb: Boolean = false,
    val geoxGeosite: Boolean = false,
    // Matching
    val forceDnsMapping: Boolean = false,
    val parsePureIp: Boolean = false,
    val overrideDestination: Boolean = false,
    val forceDomain: String = "",
    val skipDomain: String = "",
    val skipSrcAddress: String = "",
    val skipDstAddress: String = "",
)

/**
 * @param findProcessModes / [geodataModeLabels] index into lists the host supplies, because the
 *   core owns those vocabularies and duplicating them here would silently desync.
 * @param onImportFile asks the host for a file; the four import rows share one callback and the
 *   caller distinguishes them by [GeoFileKind].
 */
@Composable
fun MetaFeatureSettingsPage(
    state: MetaFeatureUiState,
    onStateChange: (MetaFeatureUiState) -> Unit,
    findProcessModes: List<String>,
    geodataModeLabels: List<String>,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onImportFile: (GeoFileKind) -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.meta_features),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            SettingsSection(title = stringResource(R.string.section_connection)) {
                MiuixSwitchWidget(
                    title = stringResource(R.string.unified_delay),
                    checked = state.unifiedDelay,
                    onCheckedChange = { onStateChange(state.copy(unifiedDelay = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.tcp_concurrent),
                    checked = state.tcpConcurrent,
                    onCheckedChange = { onStateChange(state.copy(tcpConcurrent = it)) },
                )
                MiuixDropdownPreference(
                    title = stringResource(R.string.find_process_mode),
                    items = findProcessModes,
                    selectedIndex = state.findProcessMode.coerceIn(
                        0,
                        (findProcessModes.size - 1).coerceAtLeast(0),
                    ),
                    onSelectedIndexChange = { onStateChange(state.copy(findProcessMode = it)) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.section_sniffing)) {
                SniffRow(
                    portsLabel = stringResource(R.string.sniff_http_ports),
                    overrideLabel = stringResource(R.string.sniff_http_override_destination),
                    ports = state.sniffHttpPorts,
                    override = state.sniffHttpOverrideDestination,
                    onPortsChange = { onStateChange(state.copy(sniffHttpPorts = it)) },
                    onOverrideChange = {
                        onStateChange(state.copy(sniffHttpOverrideDestination = it))
                    },
                )
                SniffRow(
                    portsLabel = stringResource(R.string.sniff_tls_ports),
                    overrideLabel = stringResource(R.string.sniff_tls_override_destination),
                    ports = state.sniffTlsPorts,
                    override = state.sniffTlsOverrideDestination,
                    onPortsChange = { onStateChange(state.copy(sniffTlsPorts = it)) },
                    onOverrideChange = {
                        onStateChange(state.copy(sniffTlsOverrideDestination = it))
                    },
                )
                SniffRow(
                    portsLabel = stringResource(R.string.sniff_quic_ports),
                    overrideLabel = stringResource(R.string.sniff_quic_override_destination),
                    ports = state.sniffQuicPorts,
                    override = state.sniffQuicOverrideDestination,
                    onPortsChange = { onStateChange(state.copy(sniffQuicPorts = it)) },
                    onOverrideChange = {
                        onStateChange(state.copy(sniffQuicOverrideDestination = it))
                    },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.section_geodata)) {
                MiuixDropdownPreference(
                    title = stringResource(R.string.geodata_mode),
                    items = geodataModeLabels,
                    selectedIndex = state.geodataMode.coerceIn(
                        0,
                        (geodataModeLabels.size - 1).coerceAtLeast(0),
                    ),
                    onSelectedIndexChange = { onStateChange(state.copy(geodataMode = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.geox_geoip),
                    checked = state.geoxGeoip,
                    onCheckedChange = { onStateChange(state.copy(geoxGeoip = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.geox_mmdb),
                    checked = state.geoxMmdb,
                    onCheckedChange = { onStateChange(state.copy(geoxMmdb = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.geox_geosite),
                    checked = state.geoxGeosite,
                    onCheckedChange = { onStateChange(state.copy(geoxGeosite = it)) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.import_geoip_file),
                    description = stringResource(R.string.import_file_summary),
                    onClick = { onImportFile(GeoFileKind.GEOIP) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.import_geosite_file),
                    description = stringResource(R.string.import_file_summary),
                    onClick = { onImportFile(GeoFileKind.GEOSITE) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.import_country_file),
                    description = stringResource(R.string.import_file_summary),
                    onClick = { onImportFile(GeoFileKind.COUNTRY) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.import_asn_file),
                    description = stringResource(R.string.import_file_summary),
                    onClick = { onImportFile(GeoFileKind.ASN) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.section_matching)) {
                MiuixSwitchWidget(
                    title = stringResource(R.string.force_dns_mapping),
                    checked = state.forceDnsMapping,
                    onCheckedChange = { onStateChange(state.copy(forceDnsMapping = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.parse_pure_ip),
                    checked = state.parsePureIp,
                    onCheckedChange = { onStateChange(state.copy(parsePureIp = it)) },
                )
                MiuixSwitchWidget(
                    title = stringResource(R.string.override_destination),
                    checked = state.overrideDestination,
                    onCheckedChange = { onStateChange(state.copy(overrideDestination = it)) },
                )
                MiuixTextField(
                    value = state.forceDomain,
                    onValueChange = { onStateChange(state.copy(forceDomain = it)) },
                    label = stringResource(R.string.force_domain),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                MiuixTextField(
                    value = state.skipDomain,
                    onValueChange = { onStateChange(state.copy(skipDomain = it)) },
                    label = stringResource(R.string.skip_domain),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                MiuixTextField(
                    value = state.skipSrcAddress,
                    onValueChange = { onStateChange(state.copy(skipSrcAddress = it)) },
                    label = stringResource(R.string.skip_src_address),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                MiuixTextField(
                    value = state.skipDstAddress,
                    onValueChange = { onStateChange(state.copy(skipDstAddress = it)) },
                    label = stringResource(R.string.skip_dst_address),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** One sniffing group: the port list plus its override switch. */
@Composable
private fun SniffRow(
    portsLabel: String,
    overrideLabel: String,
    ports: String,
    override: Boolean,
    onPortsChange: (String) -> Unit,
    onOverrideChange: (Boolean) -> Unit,
) {
    MiuixTextField(
        value = ports,
        onValueChange = onPortsChange,
        label = portsLabel,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
    MiuixSwitchWidget(
        title = overrideLabel,
        checked = override,
        onCheckedChange = onOverrideChange,
    )
}

/** Which GEO asset an import row refers to. */
enum class GeoFileKind {
    GEOIP,
    GEOSITE,
    COUNTRY,
    ASN,
}
