// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.util

import android.content.res.Resources
import io.github.neo.clash.R
import io.github.neo.clash.core.model.Traffic
import io.github.neo.clash.core.util.trafficDownload
import io.github.neo.clash.core.util.trafficTotal
import io.github.neo.clash.core.util.trafficUpload
import java.util.concurrent.TimeUnit

/**
 * Human-readable upload / download / total for a packed traffic counter.
 *
 * The core hands out one `Long` with the direction counts in separate bit fields, so rendering a
 * card needs three decodings of the same value. Doing it once here keeps every screen from having
 * to know the layout, and keeps the MiB/KiB formatting consistent with `MainDesign.setForwarded`.
 */
data class TrafficSummary(
    val upload: String,
    val download: String,
    val total: String,
) {
    companion object {
        fun of(traffic: Traffic): TrafficSummary = TrafficSummary(
            upload = traffic.trafficUpload(),
            download = traffic.trafficDownload(),
            total = traffic.trafficTotal(),
        )
    }
}

/**
 * Formats a profile's `updatedAt` timestamp as a relative age.
 *
 * Profile ages are shown as "5 min" rather than absolute dates because that is the only thing a
 * user acts on (did the subscription refresh?), and the XML UI's profile list uses the same
 * granularity, so both lists read the same.
 *
 * Takes [resources] rather than reading from a `Context` so the unit strings stay localisable: a
 * hard-coded "min" would be the only untranslated word on an otherwise translated screen.
 *
 * @return null when [updatedAt] is non-positive, which means "never updated".
 */
fun formatElapsed(
    updatedAt: Long,
    resources: Resources,
    now: Long = System.currentTimeMillis(),
): String? {
    // `updatedAt` arrives from the service, where 0 is the "never" sentinel. Negative values would
    // mean clock skew rather than a real timestamp, so both are treated as "unknown".
    if (updatedAt <= 0L)
        return null

    val elapsed = (now - updatedAt).coerceAtLeast(0L)

    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val days = TimeUnit.MILLISECONDS.toDays(elapsed)

    return when {
        minutes < 1 -> resources.getString(R.string.elapsed_less_than_minute)
        minutes < 60 -> resources.getString(R.string.elapsed_minutes, minutes)
        hours < 24 -> resources.getString(R.string.elapsed_hours, hours)
        days < 30 -> resources.getString(R.string.elapsed_days, days)
        days < 365 -> resources.getString(R.string.elapsed_months, days / 30)
        else -> resources.getString(R.string.elapsed_years, days / 365)
    }
}
