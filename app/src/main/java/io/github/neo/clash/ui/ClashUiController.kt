// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui

import android.content.Context
import android.content.Intent
import io.github.neo.clash.ui.page.files.CrumbUi
import io.github.neo.clash.ui.page.files.FileEntryUi
import io.github.neo.clash.ui.page.files.FilesUiState
import io.github.neo.clash.ui.page.logs.LogFileUi
import io.github.neo.clash.ui.page.logs.LogsUiState
import io.github.neo.clash.ui.page.override.OverrideSettingsUiState
import io.github.neo.clash.util.clashDir
import io.github.neo.clash.util.logsDir
import io.github.neo.clash.ui.page.access.AccessApp
import io.github.neo.clash.ui.page.access.AccessControlUiState
import io.github.neo.clash.ui.page.access.AccessMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.neo.clash.R
import io.github.neo.clash.common.log.Log
import io.github.neo.clash.core.bridge.Bridge
import io.github.neo.clash.core.model.TunnelState
import io.github.neo.clash.design.model.DarkMode
import io.github.neo.clash.design.store.UiStore
import io.github.neo.clash.remote.Broadcasts
import io.github.neo.clash.remote.Remote
import io.github.neo.clash.service.model.AccessControlMode
import io.github.neo.clash.service.model.Profile
import io.github.neo.clash.service.store.ServiceStore
import io.github.neo.clash.util.startClashService
import io.github.neo.clash.util.stopClashService
import io.github.neo.clash.util.withClash
import io.github.neo.clash.util.withProfile
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The single owner of every piece of state the Compose UI shows.
 *
 * Why this class exists: the XML screens drive themselves from a `BaseActivity` loop that reads
 * `design.requests` and calls into `withClash` / `withProfile`. That loop cannot be reused from
 * Compose, and re-implementing the reads inside composables would re-fetch on every recomposition.
 * This controller is the equivalent loop expressed as a state machine: it owns [tunnel], [proxies],
 * [profiles] and [settings], refreshes them through the same suspend helpers the XML UI uses, and
 * exposes `StateFlow`s that Compose collects.
 *
 * Reactivity comes from [Broadcasts.Observer], not from polling `clashRunning`:
 * [broadcastsObserver] is added to `Remote.broadcasts` in [start] and removed in [stop], which the
 * host calls from `onStart`/`onStop`. The `running` flow is collected as well, so the state is
 * correct even when the app is brought back to the foreground after a stop that happened while it
 * was in the background (`Broadcasts.register()` re-seeds it from `StatusClient` on every
 * transition to visible).
 *
 * It is deliberately not an Android `ViewModel`: it must outlive tab switches but be tied to the
 * host activity's scope, because every query goes through `Remote.service`, which `Remote` unbinds
 * as soon as the app is no longer visible.
 */
