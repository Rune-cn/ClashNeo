// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui

import io.github.neo.clash.core.model.ProxyGroup
import io.github.neo.clash.core.model.Traffic
import io.github.neo.clash.core.model.TunnelState
import io.github.neo.clash.service.model.Profile
import java.util.UUID

/**
 * What the 控制 screen knows about the tunnel right now.
 *
 * [phase] is kept apart from [mode]/[traffic] on purpose. The phase is a projection of two
 * independent facts — what the service last reported (`Broadcasts.running`) and whether a
 * start/stop *we* requested is still in flight — while the details are whatever the core answered
 * the last time it was asked. Keeping them apart is what stops the card from claiming "starting"
 * forever when the service never answers: the pending flag expires on its own inside
 * [ClashUiController], and the card falls back to the reported state.
 */
data class TunnelUiState(
    val phase: TunnelPhase = TunnelPhase.Stopped,
    val mode: TunnelState.Mode? = null,
    val activeProfile: Profile? = null,
    val traffic: Traffic = 0L,
    val trafficAvailable: Boolean = false,
    val detailsAvailable: Boolean = false,
    /** The service refused a start because the active profile has no configuration. */
    val needsProfile: Boolean = false,
    /** A manual refresh is running; the refresh action reports it instead of seeming dead. */
    val refreshing: Boolean = false,
    /** The last reason the core gave for stopping, when it exited abnormally. */
    val stopCause: String? = null,
) {
    /** Whether the tunnel is up *and* no transition is in flight. */
    val isRunning: Boolean
        get() = phase == TunnelPhase.Running

    /** The start/stop control must not accept a second press while a transition is in flight. */
    val isTransitioning: Boolean
        get() = phase == TunnelPhase.Starting ||
            phase == TunnelPhase.Stopping ||
            phase == TunnelPhase.Confirming

    /**
     * Traffic is only meaningful while the core is up.
     *
     * A stopped core keeps its last counter values, so rendering them after a stop would show a
     * frozen number that looks live.
     */
    val showTraffic: Boolean
        get() = trafficAvailable && isRunning

    /** Whether the details section has anything truthful to say about the core. */
    val showDetails: Boolean
        get() = isRunning && detailsAvailable
}

/**
 * The five states the status card can render.
 *
 * [Confirming] exists because a stop request is answered by the service, not by the controller: the
 * card must not flip to "stopped" the instant the button is pressed, or the user sees no
 * acknowledgement for the second or two the teardown takes.
 */
enum class TunnelPhase {
    Running,
    Stopped,
    Starting,
    Stopping,
    Confirming,
}

/**
 * One proxy group exactly as `IClashManager.queryProxyGroup` returned it.
 *
 * [selectable] mirrors the rule `ProxyActivity` applies: only a `Selector` group accepts
 * `patchSelector`. Rendering every group as tappable would produce taps the core silently rejects.
 * [error] carries the reason a group failed to load instead of hiding the group, so one broken
 * selector does not make the screen look like it has fewer groups than the configuration declares.
 */
data class ProxyGroupUiState(
    val name: String,
    val group: ProxyGroup? = null,
    val error: String? = null,
) {
    val selectable: Boolean
        get() = group?.type == SELECTOR_TYPE

    companion object {
        /**
         * The adapter name the core reports for a selectable group. Compared exactly because
         * `patchSelector` is only implemented for this adapter; URLTest/Fallback groups choose
         * their node by themselves.
         */
        const val SELECTOR_TYPE = "Selector"
    }
}

/**
 * Everything the 代理 screen renders.
 *
 * [loading] is only true for the very first load of a session; later loads keep the previous
 * groups on screen so switching tabs does not flash an empty list.
 */
data class ProxyUiState(
    val groups: List<ProxyGroupUiState> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty()
}

/**
 * Everything the 配置 screen renders.
 *
 * [profiles] is the raw `IProfileManager.queryAll()` result, so `active` is whatever the service
 * says rather than a copy that could drift while a switch is in flight. [switching] names the
 * profile whose activation has not completed yet.
 */
data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val switching: UUID? = null,
    val updatingAll: Boolean = false,
) {
    val isEmpty: Boolean
        get() = profiles.isEmpty()

    val activeProfile: Profile?
        get() = profiles.firstOrNull { it.active }
}

/**
 * Everything the 设置 screen renders.
 *
 * Every value is read from the same stores the XML settings screens use (`UiStore` for the app
 * preferences, `ServiceStore` for the tunnel), so these rows describe the app's actual
 * configuration rather than repeating its defaults.
 */
data class SettingsUiState(
    val appVersionName: String = "",
    val appVersionCode: Int = 0,
    val kernelVersion: String? = null,
    val enableVpn: Boolean = true,
    val darkModeLabel: String = "",
    val accessControlLabel: String = "",
    val profileCount: Int = 0,
    val tunnelRunning: Boolean = false,
)
