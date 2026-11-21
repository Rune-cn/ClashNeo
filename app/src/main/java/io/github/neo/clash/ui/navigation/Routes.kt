// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys.
 *
 * The back stack is a list of these, which is what lets `NavDisplay` own the transition and the
 * predictive-back gesture. A plain `enum` of sub-pages — the previous approach — cannot be a back
 * stack: there is nothing to pop, so the system has no gesture to drive and the whole screen just
 * disappears on release.
 */
@Serializable
sealed interface Route : NavKey {
    /** The tab shell. Always the bottom of the stack. */
    @Serializable
    data object Main : Route

    @Serializable
    data object About : Route

    @Serializable
    data object Theme : Route

    @Serializable
    data object Network : Route

    @Serializable
    data object AccessControl : Route

    @Serializable
    data object Override : Route

    @Serializable
    data object AppSettings : Route

    /** @param uuid identifies which profile is being edited. */
    @Serializable
    data class Properties(val uuid: String) : Route

    @Serializable
    data object Providers : Route

    @Serializable
    data object Logs : Route

    @Serializable
    data object Logcat : Route

    @Serializable
    data object NewProfile : Route

    @Serializable
    data object Files : Route

    @Serializable
    data object MetaFeatures : Route
}
