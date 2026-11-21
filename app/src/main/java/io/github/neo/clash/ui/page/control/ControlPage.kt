// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.page.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neo.clash.R
import io.github.neo.clash.ui.TunnelPhase
import io.github.neo.clash.ui.TunnelUiState
import io.github.neo.clash.ui.theme.AppTheme
import io.github.neo.clash.ui.theme.StatusColors
import io.github.neo.clash.ui.util.TrafficSummary
import io.github.neo.clash.ui.widget.MiuixNavigationItemWidget
import io.github.neo.clash.ui.widget.MiuixPageScaffold
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
    MiuixPageScaffold(
        title = stringResource(R.string.tab_control),
        modifier = modifier,
        horizontalPadding = 12.dp,
        verticalPadding = 12.dp,
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            StatusCard(state = state)
        }

        item {
            ToggleButton(phase = state.phase, onToggle = onToggle)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SettingsSection(title = stringResource(R.string.section_status)) {
                StatusRows(state = state)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SettingsSection(title = stringResource(R.string.section_actions)) {
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.action_refresh),
                    description = stringResource(R.string.action_refresh_summary),
                    onClick = onRefresh,
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.action_logs),
                    description = stringResource(R.string.action_logs_summary),
                    onClick = onOpenLogs,
                )
                MiuixNavigationItemWidget(
                    title = stringResource(R.string.action_network_settings),
                    description = stringResource(R.string.action_network_settings_summary),
                    onClick = onOpenNetworkSettings,
                )
            }
        }
    }
}

/** Read-only rows describing the current tunnel. */
@Composable
private fun StatusRows(state: TunnelUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.label_mode),
            fontSize = 16.sp,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = state.mode?.let { modeName(it) } ?: "—",
            fontSize = 16.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }

    state.activeProfile?.let { profile ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.label_profile),
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = profile.name,
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }

    if (state.showTraffic) {
        val traffic = TrafficSummary.of(state.traffic)
        TrafficRow(label = stringResource(R.string.label_upload), value = traffic.upload)
        TrafficRow(label = stringResource(R.string.label_download), value = traffic.download)
        TrafficRow(label = stringResource(R.string.label_total), value = traffic.total)
    }
}

@Composable
private fun TrafficRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
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
            .height(52.dp)
            .padding(vertical = 6.dp),
        enabled = !pending,
    ) {
        Text(
            text = stringResource(
                when (phase) {
                    TunnelPhase.Running -> R.string.action_stop
                    TunnelPhase.Stopped -> R.string.action_start
                    TunnelPhase.Starting -> R.string.action_starting
                    TunnelPhase.Stopping -> R.string.action_stopping
                    // The VPN consent dialog is up; the button stays disabled and reads as pending.
                    TunnelPhase.Confirming -> R.string.action_waiting_permission
                },
            ),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Large status card mirroring the running / stopped / transitional phases.
 *
 * The palettes bypass the dynamic colour scheme on purpose: the green/red/amber distinction is the
 * primary signal and must survive a wallpaper-derived palette that happens to be green. The
 * container colour and the rotating glyph animate, so a phase change reads as a transition rather
 * than a hard cut.
 */
@Composable
private fun StatusCard(state: TunnelUiState) {
    val isDark = AppTheme.isDark

    val targetContainer = when (state.phase) {
        TunnelPhase.Running ->
            if (isDark) StatusColors.runningContainerDark else StatusColors.runningContainerLight
        TunnelPhase.Stopped ->
            if (isDark) StatusColors.stoppedContainerDark else StatusColors.stoppedContainerLight
        TunnelPhase.Starting,
        TunnelPhase.Stopping,
        TunnelPhase.Confirming ->
            if (isDark) StatusColors.busyContainerDark else StatusColors.busyContainerLight
    }

    val targetAccent = when (state.phase) {
        TunnelPhase.Running ->
            if (isDark) StatusColors.runningAccentDark else StatusColors.runningAccentLight
        TunnelPhase.Stopped ->
            if (isDark) StatusColors.stoppedAccentDark else StatusColors.stoppedAccentLight
        TunnelPhase.Starting,
        TunnelPhase.Stopping,
        TunnelPhase.Confirming ->
            if (isDark) StatusColors.busyAccentDark else StatusColors.busyAccentLight
    }

    val container by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "status-container",
    )
    val accent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "status-accent",
    )

    val glyph: ImageVector = when (state.phase) {
        TunnelPhase.Running -> Icons.Rounded.CheckCircle
        TunnelPhase.Stopped -> Icons.Rounded.Stop
        TunnelPhase.Starting,
        TunnelPhase.Stopping,
        TunnelPhase.Confirming -> Icons.Rounded.Sync
    }

    // The busy glyph spins so a slow handshake is visibly "working" instead of looking frozen.
    val spinning = state.phase == TunnelPhase.Starting ||
        state.phase == TunnelPhase.Stopping ||
        state.phase == TunnelPhase.Confirming
    val rotation by animateFloatAsState(
        targetValue = if (spinning) 360f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "status-glyph-rotation",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = container),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(38.dp, 16.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier
                        .size(148.dp)
                        .rotate(rotation),
                    imageVector = glyph,
                    tint = accent.copy(alpha = 0.8f),
                    contentDescription = null,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(
                        when (state.phase) {
                            TunnelPhase.Running -> R.string.status_running
                            TunnelPhase.Stopped -> R.string.status_stopped
                            TunnelPhase.Starting -> R.string.status_starting
                            TunnelPhase.Stopping -> R.string.status_stopping
                            TunnelPhase.Confirming -> R.string.status_waiting_permission
                        },
                    ),
                    fontSize = 24.sp,
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
    // rather than folded into Rule, because a user debugging their config needs to know which one
    // is active.
    io.github.neo.clash.core.model.TunnelState.Mode.Script -> "Script"
}
