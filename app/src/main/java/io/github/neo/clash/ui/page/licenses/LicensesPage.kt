// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.licenses

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
import io.github.neo.clash.R
import io.github.neo.clash.ui.widget.MiuixBackButton
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** One attributed component. */
@Immutable
data class LicenseEntry(
    val name: String,
    val license: String,
    val url: String,
    val note: String? = null,
)

/**
 * Open-source notices.
 *
 * The list is explicit rather than generated: this project has no licence-metadata plugin, and a
 * page that silently claims to be complete is worse than a short, accurate one. Each entry here is
 * a component the app actually links against, read from `gradle/libs.versions.toml`.
 */
@Immutable
data class LicensesUiState(
    val entries: List<LicenseEntry> = defaultEntries(),
)

private fun defaultEntries(): List<LicenseEntry> = listOf(
    LicenseEntry(
        name = "ClashNeo",
        license = "GPL-3.0",
        url = "https://github.com/Rune-cn/ClashNeo",
        note = "This application itself.",
    ),
    LicenseEntry(
        name = "ClashMetaForAndroid",
        license = "GPL-3.0",
        url = "https://github.com/MetaCubeX/ClashMetaForAndroid",
        note = "Upstream project this UI is derived from.",
    ),
    LicenseEntry(
        name = "mihomo",
        license = "GPL-3.0",
        url = "https://github.com/MetaCubeX/mihomo",
        note = "The proxy kernel, as a git submodule.",
    ),
    LicenseEntry(
        name = "Miuix",
        license = "Apache-2.0",
        url = "https://github.com/compose-miuix-ui/miuix",
    ),
    LicenseEntry(
        name = "AndroidX / Jetpack Compose",
        license = "Apache-2.0",
        url = "https://developer.android.com/jetpack/androidx",
    ),
    LicenseEntry(
        name = "Kotlin & kotlinx.coroutines",
        license = "Apache-2.0",
        url = "https://github.com/JetBrains/kotlin",
    ),
    LicenseEntry(
        name = "kotlinx.serialization",
        license = "Apache-2.0",
        url = "https://github.com/Kotlin/kotlinx.serialization",
    ),
    LicenseEntry(
        name = "Room",
        license = "Apache-2.0",
        url = "https://developer.android.com/jetpack/androidx/releases/room",
    ),
)

/**
 * @param onOpen link handler; the page itself never launches a browser so it stays previewable.
 */
@Composable
fun LicensesPage(
    state: LicensesUiState,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
    onBack: () -> Unit = {},
    onOpen: (LicenseEntry) -> Unit = {},
) {
    MiuixPageScaffold(
        title = stringResource(R.string.settings_licenses),
        modifier = modifier,
        outerPadding = outerPadding,
        navigationIcon = { MiuixBackButton(onClick = onBack) },
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.licenses_intro),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_licenses)) {
                state.entries.forEach { entry ->
                    MiuixNavigationItemWidget(
                        title = entry.name,
                        description = entry.note?.let { "$it · ${entry.license}" }
                            ?: entry.license,
                        onClick = { onOpen(entry) },
                    )
                }
            }
        }
    }
}