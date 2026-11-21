// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.providers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.design.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One proxy provider row. */
@Immutable
data class ProviderUi(
    val name: String,
    val vehicleType: String,
    /** Human-readable age, or null when the provider carries no timestamp. */
    val elapsed: String?,
    val updating: Boolean = false,
    /** Inline providers are configuration, not remote content, so they cannot be refreshed. */
    val updatable: Boolean = true,
)

@Immutable
data class ProvidersUiState(
    val providers: List<ProviderUi> = emptyList(),
)

/**
 * Proxy providers, the Compose counterpart of `ProvidersDesign`.
 *
 * A row is disabled while it is updating: the XML adapter did the same, because a second update
 * request for the same provider races the first one against the same core endpoint.
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
    MiuixPageScaffold(
        title = stringResource(R.string.providers),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        if (state.providers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_providers),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        } else {
            item {
                SettingsSection(title = stringResource(R.string.providers)) {
                    val updatingLabel = stringResource(R.string.updating)
                    state.providers.forEach { provider ->
                        // Composed here rather than inside a builder: `stringResource` is
                        // @Composable and cannot run inside `buildString`.
                        val parts = buildList {
                            add(provider.vehicleType)
                            provider.elapsed?.let { add(it) }
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
                SettingsSection(title = stringResource(R.string.action)) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.update_all),
                        description = stringResource(R.string.update_all_summary),
                        onClick = onUpdateAll,
                    )
                }
            }
        }
    }
}
