// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.neo.clash.ui.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor

/**
 * Tinted card that carries an advisory message, optionally with a trailing action.
 *
 * [actionContent] is a slot rather than a button type so callers can use a text button, an icon
 * button or nothing at all; the row end padding adapts so the slot is never crowded.
 */
@Composable
private fun TipCard(
    modifier: Modifier = Modifier,
    tipContent: @Composable () -> Unit,
    actionContent: @Composable (() -> Unit)? = null,
) {
    val endPadding = if (actionContent == null) 16.dp else 12.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            contentColor = MiuixTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = endPadding, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                tipContent()
            }

            if (actionContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                actionContent()
            }
        }
    }
}

/**
 * Error card.
 *
 * The red palette is fixed rather than dynamic for the same reason as the status card: an error
 * has to read as an error regardless of the wallpaper-derived hue.
 */
@Composable
fun WarningCard(
    isDark: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = when {
                isDynamicColor -> MiuixTheme.colorScheme.errorContainer
                isDark -> Color(0xFF310808)
                else -> Color(0xFFF8E2E2)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = message,
                style = MiuixTheme.textStyles.body2,
                color = if (isDynamicColor) {
                    MiuixTheme.colorScheme.onErrorContainer
                } else {
                    Color(0xFFF72727)
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Advisory card used above settings groups. */
@Composable
fun MiuixSettingsTipCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    TipCard(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        tipContent = {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

/**
 * Error block with a human-readable heading and the raw detail underneath.
 *
 * The detail is selectable-looking but read-only: it is a [BasicTextField] purely so long stack
 * traces scroll independently of the page.
 */
@Composable
fun MiuixErrorTextBlock(
    heading: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val cardBackgroundColor = MiuixTheme.colorScheme.errorContainer
    val contentColor = MiuixTheme.colorScheme.onErrorContainer

    Card(
        modifier = modifier,
        colors = CardColors(
            color = cardBackgroundColor,
            contentColor = contentColor,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                color = MiuixTheme.colorScheme.error,
                text = heading,
                fontWeight = FontWeight.Bold,
                style = MiuixTheme.textStyles.body1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 16.dp, 16.dp, 8.dp),
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MiuixTheme.colorScheme.dividerLine,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp, 16.dp, 16.dp),
            ) {
                BasicTextField(
                    value = detail.trim(),
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MiuixTheme.textStyles.body2.copy(color = contentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Allow vertical scrolling for long stack traces.
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

/**
 * Status headline used by the control screen when something needs attention.
 *
 * Kept here beside [WarningCard] so every "something is wrong" surface shares one palette.
 */
@Composable
fun MiuixNoticeCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    WarningCard(
        isDark = AppTheme.isDark,
        message = message,
        modifier = modifier,
    )
}
