// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.logcat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One rendered log line. */
@Immutable
data class LogcatLineUi(
    val time: String,
    val level: String,
    val message: String,
)

@Immutable
data class LogcatUiState(
    val lines: List<LogcatLineUi> = emptyList(),
    val running: Boolean = false,
)

/**
 * Live log view, the Compose counterpart of `LogcatActivity`.
 *
 * [LogcatUiState.lines] is expected newest-first. Ordering in the host rather than scrolling here
 * keeps the newest message visible without owning a `LazyListState`: a log that silently appends
 * off-screen looks frozen, and driving the scroll from a page would fight the scaffold's own list.
 */
@Composable
fun LogcatPage(
    state: LogcatUiState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.logcat),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 8.dp,
    ) {
        if (state.lines.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            if (state.running) R.string.logcat_waiting else R.string.logcat_stopped,
                        ),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        } else {
            items(count = state.lines.size) { index ->
                val line = state.lines[index]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = "${line.time}  ${line.level}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = line.message,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}