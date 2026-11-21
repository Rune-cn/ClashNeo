// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.files

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

/** One entry in the working directory. */
@Immutable
data class FileEntryUi(
    val name: String,
    val directory: Boolean,
    val sizeLabel: String? = null,
)

/** Breadcrumb segment; the current directory is the last one and is not clickable. */
@Immutable
data class CrumbUi(val label: String, val current: Boolean)

@Immutable
data class FilesUiState(
    val path: List<CrumbUi> = emptyList(),
    val entries: List<FileEntryUi> = emptyList(),
)

/**
 * File browser for the core's working directory, the Compose counterpart of `FilesDesign`.
 *
 * The XML screen offered seven operations per entry (open, rename, delete, import, export…). Rather
 * than cramming them into the row, a tap opens the entry and long-press-style actions live behind
 * [onShowActions]; a row with five trailing icons is unusable on a phone.
 */
@Composable
fun FilesPage(
    state: FilesUiState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onOpenEntry: (FileEntryUi) -> Unit = {},
    onShowActions: (FileEntryUi) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.files),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = if (state.path.size > 1) onNavigateUp else onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        if (state.path.isNotEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = state.path.joinToString("/") { it.label },
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        if (state.entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.empty_directory),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        } else {
            item {
                SettingsSection(title = stringResource(R.string.files)) {
                    state.entries.forEach { entry ->
                        MiuixNavigationItemWidget(
                            title = entry.name,
                            description = entry.sizeLabel
                                ?: stringResource(R.string.directory),
                            onClick = { onOpenEntry(entry) },
                        )
                    }
                }
            }
        }
    }
}
