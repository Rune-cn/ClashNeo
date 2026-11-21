// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.properties

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.neo.clash.design.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.MiuixTextField
import io.github.neo.clash.ui.widget.SettingsSection

/**
 * Editable fields of one profile, the Compose counterpart of `PropertiesDesign`.
 *
 * Every field is a [String] rather than a parsed number: the XML screen validates on commit and
 * reports an error span, so keeping the raw text lets the same validation surface a field-level
 * message instead of silently coercing (or rejecting) a half-typed value.
 *
 * [dirty] drives the "exit without saving" confirmation; without it the user loses edits on a
 * stray back press.
 */
@Immutable
data class PropertiesUiState(
    val name: String = "",
    val url: String = "",
    val ageSecretKey: String = "",
    val autoUpdateMinutes: String = "",
    val dirty: Boolean = false,
    val processing: Boolean = false,
    val nameError: Boolean = false,
    val urlError: Boolean = false,
    val autoUpdateError: Boolean = false,
)

/**
 * @param onBrowseFiles opens the stored-file picker; the XML screen offered the same action next to
 *   the URL field so a profile can point at a local file instead of a remote one.
 */
@Composable
fun PropertiesPage(
    state: PropertiesUiState,
    onStateChange: (PropertiesUiState) -> Unit,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onCommit: () -> Unit = {},
    onBrowseFiles: () -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.profile),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            SettingsSection(title = stringResource(R.string.properties)) {
                MiuixTextField(
                    value = state.name,
                    onValueChange = {
                        onStateChange(state.copy(name = it, dirty = true, nameError = false))
                    },
                    label = stringResource(R.string.name),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                MiuixTextField(
                    value = state.url,
                    onValueChange = {
                        onStateChange(state.copy(url = it, dirty = true, urlError = false))
                    },
                    label = stringResource(R.string.profile_url),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                MiuixTextField(
                    value = state.ageSecretKey,
                    onValueChange = { onStateChange(state.copy(ageSecretKey = it, dirty = true)) },
                    label = stringResource(R.string.age_secret_key_hint),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                MiuixTextField(
                    value = state.autoUpdateMinutes,
                    onValueChange = {
                        onStateChange(
                            state.copy(autoUpdateMinutes = it, dirty = true, autoUpdateError = false),
                        )
                    },
                    label = stringResource(R.string.auto_update_minutes),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.files)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.browse),
                    description = stringResource(R.string.profile_file),
                    onClick = onBrowseFiles,
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.action)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.save),
                    description = stringResource(R.string.commit_changes),
                    onClick = onCommit,
                )
            }
        }
    }
}
