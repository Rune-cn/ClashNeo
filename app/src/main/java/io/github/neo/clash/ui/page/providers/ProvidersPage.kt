// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.R
import io.github.neo.clash.core.model.Provider
import io.github.neo.clash.ui.util.formatElapsed
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.SettingsSection
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * One proxy or rule provider, exactly as `IClashManager.queryProviders` returned it.
 *
 * The core's own enums are kept here rather than pre-formatted labels: the row text is built by the
 * page, so it follows a language change instead of freezing the wording that was current when the
 * list was fetched.
 */
@Immutable
data class ProviderUi(
    val name: String,
    val type: Provider.Type,
    val vehicleType: Provider.VehicleType,
    /** When the core last fetched this provider, in milliseconds; 0 means "never". */
    val updatedAt: Long,
    val updating: Boolean = false,
) {
    /**
     * Inline providers are configuration, not downloaded content, so there is nothing to refresh.
     * The core would reject the request anyway.
     */
    val updatable: Boolean
        get() = vehicleType != Provider.VehicleType.Inline
}

/**
 * Everything the 提供者 (Providers) screen renders.
 *
 * [coreRunning] is part of the state, not inferred by the page, because "the list is empty" has two
 * very different causes: the core is not running (so nobody could answer), or the active
 * configuration really declares no providers. The page words the empty state from this flag rather
 * than showing one message that would be misleading half of the time.
 */
@Immutable
data class ProvidersUiState(
    val providers: List<ProviderUi> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val coreRunning: Boolean = false,
)

/**
 * Proxy providers, the Compose counterpart of `ProvidersDesign`.
 *
 * A row is disabled while it is updating: the XML adapter did the same, because a second update
 * request for the same provider races the first one against the same core endpoint.
 *
 * Provider ages are relative ("5 min"), so they need a clock of their own; the legacy adapter drove
 * that from a one-minute ticker and this page does the same. Without it a row would still claim
 * "5 min" an hour after the screen was opened.
 */
@Composable
fun ProvidersPage(
    state: ProvidersUiState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onUpdate: (ProviderUi) -> Unit = {},
    onUpdateAll: () -> Unit = {},
) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(PROVIDER_AGE_TICK)
            value = System.currentTimeMillis()
        }
    }

    val pageTitle = stringResource(R.string.settings_providers)

    when {
        // Every branch renders through the scaffold so the title bar and the back affordance are
        // always present: an error or an empty list is still this screen, not a different one.
        state.error != null -> MessageScreen(
            pageTitle = pageTitle,
            title = stringResource(R.string.providers_error_title),
            body = state.error,
            modifier = modifier,
            outerPadding = outerPadding,
            onBack = onBack,
        )

        state.providers.isEmpty() && state.loading -> MessageScreen(
            pageTitle = pageTitle,
            title = stringResource(R.string.providers_loading_title),
            body = stringResource(R.string.providers_loading_body),
            modifier = modifier,
            outerPadding = outerPadding,
            onBack = onBack,
        )

        state.providers.isEmpty() -> MessageScreen(
            pageTitle = pageTitle,
            title = stringResource(R.string.providers_empty_title),
            body = stringResource(
                if (state.coreRunning) {
                    R.string.providers_empty_body_none
                } else {
                    R.string.providers_empty_body_no_core
                },
            ),
            modifier = modifier,
            outerPadding = outerPadding,
            onBack = onBack,
        )

        else -> MiuixPageScaffold(
            title = pageTitle,
            modifier = modifier,
            outerPadding = outerPadding,
            navigationIcon = { MiuixBackButton(onClick = onBack) },
            horizontalPadding = 12.dp,
            verticalPadding = 12.dp,
        ) {
            item {
                SettingsSection(title = pageTitle) {
                    val updatingLabel = stringResource(R.string.providers_updating)
                    val resources = LocalResources.current

                    state.providers.forEach { provider ->
                        // Composed here rather than inside a builder: `stringResource` is
                        // @Composable and cannot run inside `buildString`.
                        val parts = buildList {
                            add(providerKindLabel(provider.type, provider.vehicleType))
                            // Inline providers carry no fetch timestamp, so an age would be a lie;
                            // the XML adapter hid it for the same reason.
                            if (provider.updatable) {
                                formatElapsed(provider.updatedAt, resources, now)?.let { add(it) }
                            }
                            if (provider.updating) add(updatingLabel)
                        }
                        MiuixNavigationItemWidget(
                            title = provider.name,
                            description = parts.joinToString(" · "),
                            onClick = {
                                if (provider.updatable && !provider.updating) onUpdate(provider)
                            },
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.providers_actions)) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.providers_update_all),
                        description = stringResource(R.string.providers_update_all_summary),
                        onClick = onUpdateAll,
                    )
                }
            }
        }
    }
}

/** "Proxy(HTTP)": the adapter type, then the vehicle that stores it. */
@Composable
private fun providerKindLabel(
    type: Provider.Type,
    vehicleType: Provider.VehicleType,
): String {
    val typeLabel = stringResource(
        when (type) {
            Provider.Type.Proxy -> R.string.provider_type_proxy
            Provider.Type.Rule -> R.string.provider_type_rule
        },
    )
    val vehicleLabel = stringResource(
        when (vehicleType) {
            Provider.VehicleType.HTTP -> R.string.provider_vehicle_http
            Provider.VehicleType.File -> R.string.provider_vehicle_file
            Provider.VehicleType.Inline -> R.string.provider_vehicle_inline
            Provider.VehicleType.Compatible -> R.string.provider_vehicle_compatible
        },
    )

    return stringResource(R.string.providers_kind, typeLabel, vehicleLabel)
}

/**
 * Full-screen empty / error / loading placeholder.
 *
 * Kept local to this file for the same reason `ProxyPage` keeps its own: each screen words its
 * empty state, and a shared component would invite callers to pass strings that no longer match the
 * situation.
 */
@Composable
private fun MessageScreen(
    pageTitle: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
) {
    MiuixPageScaffold(
        title = pageTitle,
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
    }
}

/** How often the relative provider ages are re-read: the legacy adapter ticked once a minute. */
private const val PROVIDER_AGE_TICK = 60_000L