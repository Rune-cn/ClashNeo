// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.neo.clash.ui.widget.SettingsEntry
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * State the settings page renders.
 *
 * Only the switches that this screen owns live here; everything else is a navigation entry, so the
 * screen never has to know about preference storage internals.
 */
data class SettingsUiState(
    val isRunning: Boolean = false,
)

/**
 * Settings screen with the four MIUI-style sections.
 *
 * [onNavigate] receives a stable route key rather than a lambda per row, which keeps the call site
 * exhaustive when a section is added and lets the host decide how routes map to screens.
 */
@Composable
fun SettingsPage(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onNavigate: (SettingsRoute) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsSection(title = "Network") {
            SettingsEntry(
                title = "Network settings",
                summary = "TUN, DNS and routing",
                onClick = { onNavigate(SettingsRoute.Network) },
            )
            SettingsEntry(
                title = "Override",
                summary = "Port, interface and stack",
                onClick = { onNavigate(SettingsRoute.Override) },
            )
        }

        SettingsSection(title = "Behaviour") {
            SettingsEntry(
                title = "Access control",
                summary = "Per-app proxy rules",
                onClick = { onNavigate(SettingsRoute.AccessControl) },
            )
            SettingsEntry(
                title = "Meta features",
                onClick = { onNavigate(SettingsRoute.MetaFeatures) },
            )
        }

        SettingsSection(title = "Appearance") {
            SettingsEntry(
                title = "Theme",
                summary = "Colour, dark mode and glass",
                onClick = { onNavigate(SettingsRoute.Theme) },
            )
            SettingsEntry(
                title = "Language",
                onClick = { onNavigate(SettingsRoute.Language) },
            )
        }

        SettingsSection(title = "About") {
            SettingsEntry(
                title = "About ClashNeo",
                onClick = { onNavigate(SettingsRoute.About) },
            )
            SettingsEntry(
                title = "Open source licenses",
                onClick = { onNavigate(SettingsRoute.Licenses) },
            )
        }
    }
}

/**
 * Stable identifiers for the destinations the settings screen can open.
 *
 * Using an enum instead of separate lambdas keeps [SettingsPage] closed for modification: adding a
 * destination only requires a new constant and one branch in the host.
 */
enum class SettingsRoute {
    Network,
    Override,
    AccessControl,
    MetaFeatures,
    Theme,
    Language,
    About,
    Licenses,
}
