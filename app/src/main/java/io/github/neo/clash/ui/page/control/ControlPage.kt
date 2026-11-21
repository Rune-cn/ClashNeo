// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.ui.TunnelPhase
import io.github.neo.clash.ui.TunnelUiState
import io.github.neo.clash.ui.theme.AppTheme
import io.github.neo.clash.ui.theme.StatusColors
import io.github.neo.clash.ui.util.TrafficSummary
import io.github.neo.clash.ui.widget.SettingsEntry
import io.github.neo.clash.ui.widget.SettingsSection
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The 控制 (Control) tab.
 *
 * Every value here comes from [TunnelUiState], which [io.github.neo.clash.ui.ClashUiController]
 * keeps in sync with the service. Nothing on this screen is a placeholder: when the tunnel is down
 * the card says so, and when a field cannot be read (older core, core not started) the row is
 * omitted rather than showing a zero that looks like real traffic.
 */
@Composable
fun ControlPage(
    state: TunnelUiState,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onRefresh: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.padding(top = 12.dp)) {
            StatusCard(state = state)
        }

        ToggleButton(phase = state.phase, onToggle = onToggle)

        SettingsSection(title = "Status") {
            SettingsEntry(
                title = "Mode",
                summary = state.mode?.let { modeName(it) } ?: "—",
            )
            state.activeProfile?.let { profile ->
                SettingsEntry(title = "Profile", summary = profile.name)
            }
            if (state.showTraffic) {
                val traffic = TrafficSummary.of(state.traffic)
                SettingsEntry(title = "Upload", summary = traffic.upload)
                SettingsEntry(title = "Download", summary = traffic.download)
                SettingsEntry(title = "Total", summary = traffic.total)
            }
        }

        SettingsSection(title = "Actions") {
            SettingsEntry(
                title = "Refresh",
                summary = "Re-read status from the core",
                onClick = onRefresh,
            )
            SettingsEntry(title = "Logs", onClick = onOpenLogs)
            SettingsEntry(
                title = "Network settings",
                summary = "TUN, DNS and routing",
                onClick = onOpenNetworkSettings,
            )
        }
    }
}

/**
 * The tunnel switch.
 *
 * The button is disabled while a request is in flight: pressing it twice during the VPN handshake
 * would queue a start and a stop against the same service, and the core does not define which wins.
 */
@Composable
private fun ToggleButton(phase: TunnelPhase, onToggle: () -> Unit) {
    val pending = phase == TunnelPhase.Starting || phase == TunnelPhase.Stopping

    Button(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        enabled = !pending,
    ) {
        Text(
            text = when (phase) {
                TunnelPhase.Running -> "Stop"
                TunnelPhase.Stopped -> "Start"
                TunnelPhase.Starting -> "Starting…"
                TunnelPhase.Stopping -> "Stopping…"
                // The VPN consent dialog is up; the button stays disabled and reads as pending.
                TunnelPhase.Confirming -> "Waiting for permission…"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Large status card mirroring the running / stopped / transitional phases.
 *
 * The palettes bypass the dynamic colour scheme on purpose: the green/red/amber distinction is the
 * primary signal and must survive a wallpaper-derived palette that happens to be green.
 */
@Composable
private fun StatusCard(state: TunnelUiState) {
    val isDark = AppTheme.isDark

    val container = when (state.phase) {
        TunnelPhase.Running -> if (isDark) StatusColors.runningContainerDark else StatusColors.runningContainerLight
        TunnelPhase.Stopped -> if (isDark) StatusColors.stoppedContainerDark else StatusColors.stoppedContainerLight
        TunnelPhase.Starting,
        TunnelPhase.Stopping,
        TunnelPhase.Confirming -> if (isDark) StatusColors.busyContainerDark else StatusColors.busyContainerLight
    }

    val accent = when (state.phase) {
        TunnelPhase.Running -> if (isDark) StatusColors.runningAccentDark else StatusColors.runningAccentLight
        TunnelPhase.Stopped -> if (isDark) StatusColors.stoppedAccentDark else StatusColors.stoppedAccentLight
        TunnelPhase.Starting,
        TunnelPhase.Stopping,
        TunnelPhase.Confirming -> if (isDark) StatusColors.busyAccentDark else StatusColors.busyAccentLight
    }

    val glyph: ImageVector = when (state.phase) {
        TunnelPhase.Running -> Icons.Rounded.PlayArrow
        TunnelPhase.Stopped -> Icons.Rounded.Stop
        TunnelPhase.Starting,
        TunnelPhase.Stopping,
        TunnelPhase.Confirming -> Icons.Rounded.Sync
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = container),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(42.dp, 22.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(124.dp),
                    imageVector = glyph,
                    tint = accent.copy(alpha = 0.8f),
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
                    text = when (state.phase) {
                        TunnelPhase.Running -> "Running"
                        TunnelPhase.Stopped -> "Stopped"
                        TunnelPhase.Starting -> "Starting"
                        TunnelPhase.Stopping -> "Stopping"
                        TunnelPhase.Confirming -> "Waiting for permission"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                state.activeProfile?.let {
                    Text(
                        text = it.name,
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                if (state.showTraffic) {
                    val traffic = TrafficSummary.of(state.traffic)
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "↑ ${traffic.upload}",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = "↓ ${traffic.download}",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Display name for a tunnel mode.
 *
 * The enum is the core's serialized vocabulary (`direct` / `global` / `rule`), which is not what a
 * user should read, so the mapping lives next to the screen that shows it.
 */
internal fun modeName(mode: io.github.neo.clash.core.model.TunnelState.Mode): String = when (mode) {
    io.github.neo.clash.core.model.TunnelState.Mode.Direct -> "Direct"
    io.github.neo.clash.core.model.TunnelState.Mode.Global -> "Global"
    io.github.neo.clash.core.model.TunnelState.Mode.Rule -> "Rule"
    // `Script` is a valid core mode (a JavaScript rule provider drives routing). It is surfaced
    // rather than folded into Rule, because a user debugging their config needs to know which
    // one is active.
    io.github.neo.clash.core.model.TunnelState.Mode.Script -> "Script"
}
