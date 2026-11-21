// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.ui.theme.AppTheme
import io.github.neo.clash.ui.theme.StatusCardColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Model the home status card renders.
 *
 * [isRunning] mirrors `Remote.broadcasts.clashRunning`; [profileName] is the currently selected
 * profile, or `null` when nothing is selected yet. Kept as a small, dependency-free data class so
 * the page can be previewed and tested without the service binding.
 */
data class HomeUiState(
    val isRunning: Boolean = false,
    val profileName: String? = null,
    val versionName: String = "",
)

/**
 * Scrolling home page: the status card plus whatever the caller supplies below it.
 *
 * [content] is a slot rather than a fixed block, so settings entries can be added later without
 * touching this file again.
 */
@Composable
fun HomePage(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusCard(state = state)
        content()
    }
}

/**
 * Running / stopped status card.
 *
 * The green and red palettes deliberately bypass the dynamic colour scheme: "running" and
 * "stopped" have to stay recognisable even when the user's wallpaper-derived palette is orange.
 * The accent glyph carries the same colour at reduced alpha so it reads as decoration rather than
 * a second signal.
 */
@Composable
private fun StatusCard(state: HomeUiState) {
    val isHealthy = state.isRunning
    val isDark = AppTheme.isDark

    val containerColor = when {
        isHealthy && isDark -> StatusCardColors.runningDark
        isHealthy -> StatusCardColors.runningLight
        isDark -> StatusCardColors.abnormalDark
        else -> StatusCardColors.abnormalLight
    }

    val accentColor = when {
        isHealthy && isDark -> StatusCardColors.runningAccentDark
        isHealthy -> StatusCardColors.runningAccentLight
        isDark -> StatusCardColors.abnormalAccentDark
        else -> StatusCardColors.abnormalAccentLight
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = containerColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp),
        ) {
            // Oversized decorative glyph, offset away from the text column so the status label
            // always has room regardless of the user's font scale.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(42.dp, 22.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(124.dp),
                    imageVector = if (isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    tint = accentColor.copy(alpha = 0.8f),
                    contentDescription = null,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = if (isHealthy) "Running" else "Stopped",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )

                if (state.profileName != null) {
                    Text(
                        text = state.profileName,
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}
