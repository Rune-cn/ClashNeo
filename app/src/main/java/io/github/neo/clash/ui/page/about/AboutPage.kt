// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.ui.widget.SettingsEntry
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Immutable facts the about screen displays.
 *
 * [upstreamName] / [upstreamUrl] are kept explicit rather than hard-coded in the layout because the
 * GPL-3.0 obligation to credit the original project is a property of the build, not of this file.
 */
data class AboutUiState(
    val appName: String = "ClashNeo",
    val versionName: String = "",
    val versionCode: Int = 0,
    val upstreamName: String = "ClashMetaForAndroid",
    val upstreamUrl: String = "https://github.com/MetaCubeX/ClashMetaForAndroid",
    val sourceUrl: String = "https://github.com/Rune-cn/ClashNeo",
    val kernelName: String = "mihomo",
    val kernelUrl: String = "https://github.com/MetaCubeX/mihomo",
)

/**
 * About screen: identity block, version, and attribution links.
 *
 * The attribution section is not optional: this project is a GPL-3.0 derivative of
 * ClashMetaForAndroid, so the upstream and kernel links must remain visible to users.
 */
@Composable
fun AboutPage(
    state: AboutUiState,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = state.appName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (state.versionCode > 0) {
                        "${state.versionName} (${state.versionCode})"
                    } else {
                        state.versionName
                    },
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        SettingsSection(title = "Project") {
            SettingsEntry(
                title = "Source code",
                summary = state.sourceUrl,
                onClick = { uriHandler.openUri(state.sourceUrl) },
                trailing = { LinkIcon() },
            )
        }

        SettingsSection(title = "Credits") {
            SettingsEntry(
                title = state.upstreamName,
                summary = "Upstream project this is based on",
                onClick = { uriHandler.openUri(state.upstreamUrl) },
                trailing = { LinkIcon() },
            )
            SettingsEntry(
                title = state.kernelName,
                summary = "Proxy kernel",
                onClick = { uriHandler.openUri(state.kernelUrl) },
                trailing = { LinkIcon() },
            )
        }

        SettingsSection(title = "License") {
            SettingsEntry(
                title = "GNU General Public License v3.0",
                summary = "This app is free software and comes with ABSOLUTELY NO WARRANTY.",
            )
        }
    }
}

@Composable
private fun LinkIcon() {
    Icon(
        modifier = Modifier.size(20.dp),
        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        contentDescription = null,
    )
}
