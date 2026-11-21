// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.R
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * State the settings page renders.
 *
 * Only the values this screen owns live here; everything else is a navigation entry, so the screen
 * never has to know about preference storage internals.
 */
data class SettingsUiState(
    val isRunning: Boolean = false,
)

/**
 * Settings screen with the MIUI-style sections.
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
    MiuixPageScaffold(
        title = stringResource(R.string.tab_settings),
        modifier = modifier,
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            SettingsSection(title = stringResource(R.string.settings_section_network)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_network),
                    description = stringResource(R.string.settings_network_summary),
                    onClick = { onNavigate(SettingsRoute.Network) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_override),
                    description = stringResource(R.string.settings_override_summary),
                    onClick = { onNavigate(SettingsRoute.Override) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_section_behaviour)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_access_control),
                    description = stringResource(R.string.settings_access_control_summary),
                    onClick = { onNavigate(SettingsRoute.AccessControl) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_meta_features),
                    description = stringResource(R.string.settings_meta_features_summary),
                    onClick = { onNavigate(SettingsRoute.MetaFeatures) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_theme),
                    description = stringResource(R.string.settings_theme_summary),
                    onClick = { onNavigate(SettingsRoute.Theme) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_language),
                    description = stringResource(R.string.settings_language_summary),
                    onClick = { onNavigate(SettingsRoute.Language) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_about),
                    description = stringResource(R.string.settings_about_summary),
                    onClick = { onNavigate(SettingsRoute.About) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.settings_licenses),
                    description = stringResource(R.string.settings_licenses_summary),
                    onClick = { onNavigate(SettingsRoute.Licenses) },
                )
            }
        }

        item { Box(modifier = Modifier.padding(bottom = 8.dp)) }
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
