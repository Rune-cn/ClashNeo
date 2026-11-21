// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.R
import io.github.neo.clash.service.model.Profile
import io.github.neo.clash.ui.ProfilesUiState
import io.github.neo.clash.ui.util.formatElapsed
import io.github.neo.clash.ui.widget.MiuixPageScaffold
import io.github.neo.clash.ui.widget.SettingsEntry
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    outerPadding: PaddingValues = PaddingValues(),
    onActivate: (Profile) -> Unit,
) {
    when {
        state.error != null -> MessageState(
            title = stringResource(R.string.profiles_unavailable),
            body = state.error,
            modifier = modifier.fillMaxSize(),
        )

        state.profiles.isEmpty() && state.loading -> MessageState(
            title = stringResource(R.string.profiles_loading),
            body = stringResource(R.string.profiles_loading_body),
            modifier = modifier.fillMaxSize(),
        )

        state.profiles.isEmpty() -> MessageState(
            title = stringResource(R.string.profiles_empty),
            body = stringResource(R.string.profiles_empty_body),
            modifier = modifier.fillMaxSize(),
        )

        else -> MiuixPageScaffold(
            title = stringResource(R.string.tab_profiles),
            modifier = modifier,
            outerPadding = outerPadding,
            horizontalPadding = 12.dp,
            verticalPadding = 12.dp,
        ) {
            state.profiles.forEach { profile ->
                item(key = profile.uuid.toString()) {
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
}

@Composable
private fun ProfileRow(
    profile: Profile,
    switching: Boolean,
    anySwitching: Boolean,
    onActivate: (Profile) -> Unit,
) {
    Box(modifier = Modifier.padding(bottom = 4.dp)) {
        SettingsSection(title = profile.name) {
            val detail = buildList {
                add(profile.type.name)
                formatElapsed(profile.updatedAt, LocalResources.current)?.let {
                    add(stringResource(R.string.profile_updated_at, it))
                }
                if (profile.pending) add(stringResource(R.string.profile_pending))
            }.joinToString(" · ")

            val actionable = !anySwitching && !profile.active

            SettingsEntry(
                title = stringResource(
                    if (profile.active) R.string.profile_active else R.string.profile_tap_to_activate,
                ),
                summary = detail,
                enabled = actionable,
                onClick = if (actionable) {
                    { onActivate(profile) }
                } else {
                    null
                },
                trailing = {
                    when {
                        switching -> Text(
                            text = stringResource(R.string.profile_switching),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )

                        else -> AnimatedVisibility(
                            visible = profile.active,
                            enter = scaleIn(),
                            exit = scaleOut(),
                        ) {
                            ActiveMark()
                        }
                    }
                },
            )
        }
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
