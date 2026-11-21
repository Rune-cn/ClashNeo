// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.rounded.Add
import top.yukonga.miuix.kmp.basic.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixTextField
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.window.WindowBottomSheet

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
    onAddFromFile: () -> Unit = {},
    onAddFromUrl: (String) -> Unit = {},
    onAddFromQr: () -> Unit = {},
) {
    // Sheet state is transient UI, so it lives here rather than being hoisted: no other screen
    // needs to know whether the add-sheet is open.
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf(ProfileSource.URL) }
    var urlInput by remember { mutableStateOf("") }

    // The FAB is a sibling of the screen, not part of it: it must stay above the list and
    // outside the scrolled content, otherwise it scrolls away and cannot be tapped at the end.
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.error != null -> MessageScreen(
                pageTitle = stringResource(R.string.tab_profiles),
                title = stringResource(R.string.profiles_unavailable),
                body = state.error,
                modifier = Modifier.fillMaxSize(),
                outerPadding = outerPadding,
            )

            state.profiles.isEmpty() && state.loading -> MessageScreen(
                pageTitle = stringResource(R.string.tab_profiles),
                title = stringResource(R.string.profiles_loading),
                body = stringResource(R.string.profiles_loading_body),
                modifier = Modifier.fillMaxSize(),
                outerPadding = outerPadding,
            )

            state.profiles.isEmpty() -> MessageScreen(
                pageTitle = stringResource(R.string.tab_profiles),
                title = stringResource(R.string.profiles_empty),
                body = stringResource(R.string.profiles_empty_body),
                modifier = Modifier.fillMaxSize(),
                outerPadding = outerPadding,
            )

            else -> MiuixPageScaffold(
                title = stringResource(R.string.tab_profiles),
                modifier = Modifier.fillMaxSize(),
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

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // Clear the navigation bar: the FAB sits above it rather than on top of it.
                .padding(end = 20.dp, bottom = outerPadding.calculateBottomPadding() + 20.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add_profile),
            )
        }

        // Slides up from the bottom, matching the upstream flow for adding a profile: pick a
        // source first, then supply only the field that source needs.
        WindowBottomSheet(
            show = showAddSheet,
            onDismissRequest = { showAddSheet = false },
            title = stringResource(R.string.add_profile),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileSource.entries.forEach { source ->
                    MiuixNavigationItemWidget(
                        title = stringResource(source.labelRes),
                        description = stringResource(source.summaryRes),
                        onClick = { selectedSource = source },
                    )
                }

                if (selectedSource == ProfileSource.URL) {
                    MiuixTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = stringResource(R.string.profile_url_hint),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                Button(
                    onClick = {
                        when (selectedSource) {
                            ProfileSource.FILE -> onAddFromFile()
                            ProfileSource.URL -> onAddFromUrl(urlInput.trim())
                            ProfileSource.QR -> onAddFromQr()
                        }
                        showAddSheet = false
                        urlInput = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(text = stringResource(R.string.add_profile_confirm))
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

/** How the user supplies the new profile. */
enum class ProfileSource { FILE, URL, QR }

private val ProfileSource.labelRes: Int
    get() = when (this) {
        ProfileSource.FILE -> R.string.add_profile_from_file
        ProfileSource.URL -> R.string.add_profile_from_url
        ProfileSource.QR -> R.string.add_profile_from_qr
    }

private val ProfileSource.summaryRes: Int
    get() = when (this) {
        ProfileSource.FILE -> R.string.add_profile_from_file_summary
        ProfileSource.URL -> R.string.add_profile_from_url_summary
        ProfileSource.QR -> R.string.add_profile_from_qr_summary
    }

@Composable
private fun MessageScreen(
    pageTitle: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    outerPadding: PaddingValues = PaddingValues(),
) {
    MiuixPageScaffold(
        title = pageTitle,
        modifier = modifier,
        outerPadding = outerPadding,
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
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
    }

    }
