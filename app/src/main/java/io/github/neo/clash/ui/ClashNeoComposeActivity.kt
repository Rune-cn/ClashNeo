// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.neo.clash.LogsActivity
import io.github.neo.clash.R
import io.github.neo.clash.LogcatActivity
import io.github.neo.clash.LogcatService
import io.github.neo.clash.NetworkSettingsActivity
import io.github.neo.clash.SettingsActivity
import io.github.neo.clash.common.util.intent
import io.github.neo.clash.ui.page.about.AboutPage
import io.github.neo.clash.ui.page.about.AboutUiState
import io.github.neo.clash.ui.page.control.ControlPage
import io.github.neo.clash.ui.page.profiles.ProfilesPage
import io.github.neo.clash.ui.page.proxy.ProxyPage
import io.github.neo.clash.ui.page.settings.SettingsPage
import io.github.neo.clash.ui.page.settings.SettingsRoute
import io.github.neo.clash.ui.page.settings.SettingsUiState
import io.github.neo.clash.ui.theme.ClashNeoTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The application's main screen.
 *
 * This replaced `BaseActivity<MainDesign>` as the launcher target. The `BaseActivity` loop could not
 * be reused because it is built around `Design`/XML inflation and a `select` loop over `requests`;
 * [ClashUiController] performs the equivalent role for Compose, exposing `StateFlow`s instead.
 *
 * The legacy XML screens are still the implementation for the deeper settings pages, so this
 * activity launches them by class rather than reimplementing them.
 */
class ClashNeoComposeActivity : ComponentActivity() {

    private lateinit var controller: ClashUiController

    /**
     * Launches the VPN consent dialog and reports the answer back to the controller.
     *
     * Registered before `onStart` (which is when `setContent` runs) because the controller can ask
     * for consent during its very first refresh.
     */
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            controller.onVpnPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = ClashUiController(this, lifecycleScope)
        controller.start()

        lifecycleScope.launch {
            controller.messages.collectLatest { message ->
                when (message) {
                    is ChannelMessages.VpnPermissionRequired -> vpnPermissionLauncher.launch(message.intent)
                    else -> Unit
                }
            }
        }

        controller.refreshAll()

        setContent {
            ClashNeoTheme {
                ClashNeoApp(
                    controller = controller,
                    onOpenLogs = { openLogs() },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // The service is started from outside this screen too (quick settings tile), so the state
        // is re-read on every appearance rather than trusting what was last rendered.
        controller.refreshAll()
    }

    /**
     * Routes to the log viewer, preferring the live logcat screen when the log service is up.
     *
     * Mirrors `MainActivity`'s choice so the two entry points cannot disagree about which log
     * screen a user lands on.
     */
    private fun openLogs() {
        val target = if (LogcatService.running) LogcatActivity::class else LogsActivity::class
        startActivity(target.intent)
    }
}

/**
 * Tabs of the bottom navigation, in the order the user specified.
 *
 * An enum rather than a list of lambdas keeps the badge/selected logic exhaustive: adding a tab
 * forces a branch in the `when` below instead of silently rendering nothing.
 */
private enum class Tab(val label: String, val icon: ImageVector) {
    Control("控制", Icons.Rounded.Dashboard),
    Proxy("代理", Icons.Rounded.Dns),
    Profiles("配置", Icons.Rounded.Description),
    Settings("设置", Icons.Rounded.Settings),
}

/**
 * Root layout: bottom navigation plus the selected tab.
 *
 * State is hoisted from [ClashUiController] with `collectAsStateWithLifecycle`, so the flows stop
 * being observed while the activity is stopped and resume with the latest value on return.
 */
@Composable
private fun ClashNeoApp(
    controller: ClashUiController,
    onOpenLogs: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(Tab.Control) }
    var showAbout by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val tunnel by controller.tunnel.collectAsStateWithLifecycle()
    val proxies by controller.proxies.collectAsStateWithLifecycle()
    val profiles by controller.profiles.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        controller.messages.collectLatest { message ->
            when (message) {
                is ChannelMessages.Text ->
                    snackbarHostState.showSnackbar(message.text)

                is ChannelMessages.NeedImport ->
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.message_import_first),
                    )

                is ChannelMessages.ProfileSwitched ->
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.message_switch_succeeded, message.name),
                    )

                // Handled by the activity, which owns the VPN launcher.
                is ChannelMessages.VpnPermissionRequired -> Unit

                null -> Unit
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            showAbout = false
                        },
                        icon = tab.icon,
                        label = tab.label,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)

        if (showAbout) {
            AboutPage(
                state = AboutUiState(
                    appName = "ClashNeo",
                    versionName = io.github.neo.clash.BuildConfig.VERSION_NAME,
                    versionCode = io.github.neo.clash.BuildConfig.VERSION_CODE,
                ),
                modifier = contentModifier,
            )
            return@Scaffold
        }

        when (selectedTab) {
            Tab.Control -> ControlPage(
                state = tunnel,
                modifier = contentModifier,
                onToggle = controller::toggleTunnel,
                onRefresh = controller::refreshAll,
                onOpenLogs = onOpenLogs,
                onOpenNetworkSettings = { context.startActivity(NetworkSettingsActivity::class.intent) },
            )

            Tab.Proxy -> ProxyPage(
                state = proxies,
                modifier = contentModifier,
                onSelect = controller::selectProxy,
            )

            Tab.Profiles -> ProfilesPage(
                state = profiles,
                modifier = contentModifier,
                onActivate = controller::activateProfile,
            )

            Tab.Settings -> SettingsPage(
                state = SettingsUiState(),
                modifier = contentModifier,
                onNavigate = { route ->
                    when (route) {
                        SettingsRoute.About -> showAbout = true
                        SettingsRoute.Network -> context.startActivity(NetworkSettingsActivity::class.intent)
                        else -> context.startActivity(SettingsActivity::class.intent)
                    }
                },
            )
        }
    }
}
