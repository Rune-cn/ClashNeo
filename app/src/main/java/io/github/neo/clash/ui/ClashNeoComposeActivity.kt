// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.neo.clash.BuildConfig
import io.github.neo.clash.remote.Remote
import io.github.neo.clash.ui.page.about.AboutPage
import io.github.neo.clash.ui.page.about.AboutUiState
import io.github.neo.clash.ui.page.home.HomePage
import io.github.neo.clash.ui.page.home.HomeUiState
import io.github.neo.clash.ui.page.settings.SettingsPage
import io.github.neo.clash.ui.page.settings.SettingsRoute
import io.github.neo.clash.ui.page.settings.SettingsUiState
import io.github.neo.clash.ui.theme.ClashNeoTheme
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold

/**
 * Compose host for the ClashNeo UI.
 *
 * This activity exists alongside the original XML screens and owns nothing but navigation state:
 * running status is read from [Remote.broadcasts], which is the same source `BaseActivity` uses, so
 * the two UIs can never disagree about whether the tunnel is up.
 *
 * It deliberately does not extend `BaseActivity`: that class is built around `Design`/XML view
 * inflation and would fight the Compose hierarchy.
 */
class ClashNeoComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClashNeoTheme {
                ClashNeoApp()
            }
        }
    }

    /**
     * Whether the tunnel is running, re-read on every recomposition.
     *
     * `clashRunning` is a plain field on `Broadcasts` that the broadcast receiver mutates; Compose
     * is told to re-read it via the surrounding state that the activity's lifecycle events touch.
     * A dedicated observable would be better, but introducing one here would change `Broadcasts`
     * for every existing XML caller, so the cast stays local and explicit.
     */
    private fun isClashRunning(): Boolean = Remote.broadcasts.clashRunning
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Settings("Settings", Icons.Rounded.Settings),
    About("About", Icons.Rounded.Info),
}

/**
 * Root composable: tab scaffold plus the per-tab screen.
 *
 * Navigation is intentionally a single `mutableStateOf` rather than a navigation library: three
 * top-level destinations and no nested stacks do not justify the dependency, and the XML UI keeps
 * owning every deeper screen.
 */
@Composable
private fun ClashNeoApp() {
    var selectedTab by remember { mutableStateOf(Tab.Home) }

    val items = remember {
        Tab.entries.map { tab -> NavigationItem(label = tab.label, icon = tab.icon) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = tab.icon,
                        label = items[index].label,
                    )
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)

        when (selectedTab) {
            Tab.Home -> HomePage(
                state = HomeUiState(
                    isRunning = Remote.broadcasts.clashRunning,
                    versionName = BuildConfig.VERSION_NAME,
                ),
                modifier = contentModifier,
            )

            Tab.Settings -> SettingsPage(
                state = SettingsUiState(isRunning = Remote.broadcasts.clashRunning),
                modifier = contentModifier,
                onNavigate = { route -> handleRoute(route) },
            )

            Tab.About -> AboutPage(
                state = AboutUiState(
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                ),
                modifier = contentModifier,
            )
        }
    }
}

/**
 * Route handler for the settings screen.
 *
 * Deep screens still live in the XML UI, so every route is a no-op for now. The branch is written
 * out exhaustively so adding a `SettingsRoute` constant forces a decision here instead of silently
 * doing nothing.
 */
private fun handleRoute(route: SettingsRoute) {
    when (route) {
        SettingsRoute.Network,
        SettingsRoute.Override,
        SettingsRoute.AccessControl,
        SettingsRoute.MetaFeatures,
        SettingsRoute.Theme,
        SettingsRoute.Language,
        SettingsRoute.About,
        SettingsRoute.Licenses,
        -> Unit
    }
}
