// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.service.model.Profile
import io.github.neo.clash.ui.ProfilesUiState
import io.github.neo.clash.ui.util.formatElapsed
import io.github.neo.clash.ui.widget.SettingsEntry
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.UUID

/**
 * The 配置 (Profiles) tab.
 *
 * The list is the core's own `queryAll()` result, so the active marker reflects the service rather
 * than a local copy that could drift while a switch is in flight. While [ProfilesUiState.switching]
 * names a profile, that row is the only one disabled: tapping a second profile mid-switch would
 * race two activations against the same core.
 */
@Composable
fun ProfilesPage(
    state: ProfilesUiState,
    modifier: Modifier = Modifier,
    onActivate: (Profile) -> Unit,
) {
    val scrollModifier = modifier.fillMaxSize()

    when {
        state.error != null -> MessageState(
            title = "Profiles unavailable",
            body = state.error,
            modifier = scrollModifier,
        )

        state.profiles.isEmpty() && state.loading -> MessageState(
            title = "Loading profiles…",
            body = "Reading the profile list from the service.",
            modifier = scrollModifier,
        )

        state.profiles.isEmpty() -> MessageState(
            title = "No profiles",
            body = "Import a profile from Settings to get started.",
            modifier = scrollModifier,
        )

        else -> LazyColumn(
            modifier = scrollModifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.profiles, key = { it.uuid.toString() }) { profile ->
                ProfileRow(
                    profile = profile,
                    switching = state.switching == profile.uuid,
                    anySwitching = state.switching != null,
                    onActivate = onActivate,
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    switching: Boolean,
    anySwitching: Boolean,
    onActivate: (Profile) -> Unit,
) {
    SettingsSection(title = profile.name) {
        val detail = buildList {
            add(profile.type.name)
            formatElapsed(profile.updatedAt)?.let { add("updated $it") }
            if (profile.pending) add("pending")
        }.joinToString(" · ")

        SettingsEntry(
            title = if (profile.active) "Active" else "Tap to activate",
            summary = detail,
            enabled = !anySwitching && !profile.active,
            onClick = if (!anySwitching && !profile.active) {
                { onActivate(profile) }
            } else {
                null
            },
            trailing = when {
                switching -> {
                    { Text("Switching…", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
                }
                profile.active -> {
                    { ActiveMark() }
                }
                else -> null
            },
        )
    }
}

@Composable
private fun ActiveMark() {
    Icon(
        imageVector = Icons.Rounded.Check,
        tint = MiuixTheme.colorScheme.primary,
        contentDescription = null,
    )
}

@Composable
private fun MessageState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
