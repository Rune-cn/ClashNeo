package io.github.neo.clash.util

import android.content.Context
import android.content.Intent
import android.net.VpnService
import io.github.neo.clash.common.compat.startForegroundServiceCompat
import io.github.neo.clash.common.constants.Intents
import io.github.neo.clash.common.util.intent
import io.github.neo.clash.design.store.UiStore
import io.github.neo.clash.service.ClashService
import io.github.neo.clash.service.TunService
import io.github.neo.clash.service.util.sendBroadcastSelf

fun Context.startClashService(): Intent? {
    val startTun = UiStore(this).enableVpn

    if (startTun) {
        val vpnRequest = VpnService.prepare(this)
        if (vpnRequest != null)
            return vpnRequest

        startForegroundServiceCompat(TunService::class.intent)
    } else {
        startForegroundServiceCompat(ClashService::class.intent)
    }

    return null
}

fun Context.stopClashService() {
    sendBroadcastSelf(Intent(Intents.ACTION_CLASH_REQUEST_STOP))
}