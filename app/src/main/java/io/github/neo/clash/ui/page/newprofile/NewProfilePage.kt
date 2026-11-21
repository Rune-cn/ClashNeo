// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.newprofile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.design.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * How the user wants to supply a new profile.
 *
 * Modelled as an enum rather than a list of lambdas so the host's `when` stays exhaustive: adding a
 * source forces a decision at the call site instead of silently rendering a dead row.
 */
enum class ProfileSource {
    FILE,
    URL,
    QR,
}

/**
 * "Add profile" chooser, the Compose counterpart of `NewProfileDesign`.
 *
 * The XML screen also listed installed apps that can hand over a config through their own intent
 * (`ProfileProvider.External`). Those are third-party entries and cannot be enumerated here, so the
 * host appends them via [extraSources]; this page only owns the three built-in ones.
 */
@Immutable
data class NewProfileUiState(
    val extraSources: List<ExternalSource> = emptyList(),
)

/** A third-party app that can supply a profile. */
@Immutable
data class ExternalSource(
    val name: String,
    val summary: String,
    val key: String,
)

@Composable
fun NewProfilePage(
    state: NewProfileUiState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onPick: (ProfileSource) -> Unit = {},
    onPickExternal: (ExternalSource) -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.create_profile),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            SettingsSection(title = stringResource(R.string.create_profile)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.file),
                    description = stringResource(R.string.import_from_file),
                    onClick = { onPick(ProfileSource.FILE) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.url),
                    description = stringResource(R.string.import_from_url),
                    onClick = { onPick(ProfileSource.URL) },
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.qr),
                    description = stringResource(R.string.import_from_qr),
                    onClick = { onPick(ProfileSource.QR) },
                )
            }
        }

        if (state.extraSources.isNotEmpty()) {
            item {
                SettingsSection(title = stringResource(R.string.from_other_apps)) {
                    state.extraSources.forEach { source ->
                        MiuixNavigationItemWidget(
                            title = source.name,
                            description = source.summary,
                            onClick = { onPickExternal(source) },
                        )
                    }
                }
            }
        }
    }
}
