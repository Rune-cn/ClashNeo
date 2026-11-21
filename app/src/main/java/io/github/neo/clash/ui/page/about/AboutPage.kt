// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.about

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
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
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    // A slow breathing pulse on the wordmark: enough motion to signal "this is the identity
    // block" without competing with the content below it.
    val pulse by rememberInfiniteTransition(label = "about-pulse").animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "about-pulse-scale",
    )

    MiuixPageScaffold(
        title = stringResource(R.string.settings_about),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = state.appName,
                        modifier = Modifier.scale(pulse),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
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
        }

        item {
            SettingsSection(title = stringResource(R.string.about_section_project)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.about_source_code),
                    description = state.sourceUrl,
                    onClick = { uriHandler.openUri(state.sourceUrl) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.about_section_credits)) {
                MiuixNavigationItemWidget(
                    title = state.upstreamName,
                    description = stringResource(R.string.about_upstream),
                    onClick = { uriHandler.openUri(state.upstreamUrl) },
                )
                MiuixNavigationItemWidget(
                    title = state.kernelName,
                    description = stringResource(R.string.about_kernel),
                    onClick = { uriHandler.openUri(state.kernelUrl) },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.about_section_license)) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.about_license_name),
                            fontSize = 16.sp,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.about_license_summary),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

/** Small outbound-link glyph for rows that leave the app. */
@Composable
internal fun LinkIcon() {
    Icon(
        modifier = Modifier.size(20.dp),
        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        contentDescription = null,
    )
}
