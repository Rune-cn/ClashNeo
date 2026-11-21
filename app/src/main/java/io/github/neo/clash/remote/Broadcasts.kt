package io.github.neo.clash.remote

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import io.github.neo.clash.common.compat.registerReceiverCompat
import io.github.neo.clash.common.constants.Intents
import io.github.neo.clash.common.log.Log
import java.util.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class Broadcasts(private val context: Application) {
    interface Observer {
        fun onServiceRecreated()
        fun onStarted()
        fun onStopped(cause: String?)
        fun onProfileChanged()
        fun onProfileUpdateCompleted(uuid: UUID?)
        fun onProfileUpdateFailed(uuid: UUID?, reason: String?)
        fun onProfileLoaded()
    }

    /**
     * The tunnel state as last reported by the service.
     *
     * This is a plain field for the XML UI, which re-reads it whenever a `BaseActivity.Event`
     * arrives. Compose cannot observe a plain field, though: a `@Composable` that reads it never
     * recomposes when the receiver writes it, which is exactly how the Miuix status card ended up
     * frozen on the first attempt at the Compose port. [running] is the observable mirror of this
     * field; it is assigned every time this property is assigned so the two can never disagree, and
     * every existing caller keeps working unchanged.
     */
    var clashRunning: Boolean = false
        set(value) {
            field = value
            runningState.value = value
        }

    private val runningState = MutableStateFlow(clashRunning)

    /** Observable view of [clashRunning], for Compose screens. */
    val running: StateFlow<Boolean> = runningState.asStateFlow()

    /**
     * The reason attached to the most recent stop, or `null` when the last stop had no cause.
     *
     * Kept as a field (as well as being part of [TunnelEvent.Stopped]) so a screen that attaches
     * after the stop can still explain why the tunnel is down, instead of only being able to say
     * that it is.
     */
    var lastStopCause: String? = null
        private set

    /**
     * Per-tunnel-session counters incremented on every start/stop edge.
     *
     * The XML UI learns about state changes through `Observer` callbacks, which are only delivered
     * to observers registered at the time. Compose needs the event itself (not only the resulting
     * boolean) so a screen can tell "just started" apart from "already running", and so the
     * controller can skip re-reading the core when it is told about a stop it caused itself.
     */
    private val events = MutableSharedFlow<TunnelEvent>(extraBufferCapacity = 16)

    /** Observable tunnel lifecycle events, for screens that need more than the boolean. */
    val tunnelEvents: SharedFlow<TunnelEvent> = events.asSharedFlow()

    private var registered = false
    private val receivers = mutableListOf<Observer>()
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.`package` != context?.packageName)
                return

            when (intent?.action) {
                Intents.ACTION_SERVICE_RECREATED -> {
                    clashRunning = false
                    lastStopCause = null
                    emit(TunnelEvent.Stopped(null))

                    receivers.forEach {
                        it.onServiceRecreated()
                    }
                }
                Intents.ACTION_CLASH_STARTED -> {
                    clashRunning = true
                    lastStopCause = null
                    emit(TunnelEvent.Started)

                    receivers.forEach {
                        it.onStarted()
                    }
                }
                Intents.ACTION_CLASH_STOPPED -> {
                    val cause = intent.getStringExtra(Intents.EXTRA_STOP_REASON)

                    clashRunning = false
                    lastStopCause = cause
                    emit(TunnelEvent.Stopped(cause))

                    receivers.forEach {
                        it.onStopped(cause)
                    }
                }
                Intents.ACTION_PROFILE_CHANGED -> {
                    emit(TunnelEvent.ProfileChanged)

                    receivers.forEach {
                        it.onProfileChanged()
                    }
                }
                Intents.ACTION_PROFILE_UPDATE_COMPLETED ->
                    receivers.forEach {
                        it.onProfileUpdateCompleted(
                            UUID.fromString(intent.getStringExtra(Intents.EXTRA_UUID)))
                    }
                Intents.ACTION_PROFILE_UPDATE_FAILED ->
                    receivers.forEach {
                        it.onProfileUpdateFailed(
                            UUID.fromString(intent.getStringExtra(Intents.EXTRA_UUID)),
                            intent.getStringExtra(Intents.EXTRA_FAIL_REASON))
                    }
                Intents.ACTION_PROFILE_LOADED -> {
                    emit(TunnelEvent.ProfileLoaded)

                    receivers.forEach {
                        it.onProfileLoaded()
                    }
                }
            }
        }
    }

    fun addObserver(observer: Observer) {
        receivers.add(observer)
    }

    fun removeObserver(observer: Observer) {
        receivers.remove(observer)
    }

    /**
     * Starts listening for the service's self-broadcasts.
     *
     * Idempotent: `Remote` calls this on every "app becomes visible" and only the first call binds
     * the receiver. The tunnel state is seeded from [StatusClient] rather than assumed `false` so a
     * freshly attached screen is correct before the first broadcast arrives.
     */
    fun register() {
        if (registered)
            return

        try {
            context.registerReceiverCompat(broadcastReceiver, IntentFilter().apply {
                addAction(Intents.ACTION_SERVICE_RECREATED)
                addAction(Intents.ACTION_CLASH_STARTED)
                addAction(Intents.ACTION_CLASH_STOPPED)
                addAction(Intents.ACTION_PROFILE_CHANGED)
                addAction(Intents.ACTION_PROFILE_UPDATE_COMPLETED)
                addAction(Intents.ACTION_PROFILE_UPDATE_FAILED)
                addAction(Intents.ACTION_PROFILE_LOADED)
            })

            registered = true

            // Seeded from the status provider rather than assumed, so a screen attached after the
            // app was killed still shows the tunnel as running. This assignment happens after
            // `registered` is set, so a start/stop broadcast that lands in between is applied on
            // top of the probe instead of being overwritten by it.
            clashRunning = StatusClient(context).currentProfile() != null
        } catch (e: Exception) {
            Log.w("Register global receiver: $e", e)

            registered = false
        }
    }

    fun unregister() {
        if (!registered)
            return

        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            Log.w("Unregister global receiver: $e", e)
        } finally {
            // Cleared even when `unregisterReceiver` throws, so the next `register()` is never
            // skipped by a stale flag: without this the receiver would be dead while the flag says
            // it is live, which is the worst possible state for a status card.
            registered = false
            clashRunning = false
        }
    }

    private fun emit(event: TunnelEvent) {
        events.tryEmit(event)
    }

    /**
     * Lifecycle of the tunnel as the service reported it.
     *
     * The boolean [running] answers "is it up"; these events answer "what just happened", which is
     * what a screen needs in order to refresh the data that only exists while the core is up.
     */
    sealed class TunnelEvent {
        object Started : TunnelEvent()

        /** [cause] is the core's failure message when it exited abnormally, else `null`. */
        data class Stopped(val cause: String?) : TunnelEvent()

        /** The profile manager changed the active profile (may happen while stopped). */
        object ProfileChanged : TunnelEvent()

        /** The core finished loading the active profile's configuration. */
        object ProfileLoaded : TunnelEvent()
    }
}
