// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.logs

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

/** One saved log file, as reported by `Files.list`. */
@Immutable
data class LogFileUi(
    val fileName: String,
    val displayDate: String,
)

/**
 * Saved log list, the Compose counterpart of `LogsDesign`.
 *
 * [liveStreaming] mirrors whether the logcat service is running: while it is, the "start" action is
 * hidden because the stream already exists and starting a second one would spawn a duplicate
 * service.
 */
@Immutable
data class LogsUiState(
    val files: List<LogFileUi> = emptyList(),
    val liveStreaming: Boolean = false,
)

/**
 * @param onOpenLive opens the live logcat screen.
 * @param onOpenFile opens one saved file.
 * @param onDeleteAll wipes every saved file; the host is responsible for asking for confirmation,
 *   because a destructive prompt is a host concern rather than a list concern.
 */
@Composable
fun LogsPage(
    state: LogsUiState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onOpenLive: () -> Unit = {},
    onOpenFile: (LogFileUi) -> Unit = {},
    onDeleteAll: () -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.logs),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        if (!state.liveStreaming) {
            item {
                SettingsSection(title = stringResource(R.string.logcat)) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.start_logcat),
                        description = stringResource(R.string.start_logcat_summary),
                        onClick = onOpenLive,
                    )
                }
            }
        }

        if (state.files.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_logs),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        } else {
            item {
                SettingsSection(title = stringResource(R.string.logs)) {
                    state.files.forEach { file ->
                        MiuixNavigationItemWidget(
                            title = file.displayDate,
                            description = file.fileName,
                            onClick = { onOpenFile(file) },
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.action)) {
                    MiuixNavigationItemWidget(
                        title = stringResource(R.string.delete_all_logs),
                        description = stringResource(R.string.delete_all_logs_warn),
                        onClick = onDeleteAll,
                    )
                }
            }
        }
    }
}