class ClashUiController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val broadcasts = Remote.broadcasts

    private val _tunnel = MutableStateFlow(TunnelUiState())
    val tunnel: StateFlow<TunnelUiState> = _tunnel.asStateFlow()

    private val _proxies = MutableStateFlow(ProxyUiState())
    val proxies: StateFlow<ProxyUiState> = _proxies.asStateFlow()

    private val _profiles = MutableStateFlow(ProfilesUiState())
    val profiles: StateFlow<ProfilesUiState> = _profiles.asStateFlow()

    private val _settings = MutableStateFlow(SettingsUiState())
    val settings: StateFlow<SettingsUiState> = _settings.asStateFlow()

    private val _access = MutableStateFlow(AccessControlUiState())
    val access: StateFlow<AccessControlUiState> = _access.asStateFlow()

    private val _override = MutableStateFlow(OverrideSettingsUiState())
    val overrideState: StateFlow<OverrideSettingsUiState> = _override.asStateFlow()

    private val _logs = MutableStateFlow(LogsUiState())
    val logs: StateFlow<LogsUiState> = _logs.asStateFlow()

    private val _files = MutableStateFlow(FilesUiState())
    val files: StateFlow<FilesUiState> = _files.asStateFlow()

    /** Directory the file browser is currently showing. */
    private var currentDir: java.io.File = context.clashDir

    private val _messages = MutableStateFlow<ChannelMessages?>(null)

    /**
     * The message to show next, or `null` when there is nothing pending.
     *
     * A `StateFlow` rather than a `SharedFlow` on purpose: a snackbar that is emitted while the
     * activity is stopped would otherwise be silently dropped, and the state that produced it (a
     * rejected start, say) would leave the user with no explanation. The host calls
     * [consumeMessage] once it has shown it.
     */
    val messages: StateFlow<ChannelMessages?> = _messages.asStateFlow()

    /** Emits once per `ACTION_PROFILE_LOADED`, so an activation can await the core's reload. */
    private val profileLoaded = MutableStateFlow(0L)

    private var pending: PendingToggle? = null
    private var pendingTimeout: Job? = null

    private var trafficJob: Job? = null
    private var started = false

    /**
     * Mirrors [Broadcasts.Observer] onto the flows.
     *
     * The callbacks arrive on the main thread from the broadcast receiver, so they only *schedule*
     * work; every query into the service is a suspend call that must not run there.
     */
    private val broadcastsObserver = object : Broadcasts.Observer {
        override fun onStarted() {
            scope.launch {
                clearPending()
                publishPhase()
                syncTunnel()
                refreshProxies()
                refreshSettings()
            }
        }

        override fun onStopped(cause: String?) {
            scope.launch {
                clearPending()
                _tunnel.value = _tunnel.value.copy(stopCause = cause)
                publishPhase()
                syncTunnel()
                // The groups only exist while the core is up; leaving them on screen would let the
                // user tap a node that the core can no longer patch.
                _proxies.value = ProxyUiState()
                refreshSettings()
            }
        }

        override fun onServiceRecreated() {
            scope.launch {
                clearPending()
                publishPhase()
                syncTunnel()
                refreshProxies()
            }
        }

        override fun onProfileChanged() {
            scope.launch {
                syncTunnel()
                refreshProfiles()
                refreshSettings()
            }
        }

        override fun onProfileLoaded() {
            profileLoaded.value = profileLoaded.value + 1
            scope.launch {
                clearPending()
                publishPhase()
                syncTunnel()
                refreshProfiles()
                refreshProxies()
            }
        }

        override fun onProfileUpdateCompleted(uuid: UUID?) {
            if (uuid == null)
                return

            scope.launch {
                val name = runCatching {
                    withProfile { queryByUUID(uuid)?.name }
                }.getOrNull()

                _messages.value = ChannelMessages.Text(
                    context.getString(R.string.message_profile_updated, name ?: uuid.toString()),
                )

                refreshProfiles()
            }
        }

        override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
            if (uuid == null)
                return

            scope.launch {
                val name = runCatching {
                    withProfile { queryByUUID(uuid)?.name }
                }.getOrNull()

                _messages.value = ChannelMessages.Text(
                    context.getString(
                        R.string.message_profile_update_failed,
                        name ?: uuid.toString(),
                        reason ?: context.getString(R.string.state_unknown_error),
                    ),
                )

                refreshProfiles()
            }
        }
    }

    /** Starts the state machine. Safe to call more than once; the observer is registered once. */
    fun start() {
        if (started)
            return

        started = true

        broadcasts.addObserver(broadcastsObserver)

        // The authoritative edge for "did the tunnel come up": assigned by `Broadcasts` itself, so
        // it also covers the seed performed by `register()` on every return to the foreground.
        scope.launch {
            var previous = broadcasts.running.value

            broadcasts.running.collect { value ->
                if (value != previous) {
                    previous = value
                    clearPending()
                    publishPhase()
                    if (!value) {
                        _proxies.value = ProxyUiState()
                    }
                }
            }
        }

        publishPhase()
        refreshAll()
    }

    /** Removes every observer and job this controller owns. The host calls this from `onDestroy`. */
    fun stop() {
        if (!started)
            return

        started = false

        broadcasts.removeObserver(broadcastsObserver)

        trafficJob?.cancel()
        trafficJob = null

        pendingTimeout?.cancel()
        pendingTimeout = null
        pending = null
    }

    /** Reloads every tab; used on first attach, on appearance and by the manual refresh action. */
    fun refreshAll() {
        _tunnel.value = _tunnel.value.copy(refreshing = true)

        scope.launch {
            try {
                syncTunnel()
                refreshProfiles()
                refreshProxies()
                refreshSettings()
            } finally {
                _tunnel.value = _tunnel.value.copy(refreshing = false)
            }
        }
    }

    fun consumeMessage() {
        _messages.value = null
    }

    // region Control

    /**
     * Starts or stops the tunnel.
     *
     * The VPN handshake is asynchronous: `startClashService()` can return a `VpnService.prepare`
     * intent, which only the host activity may launch for a result. That intent is handed back
     * through [ChannelMessages.VpnPermissionRequired] so the Compose activity keeps ownership of
     * the activity-result contract instead of this class holding a reference to an activity.
     */
    fun toggleTunnel() {
        if (pending != null)
            return

        if (broadcasts.running.value) {
            requestStop()
        } else {
            scope.launch { requestStart() }
        }
    }

    /** Called by the host once the VPN consent dialog returned `Activity.RESULT_OK`. */
    fun onVpnPermissionGranted() {
        try {
            context.startClashService()
        } catch (e: Exception) {
            Log.w("Start clash service after consent: $e", e)
            _messages.value = ChannelMessages.Text(context.getString(R.string.message_start_failed), isError = true)
            return
        }

        markPending(PendingToggle.Starting)
    }

    private suspend fun requestStart() {
        val active = runCatching {
            withProfile { queryActive() }
        }.getOrNull()

        // Matches `MainDesign.startClash`: a profile that has not been imported has no
        // configuration to load, and the service would fail after the VPN prompt.
        if (active == null || !active.imported) {
            _tunnel.value = _tunnel.value.copy(needsProfile = true)
            _messages.value = ChannelMessages.Text(context.getString(R.string.message_select_profile_first))
            return
        }

        _tunnel.value = _tunnel.value.copy(needsProfile = false)

        val vpnRequest = try {
            context.startClashService()
        } catch (e: Exception) {
            Log.w("Start clash service: $e", e)
            _messages.value = ChannelMessages.Text(context.getString(R.string.message_start_failed), isError = true)
            return
        }

        if (vpnRequest != null) {
            // The card stays "stopped" until consent is answered: claiming "starting" here would be
            // a lie if the user cancels the dialog.
            _messages.value = ChannelMessages.VpnPermissionRequired(vpnRequest)
            return
        }

        markPending(PendingToggle.Starting)
    }

    private fun requestStop() {
        // Unlike start, stop needs no preconditions: the core tolerates a stop while it is already
        // shutting down, and refusing one here would strand the user on "running".
        context.stopClashService()
        markPending(PendingToggle.Stopping)
    }

    private fun markPending(kind: PendingToggle) {
        pending = kind

        pendingTimeout?.cancel()
        pendingTimeout = scope.launch {
            delay(PENDING_TIMEOUT)
            if (pending != null) {
                // No completion broadcast arrived. Fall back to whatever the service says, so the
                // control is usable again instead of staying disabled forever.
                pending = null
                publishPhase()
            }
        }

        publishPhase()
    }

    private fun clearPending() {
        pending = null
        pendingTimeout?.cancel()
        pendingTimeout = null
    }

    /**
     * Projects the reported tunnel state and any in-flight request into [TunnelUiState.phase].
     *
     * Traffic polling is started and stopped from here because it is only meaningful while the core
     * is up, and this is the only place that knows when that changes.
     */
    private fun publishPhase() {
        val reportedRunning = broadcasts.running.value

        val phase = when {
            pending == PendingToggle.Starting && !reportedRunning -> TunnelPhase.Starting
            // A start that already flipped the service on, but whose details are still loading, is
            // shown as running: the user's request has succeeded at that point.
            pending == PendingToggle.Starting -> TunnelPhase.Running
            pending == PendingToggle.Stopping -> TunnelPhase.Confirming
            reportedRunning -> TunnelPhase.Running
            else -> TunnelPhase.Stopped
        }

        _tunnel.value = _tunnel.value.copy(phase = phase)

        if (phase == TunnelPhase.Running) {
            startTrafficPolling()
        } else {
            stopTrafficPolling()
        }
    }

    /**
     * Reads mode, traffic and the active profile from the core.
     *
     * Every query goes through the same `withClash` / `withProfile` helpers the XML screens use, so
     * a dead remote service is retried there rather than crashing the screen. A failure leaves the
     * previous value in place and marks the details unavailable instead of reporting a zero that
     * would look like real traffic.
     */
    private suspend fun syncTunnel() {
        val running = broadcasts.running.value

        publishPhase()

        if (!running) {
            _tunnel.value = _tunnel.value.copy(
                mode = null,
                traffic = 0L,
                trafficAvailable = false,
                detailsAvailable = false,
            )

            syncActiveProfile()

            return
        }

        val mode = runCatching {
            withClash { queryTunnelState().mode }
        }.onFailure {
            Log.w("Query tunnel state: $it", it)
        }.getOrNull()

        val traffic = runCatching {
            withClash { queryTrafficTotal() }
        }.onFailure {
            Log.w("Query traffic total: $it", it)
        }.getOrNull()

        _tunnel.value = _tunnel.value.copy(
            mode = mode,
            traffic = traffic ?: 0L,
            trafficAvailable = traffic != null,
            detailsAvailable = mode != null,
            stopCause = null,
        )

        syncActiveProfile()
    }

    private suspend fun syncActiveProfile() {
        val active = runCatching {
            withProfile { queryActive() }
        }.onFailure {
            Log.w("Query active profile: $it", it)
        }.getOrNull()

        _tunnel.value = _tunnel.value.copy(activeProfile = active)
    }

    private fun startTrafficPolling() {
        if (trafficJob?.isActive == true)
            return

        trafficJob = scope.launch {
            while (true) {
                val traffic = runCatching {
                    withClash { queryTrafficTotal() }
                }.getOrNull()

                if (traffic != null) {
                    _tunnel.value = _tunnel.value.copy(traffic = traffic, trafficAvailable = true)
                }

                delay(TRAFFIC_INTERVAL)
            }
        }
    }

    private fun stopTrafficPolling() {
        trafficJob?.cancel()
        trafficJob = null
    }

    // endregion

    // region Proxy

    /**
     * Reloads the proxy groups.
     *
     * The list is cleared while the tunnel is down so the screen shows its "start the tunnel first"
     * empty state rather than a stale list from a previous session.
     */
    fun refreshProxies() {
        scope.launch {
            if (!broadcasts.running.value) {
                _proxies.value = ProxyUiState()
                return@launch
            }

            val previous = _proxies.value

            _proxies.value = previous.copy(
                loading = previous.groups.isEmpty(),
                error = null,
            )

            val names = runCatching {
                withClash { queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) }
            }.onFailure {
                Log.w("Query proxy group names: $it", it)
            }.getOrElse { error ->
                _proxies.value = previous.copy(
                    loading = false,
                    error = error.message ?: error.toString(),
                )
                return@launch
            }

            _proxies.value = ProxyUiState(loading = true)

            val groups = names.map { name ->
                val loaded = runCatching {
                    withClash { queryProxyGroup(name, uiStore.proxySort) }
                }.onFailure {
                    Log.w("Query proxy group $name: $it", it)
                }

                ProxyGroupUiState(
                    name = name,
                    group = loaded.getOrNull(),
                    error = loaded.exceptionOrNull()?.let { error ->
                        error.message ?: error.toString()
                    },
                )
            }

            _proxies.value = ProxyUiState(groups = groups, loading = false)
        }
    }

    /**
     * Points [group] at [node].
     *
     * `patchSelector` returns whether the core accepted the change. A `false` is surfaced to the
     * user rather than reflected in the list, because re-rendering an unchanged selection would look
     * like the tap was simply ignored.
     */
    fun selectProxy(group: String, node: String) {
        scope.launch {
            val accepted = runCatching {
                withClash { patchSelector(group, node) }
            }.onFailure {
                Log.w("Patch selector: $it", it)
            }.getOrElse { false }

            if (!accepted) {
                _messages.value = ChannelMessages.Text(context.getString(R.string.message_proxy_not_selectable))
                return@launch
            }

            _proxies.value = _proxies.value.copy(
                groups = _proxies.value.groups.map { entry ->
                    if (entry.name != group || entry.group == null)
                        entry
                    else
                        entry.copy(group = entry.group.copy(now = node))
                },
            )

            // A group can embed other groups, so this change may move the effective node of a
            // sibling group too. Re-reading is the only correct way to learn that.
            refreshProxies()
        }
    }

    // endregion

    // region Profiles

    /** Reloads the profile list from `IProfileManager`. */
    fun refreshProfiles() {
        scope.launch {
            val previous = _profiles.value

            if (previous.profiles.isEmpty())
                _profiles.value = previous.copy(loading = true, error = null)

            val all = runCatching {
                withProfile { queryAll() }
            }.onFailure {
                Log.w("Query profiles: $it", it)
            }.getOrElse { error ->
                _profiles.value = previous.copy(
                    loading = false,
                    error = error.message ?: error.toString(),
                )
                return@launch
            }

            _profiles.value = ProfilesUiState(
                profiles = all,
                loading = false,
                switching = previous.switching,
                updatingAll = previous.updatingAll,
            )
        }
    }

    /**
     * Makes [profile] the active one.
     *
     * A profile that was never imported has nothing to activate, so instead of silently doing
     * nothing the editor is opened — the same rule `ProfilesActivity` applies through
     * `ProfilesDesign.requestSave`.
     */
    /**
     * Loads the installed-app list plus the persisted selection.
     *
     * The list comes from `PackageManager` (the manifest already holds `QUERY_ALL_PACKAGES`), and the
     * selection from `ServiceStore`, which is the store the service reads when it builds the rule
     * set. Before this the screen had an empty list and nothing to select.
     */
    fun refreshAccessControl() {
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                pm.getInstalledApplications(0).map { info ->
                    AccessApp(
                        packageName = info.packageName,
                        label = runCatching {
                            pm.getApplicationLabel(info).toString()
                        }.getOrDefault(info.packageName),
                        system = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }.sortedBy { it.label.lowercase() }
            }

            val store = ServiceStore(context)
            val mode = when (store.accessControlMode) {
                AccessControlMode.AcceptAll -> AccessMode.ACCEPT
                AccessControlMode.AcceptSelected -> AccessMode.ACCEPT
                AccessControlMode.DenySelected -> AccessMode.REJECT
            }

            _access.value = AccessControlUiState(
                apps = apps,
                selected = store.accessControlPackages,
                mode = mode,
            )
        }
    }

    /**
     * Saves the selection.
     *
     * The store keeps one set plus a three-way mode. Choosing "accept" with a non-empty set means
     * only the selected apps are proxied; "reject" means everything except them. An empty set in
     * either case means "no restriction", which maps back to `AcceptAll`.
     */
    fun saveAccessControl(state: AccessControlUiState) {
        // Reflect the change immediately; the screen reads this flow, so persisting alone would
        // leave the checkbox visibly stuck in its old position.
        _access.value = state

        val store = ServiceStore(context)
        store.accessControlPackages = state.selected
        store.accessControlMode = when {
            state.selected.isEmpty() -> AccessControlMode.AcceptAll
            state.mode == AccessMode.ACCEPT -> AccessControlMode.AcceptSelected
            else -> AccessControlMode.DenySelected
        }
    }

    /**
     * Reads the persisted override from the core.
     *
     * Overrides live in the core, not in a preferences file, so the only truthful source is
     * `queryOverride`. Without this the screen showed defaults that had no relation to the running
     * configuration.
     */
    fun refreshOverride() {
        scope.launch {
            val result = runCatching {
                withClash { queryOverride(io.github.neo.clash.core.Clash.OverrideSlot.Persist) }
            }

            result.onSuccess { cfg ->
                _override.value = OverrideSettingsUiState(
                    httpPort = cfg.httpPort?.toString().orEmpty(),
                    socksPort = cfg.socksPort?.toString().orEmpty(),
                    mixedPort = cfg.mixedPort?.toString().orEmpty(),
                    redirectPort = cfg.redirectPort?.toString().orEmpty(),
                    tproxyPort = cfg.tproxyPort?.toString().orEmpty(),
                    allowLan = cfg.allowLan ?: false,
                    allowIpv6 = cfg.ipv6 ?: false,
                    bindAddress = cfg.bindAddress.orEmpty(),
                    authentication = !cfg.authentication.isNullOrEmpty(),
                    externalController = cfg.externalController.orEmpty(),
                    logLevelIndex = cfg.logLevel?.let { level ->
                        io.github.neo.clash.core.model.LogMessage.Level.entries
                            .indexOf(level)
                    } ?: 0,
                    modeIndex = cfg.mode?.let { mode ->
                        io.github.neo.clash.core.model.TunnelState.Mode.entries.indexOf(mode)
                    } ?: 0,
                )
            }.onFailure { error ->
                Log.w("Query override: $error", error)
                postMessage(
                    error.message ?: context.getString(R.string.override_load_failed),
                    isError = true,
                )
            }
        }
    }

    /**
     * Writes the override back to the core.
     *
     * The current override is re-read first and only the fields this screen owns are replaced:
     * sending a fresh object would silently drop `secret`, `hosts` and the CORS block, which have no
     * editor here.
     */
    fun saveOverride(state: OverrideSettingsUiState) {
        scope.launch {
            val result = runCatching {
                withClash {
                    val current = queryOverride(io.github.neo.clash.core.Clash.OverrideSlot.Persist)
                    val next = current.copy(
                        httpPort = state.httpPort.toIntOrNull(),
                        socksPort = state.socksPort.toIntOrNull(),
                        mixedPort = state.mixedPort.toIntOrNull(),
                        redirectPort = state.redirectPort.toIntOrNull(),
                        tproxyPort = state.tproxyPort.toIntOrNull(),
                        allowLan = state.allowLan,
                        ipv6 = state.allowIpv6,
                        bindAddress = state.bindAddress.takeIf { it.isNotBlank() },
                        externalController = state.externalController.takeIf { it.isNotBlank() },
                        logLevel = io.github.neo.clash.core.model.LogMessage.Level.entries
                            .getOrNull(state.logLevelIndex),
                        mode = io.github.neo.clash.core.model.TunnelState.Mode.entries
                            .getOrNull(state.modeIndex),
                    )
                    patchOverride(io.github.neo.clash.core.Clash.OverrideSlot.Persist, next)
                }
            }

            result.onFailure { error ->
                Log.w("Save override: $error", error)
                postMessage(
                    error.message ?: context.getString(R.string.override_save_failed),
                    isError = true,
                )
            }
        }
    }

    /**
     * Lists saved log files, newest first.
     *
     * Reads the filesystem directly: the log service writes plain files and there is no query API
     * for them, so a listing is the only honest source.
     */
    fun refreshLogs() {
        val files = context.logsDir.listFiles()
            ?.mapNotNull { io.github.neo.clash.design.model.LogFile.parseFromFileName(it.name) }
            ?.sortedByDescending { it.date.time }
            ?: emptyList()

        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        _logs.value = LogsUiState(
            files = files.map { LogFileUi(it.fileName, formatter.format(it.date)) },
            liveStreaming = io.github.neo.clash.LogcatService.running,
        )
    }

    /**
     * Lists the core's working directory.
     *
     * This is what the file browser shows, and it is the directory profiles and the kernel actually
     * read, so what the user sees here is what the core sees.
     */
    fun refreshFiles() {
        val root = context.clashDir
        // The current directory can disappear (deleted, or cleared from another screen); fall back
        // to the root instead of showing an empty listing with a stale path.
        if (!currentDir.exists() || !currentDir.canonicalPath.startsWith(root.canonicalPath)) {
            currentDir = root
        }

        val entries = currentDir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.map { file ->
                FileEntryUi(
                    name = file.name,
                    directory = file.isDirectory,
                    sizeLabel = if (file.isDirectory) null else formatSize(file.length()),
                )
            }
            ?: emptyList()

        val crumbs = buildList {
            add(CrumbUi(root.name, currentDir == root))
            val relative = currentDir.relativeTo(root).path
            if (relative.isNotEmpty() && relative != ".") {
                var acc = root
                relative.split(java.io.File.separatorChar).forEach { part ->
                    acc = java.io.File(acc, part)
                    add(CrumbUi(part, acc == currentDir))
                }
            }
        }

        _files.value = FilesUiState(path = crumbs, entries = entries)
    }

    /** Opens a subdirectory of the current directory. */
    fun enterDirectory(name: String) {
        val next = java.io.File(currentDir, name)
        if (next.isDirectory) {
            currentDir = next
            refreshFiles()
        }
    }

    /** Goes up one level; stays at the working directory root. */
    fun navigateUpDirectory() {
        val root = context.clashDir
        if (currentDir != root) {
            currentDir = currentDir.parentFile ?: root
            refreshFiles()
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    }

    /** Surfaces a one-off message to the host, for paths that have nothing else to report. */
    fun postMessage(text: String, isError: Boolean = false) {
        _messages.value = ChannelMessages.Text(text = text, isError = isError)
    }

    /**
     * Persists edits to a profile.
     *
     * [intervalMinutes] is minutes because that is what the UI collects; the core stores
     * milliseconds, so the conversion happens here rather than at the call site where it is easy to
     * get wrong by a factor of 60.
     */
    fun saveProfile(
        uuid: UUID,
        name: String,
        source: String,
        intervalMinutes: Long,
        ageSecretKey: String?,
    ) {
        if (name.isBlank() || source.isBlank()) {
            postMessage(context.getString(R.string.add_profile_invalid), isError = true)
            return
        }

        scope.launch {
            val result = runCatching {
                withProfile {
                    patch(
                        uuid = uuid,
                        name = name,
                        source = source,
                        interval = TimeUnit.MINUTES.toMillis(intervalMinutes),
                        ageSecretKey = ageSecretKey,
                    )
                    // `patch` only writes the pending row. Without `commit` the edits never reach
                    // the imported copy and the profile keeps running its old configuration.
                    commit(uuid, null)
                }
            }

            result.onSuccess { refreshProfiles() }
                .onFailure { error ->
                    Log.w("Save profile: $error", error)
                    postMessage(
                        error.message ?: context.getString(R.string.add_profile_failed),
                        isError = true,
                    )
                }
        }
    }

    /**
     * Re-downloads a profile from its source ("拉取配置").
     *
     * Only meaningful for URL profiles; a file or external profile has nothing to fetch, so the
     * call is skipped rather than issued and failed by the core.
     */
    fun updateProfile(profile: Profile) {
        if (profile.type == Profile.Type.File || profile.type == Profile.Type.External) {
            postMessage(context.getString(R.string.profile_update_not_applicable), isError = true)
            return
        }

        scope.launch {
            val result = runCatching {
                withProfile {
                    // A profile that was created but never imported has no configuration to refresh;
                    // `update` would schedule a fetch for content that does not exist yet. `commit`
                    // is what moves it from pending to imported.
                    if (profile.imported) update(profile.uuid) else commit(profile.uuid, null)
                }
            }
            result.onSuccess { refreshProfiles() }
                .onFailure { error ->
                    Log.w("Update profile: $error", error)
                    postMessage(
                        error.message ?: context.getString(R.string.profile_update_failed_generic),
                        isError = true,
                    )
                }
        }
    }

    /** Copies a profile, including its source and interval. */
    fun duplicateProfile(profile: Profile) {
        scope.launch {
            val result = runCatching { withProfile { clone(profile.uuid) } }
            result.onSuccess { refreshProfiles() }
                .onFailure { error ->
                    Log.w("Duplicate profile: $error", error)
                    postMessage(
                        error.message ?: context.getString(R.string.profile_duplicate_failed),
                        isError = true,
                    )
                }
        }
    }

    /** Deletes a profile. Irreversible on the core side, so the host confirms first. */
    fun deleteProfile(profile: Profile) {
        scope.launch {
            val result = runCatching { withProfile { delete(profile.uuid) } }
            result.onSuccess { refreshProfiles() }
                .onFailure { error ->
                    Log.w("Delete profile: $error", error)
                    postMessage(
                        error.message ?: context.getString(R.string.profile_delete_failed),
                        isError = true,
                    )
                }
        }
    }

    /**
     * Creates a profile and actually downloads it.
     *
     * `create` alone only inserts a *pending* row with an empty `config.yaml` — the profile has no
     * content, cannot be activated, and shows nothing. The content arrives through `commit`, which
     * runs `ProfileProcessor.apply`: it fetches the source, writes the real configuration and moves
     * the profile from pending to imported. That call was missing, which is why adding a profile
     * produced a row that did nothing.
     *
     * `ProfileProcessor` also reads the `subscription-userinfo` and `profile-update-interval`
     * headers during the same fetch, so traffic figures and the auto-update interval land on the
     * profile without this class doing anything.
     */
    fun importProfile(
        type: Profile.Type,
        name: String,
        source: String,
        intervalMinutes: Long = 0L,
        ageSecretKey: String? = null,
    ) {
        if (name.isBlank() || source.isBlank()) {
            postMessage(context.getString(R.string.add_profile_invalid), isError = true)
            return
        }

        scope.launch {
            postMessage(context.getString(R.string.profile_importing))

            val result = runCatching {
                withProfile {
                    val uuid = create(type, name, source, ageSecretKey)
                    // Patch first so `commit` fetches with the real name and interval rather than
                    // the placeholder values `create` stored.
                    patch(
                        uuid = uuid,
                        name = name,
                        source = source,
                        interval = TimeUnit.MINUTES.toMillis(intervalMinutes),
                        ageSecretKey = ageSecretKey,
                    )
                    commit(uuid, null)
                    uuid
                }
            }

            result.onSuccess { uuid ->
                refreshProfiles()

                // First usable profile: activate it, otherwise the tunnel still has nothing to run
                // and the app looks broken even after a successful import.
                val hasActive = _profiles.value.profiles.any { it.active }
                if (!hasActive) {
                    runCatching { withProfile { setActive(queryByUUID(uuid) ?: return@withProfile) } }
                        .onFailure { Log.w("Activate after import: $it", it) }
                    refreshProfiles()
                }

                postMessage(context.getString(R.string.profile_imported))
            }.onFailure { error ->
                Log.w("Import profile: $error", error)
                postMessage(
                    error.message ?: context.getString(R.string.profile_import_failed),
                    isError = true,
                )
            }
        }
    }


    fun activateProfile(profile: Profile) {
        if (profile.active)
            return

        // A pending profile has no configuration yet, so activating it would leave the tunnel
        // pointing at nothing. Download it first, then activate in the same action — the previous
        // version emitted a "needs import" message and stopped, which left the user with a row that
        // could not be used at all.
        if (!profile.imported) {
            scope.launch {
                postMessage(context.getString(R.string.profile_importing))

                val imported = runCatching {
                    withProfile { commit(profile.uuid, null) }
                }

                if (imported.isFailure) {
                    val error = imported.exceptionOrNull()
                    Log.w("Import on activate: $error", error)
                    postMessage(
                        error?.message ?: context.getString(R.string.profile_import_failed),
                        isError = true,
                    )
                    return@launch
                }

                refreshProfiles()
                val refreshed = _profiles.value.profiles.firstOrNull { it.uuid == profile.uuid }
                if (refreshed != null) activateProfile(refreshed)
            }
            return
        }

        if (_profiles.value.switching != null)
            return

        scope.launch {
            _profiles.value = _profiles.value.copy(switching = profile.uuid)

            val result = runCatching {
                withProfile { setActive(profile) }
            }

            if (result.isFailure) {
                val error = result.exceptionOrNull()
                Log.w("Set active profile: $error", error)
                _profiles.value = _profiles.value.copy(switching = null)
                _messages.value = ChannelMessages.Text(
                    context.getString(
                        R.string.message_switch_failed,
                        error?.message ?: context.getString(R.string.state_unknown_error),
                    ),
                    isError = true,
                )
                return@launch
            }

            // The core reloads its configuration asynchronously and only then starts answering for
            // the new profile, so the list is refreshed after the reload rather than immediately.
            waitForProfileReload()

            _profiles.value = _profiles.value.copy(switching = null)

            refreshProfiles()
            syncTunnel()
            refreshProxies()

            _messages.value = ChannelMessages.ProfileSwitched(profile.name)
        }
    }

    /** Refreshes every imported URL/External profile, like `ProfilesDesign.Request.UpdateAll`. */
    fun updateAllProfiles() {
        if (_profiles.value.updatingAll)
            return

        scope.launch {
            _profiles.value = _profiles.value.copy(updatingAll = true)

            try {
                val targets = runCatching {
                    withProfile { queryAll() }
                }.getOrElse { emptyList() }
                    .filter { it.imported && it.type != Profile.Type.File }

                targets.forEach { profile ->
                    runCatching {
                        withProfile { update(profile.uuid) }
                    }.onFailure {
                        Log.w("Update profile ${profile.name}: $it", it)
                    }
                }
            } finally {
                _profiles.value = _profiles.value.copy(updatingAll = false)
                refreshProfiles()
            }
        }
    }

    /**
     * Waits until the core has finished loading the newly activated profile.
     *
     * `IProfileManager.setActive` returns once the profile manager has committed, but the clash
     * core reloads asynchronously. `ACTION_PROFILE_LOADED` is the framework's own "reload finished"
     * signal, so this waits for it (giving up after a few seconds) and then settles briefly, because
     * the broadcast can also fire just *before* `setActive` returns.
     */
    private suspend fun waitForProfileReload() {
        val seen = profileLoaded.value

        withTimeoutOrNull(PROFILE_RELOAD_TIMEOUT) {
            profileLoaded.first { it != seen }
        }

        delay(PROFILE_RELOAD_SETTLE)
    }

    // endregion

    // region Settings

    /**
     * Re-reads the settings this screen displays.
     *
     * These are plain `SharedPreferences`-backed values, so there is nothing to observe: they are
     * re-read whenever the screen is shown or the tunnel changes state.
     */
    fun refreshSettings() {
        scope.launch {
            val previous = _settings.value

            val profiles = runCatching {
                withProfile { queryAll() }
            }.getOrElse { emptyList() }

            _settings.value = previous.copy(
                appVersionName = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                        .versionName
                }.getOrNull() ?: previous.appVersionName,
                appVersionCode = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                        .let { info ->
                            @Suppress("DEPRECATION")
                            info.versionCode
                        }
                }.getOrDefault(previous.appVersionCode),
                kernelVersion = runCatching { Bridge.nativeCoreVersion() }.getOrNull(),
                enableVpn = uiStore.enableVpn,
                darkModeLabel = context.getString(darkModeLabel(uiStore.darkMode)),
                accessControlLabel = context.getString(accessControlLabel(serviceStore.accessControlMode)),
                profileCount = profiles.size,
                tunnelRunning = broadcasts.running.value,
            )
        }
    }

    private fun darkModeLabel(mode: DarkMode): Int = when (mode) {
        DarkMode.Auto -> R.string.settings_value_follow_system
        DarkMode.ForceLight -> R.string.settings_value_disabled
        DarkMode.ForceDark -> R.string.settings_value_enabled
    }

    private fun accessControlLabel(mode: AccessControlMode): Int = when (mode) {
        AccessControlMode.AcceptAll -> R.string.settings_value_enabled
        AccessControlMode.AcceptSelected -> R.string.settings_value_enabled
        AccessControlMode.DenySelected -> R.string.settings_value_disabled
    }

    // endregion

    private val uiStore by lazy { UiStore(context) }
    private val serviceStore by lazy { ServiceStore(context) }

    private enum class PendingToggle {
        Starting,
        Stopping,
    }

    private companion object {
        val PENDING_TIMEOUT = TimeUnit.SECONDS.toMillis(20)
        val TRAFFIC_INTERVAL = TimeUnit.SECONDS.toMillis(1)
        val PROFILE_RELOAD_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
        const val PROFILE_RELOAD_SETTLE = 300L
    }
}

/**
 * A one-shot notice from the controller to the UI.
 *
 * Deliberately free of `Context`: the controller resolves the user-facing text before emitting, so
 * the host only has to display what it is given and a test can assert on the message kind.
 */
sealed class ChannelMessages {
    /** Plain text to show in a snackbar. [isError] asks the host to present it as a failure. */
    data class Text(val text: String, val isError: Boolean = false) : ChannelMessages()

    /** The host must launch this intent and call [ClashUiController.onVpnPermissionGranted] on OK. */
    data class VpnPermissionRequired(val intent: Intent) : ChannelMessages()

    /** The profile has never been imported; the host should open its editor. */
    data class NeedImport(val uuid: UUID) : ChannelMessages()

    /** A profile switch completed; distinct from [Text] so the host can style it as a success. */
    data class ProfileSwitched(val name: String) : ChannelMessages()
}
