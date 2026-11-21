// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.store

import android.content.Context
import io.github.neo.clash.ui.page.metafeatures.MetaFeatureUiState

/**
 * Persists the kernel feature settings.
 *
 * `MetaFeatureUiState` had no backing store at all, so every value on that screen was lost the
 * moment you left it — including the port lists and the domain filters, which are long to retype.
 *
 * Saved as one `SharedPreferences` entry per field rather than a serialised blob: a blob breaks on
 * every field rename, while a missing key simply falls back to its default.
 */
class MetaFeatureStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): MetaFeatureUiState = MetaFeatureUiState(
        unifiedDelay = prefs.getBoolean(KEY_UNIFIED_DELAY, false),
        tcpConcurrent = prefs.getBoolean(KEY_TCP_CONCURRENT, false),
        findProcessMode = prefs.getInt(KEY_FIND_PROCESS_MODE, 0),
        sniffHttpPorts = prefs.getString(KEY_SNIFF_HTTP_PORTS, "").orEmpty(),
        sniffHttpOverrideDestination = prefs.getBoolean(KEY_SNIFF_HTTP_OVERRIDE, false),
        sniffTlsPorts = prefs.getString(KEY_SNIFF_TLS_PORTS, "").orEmpty(),
        sniffTlsOverrideDestination = prefs.getBoolean(KEY_SNIFF_TLS_OVERRIDE, false),
        sniffQuicPorts = prefs.getString(KEY_SNIFF_QUIC_PORTS, "").orEmpty(),
        sniffQuicOverrideDestination = prefs.getBoolean(KEY_SNIFF_QUIC_OVERRIDE, false),
        geodataMode = prefs.getInt(KEY_GEODATA_MODE, 0),
        geoxGeoip = prefs.getBoolean(KEY_GEOX_GEOIP, false),
        geoxMmdb = prefs.getBoolean(KEY_GEOX_MMDB, false),
        geoxGeosite = prefs.getBoolean(KEY_GEOX_GEOSITE, false),
        forceDnsMapping = prefs.getBoolean(KEY_FORCE_DNS_MAPPING, false),
        parsePureIp = prefs.getBoolean(KEY_PARSE_PURE_IP, false),
        overrideDestination = prefs.getBoolean(KEY_OVERRIDE_DESTINATION, false),
        forceDomain = prefs.getString(KEY_FORCE_DOMAIN, "").orEmpty(),
        skipDomain = prefs.getString(KEY_SKIP_DOMAIN, "").orEmpty(),
        skipSrcAddress = prefs.getString(KEY_SKIP_SRC_ADDRESS, "").orEmpty(),
        skipDstAddress = prefs.getString(KEY_SKIP_DST_ADDRESS, "").orEmpty(),
    )

    fun save(state: MetaFeatureUiState) {
        prefs.edit()
            .putBoolean(KEY_UNIFIED_DELAY, state.unifiedDelay)
            .putBoolean(KEY_TCP_CONCURRENT, state.tcpConcurrent)
            .putInt(KEY_FIND_PROCESS_MODE, state.findProcessMode)
            .putString(KEY_SNIFF_HTTP_PORTS, state.sniffHttpPorts)
            .putBoolean(KEY_SNIFF_HTTP_OVERRIDE, state.sniffHttpOverrideDestination)
            .putString(KEY_SNIFF_TLS_PORTS, state.sniffTlsPorts)
            .putBoolean(KEY_SNIFF_TLS_OVERRIDE, state.sniffTlsOverrideDestination)
            .putString(KEY_SNIFF_QUIC_PORTS, state.sniffQuicPorts)
            .putBoolean(KEY_SNIFF_QUIC_OVERRIDE, state.sniffQuicOverrideDestination)
            .putInt(KEY_GEODATA_MODE, state.geodataMode)
            .putBoolean(KEY_GEOX_GEOIP, state.geoxGeoip)
            .putBoolean(KEY_GEOX_MMDB, state.geoxMmdb)
            .putBoolean(KEY_GEOX_GEOSITE, state.geoxGeosite)
            .putBoolean(KEY_FORCE_DNS_MAPPING, state.forceDnsMapping)
            .putBoolean(KEY_PARSE_PURE_IP, state.parsePureIp)
            .putBoolean(KEY_OVERRIDE_DESTINATION, state.overrideDestination)
            .putString(KEY_FORCE_DOMAIN, state.forceDomain)
            .putString(KEY_SKIP_DOMAIN, state.skipDomain)
            .putString(KEY_SKIP_SRC_ADDRESS, state.skipSrcAddress)
            .putString(KEY_SKIP_DST_ADDRESS, state.skipDstAddress)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "meta_features"

        const val KEY_UNIFIED_DELAY = "unified_delay"
        const val KEY_TCP_CONCURRENT = "tcp_concurrent"
        const val KEY_FIND_PROCESS_MODE = "find_process_mode"
        const val KEY_SNIFF_HTTP_PORTS = "sniff_http_ports"
        const val KEY_SNIFF_HTTP_OVERRIDE = "sniff_http_override"
        const val KEY_SNIFF_TLS_PORTS = "sniff_tls_ports"
        const val KEY_SNIFF_TLS_OVERRIDE = "sniff_tls_override"
        const val KEY_SNIFF_QUIC_PORTS = "sniff_quic_ports"
        const val KEY_SNIFF_QUIC_OVERRIDE = "sniff_quic_override"
        const val KEY_GEODATA_MODE = "geodata_mode"
        const val KEY_GEOX_GEOIP = "geox_geoip"
        const val KEY_GEOX_MMDB = "geox_mmdb"
        const val KEY_GEOX_GEOSITE = "geox_geosite"
        const val KEY_FORCE_DNS_MAPPING = "force_dns_mapping"
        const val KEY_PARSE_PURE_IP = "parse_pure_ip"
        const val KEY_OVERRIDE_DESTINATION = "override_destination"
        const val KEY_FORCE_DOMAIN = "force_domain"
        const val KEY_SKIP_DOMAIN = "skip_domain"
        const val KEY_SKIP_SRC_ADDRESS = "skip_src_address"
        const val KEY_SKIP_DST_ADDRESS = "skip_dst_address"
    }
}