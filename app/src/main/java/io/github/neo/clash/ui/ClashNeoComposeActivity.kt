// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import io.github.neo.clash.BuildConfig
import kotlin.math.absoluteValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.neo.clash.LogcatActivity
import io.github.neo.clash.LogcatService
import io.github.neo.clash.LogsActivity
import io.github.neo.clash.NetworkSettingsActivity
import io.github.neo.clash.R
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
import io.github.neo.clash.ui.page.theme.ThemeSettingsPage
import io.github.neo.clash.ui.page.theme.ThemeMode
import io.github.neo.clash.ui.page.theme.ThemeUiState
import io.github.neo.clash.ui.theme.ClashNeoTheme
import io.github.neo.clash.ui.widget.MiuixComponentHost
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState

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
                    is ChannelMessages.VpnPermissionRequired ->
                        vpnPermissionLauncher.launch(message.intent)
                    else -> Unit
                }
            }
        }

        controller.refreshAll()

        setContent {
            // Hoisted above ClashNeoTheme: the theme has to be able to read the settings that it
            // is about to apply, so the state cannot live inside the themed subtree.
            var themeState by remember { mutableStateOf(ThemeUiState()) }

            ClashNeoTheme(
                useOs4 = themeState.useMiuixOs4,
                darkTheme = themeState.themeMode.resolveDark(),
            ) {
                ClashNeoApp(
                    controller = controller,
                    themeState = themeState,
                    onThemeStateChange = { themeState = it },
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
 * forces a branch in the `when` below instead of silently rendering nothing. The label is a
 * resource id, not a literal, so the tab bar follows the device language.
 */
/** Sub-pages layered above the tab pager. */
private enum class SubPage {
    None,
    About,
    Theme,
}

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    Control(R.string.tab_control, Icons.Rounded.Dashboard),
    Proxy(R.string.tab_proxy, Icons.Rounded.Dns),
    Profiles(R.string.tab_profiles, Icons.Rounded.Description),
    Settings(R.string.tab_settings, Icons.Rounded.Settings),
}

/**
 * Root layout: a horizontally swipeable pager plus the bottom navigation.
 *
 * A pager (rather than a `when`) matches the reference UI and makes the tabs feel like siblings:
 * swiping between them is continuous, and the back gesture can drive the pager without a special
 * case. State is hoisted from [ClashUiController] with `collectAsStateWithLifecycle`, so the flows
 * stop being observed while the activity is stopped and resume with the latest value on return.
 */
@Composable
private fun ClashNeoApp(
    controller: ClashUiController,
    themeState: ThemeUiState,
    onThemeStateChange: (ThemeUiState) -> Unit,
    onOpenLogs: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { Tab.entries.size })
    val scope = rememberCoroutineScope()
    // Sub-pages that live above the tabs. An enum (not a boolean per page) keeps the back/return
    // behaviour identical for every one of them.
    var subPage by remember { mutableStateOf(SubPage.None) }

    val snackbarHostState = remember { SnackbarHostState() }

    val tunnel by controller.tunnel.collectAsStateWithLifecycle()
    val proxies by controller.proxies.collectAsStateWithLifecycle()
    val profiles by controller.profiles.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        controller.messages.collectLatest { message ->
            when (message) {
                is ChannelMessages.Text -> snackbarHostState.showSnackbar(message.text)

                is ChannelMessages.NeedImport ->
                    snackbarHostState.showSnackbar(context.getString(R.string.message_import_first))

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

    val useBlur = true

    MiuixComponentHost(
        useOs4 = false,
        useBlur = useBlur,
    ) { scopeState ->
        Scaffold(
            bottomBar = {
                MiuixBottomBar(
                    selected = pagerState.currentPage,
                    onSelect = { index ->
                        subPage = SubPage.None
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        ) { padding ->
            when (subPage) {
                SubPage.About -> {
                    AboutPage(
                        state = AboutUiState(
                            appName = stringResource(R.string.app_display_name),
                            versionName = BuildConfig.VERSION_NAME,
                            versionCode = BuildConfig.VERSION_CODE,
                        ),
                        modifier = Modifier.fillMaxSize(),
                        outerPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        onBack = { subPage = SubPage.None },
                    )
                    return@Scaffold
                }

                SubPage.Theme -> {
                    ThemeSettingsPage(
                        state = themeState,
                        onStateChange = onThemeStateChange,
                        modifier = Modifier.fillMaxSize(),
                        outerPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        onBack = { subPage = SubPage.None },
                    )
                    return@Scaffold
                }

                SubPage.None -> Unit
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(scopeState.contentBackdropModifier),
                // No content padding here: each page receives the bottom inset as `outerPadding`
                // and appends it to its own scroll content padding. Doing both counted the bar
                // height twice and left a dead gap at the end of every list.
                contentPadding = PaddingValues(),
                beyondViewportPageCount = 1,
                overscrollEffect = null,
            ) { page ->
                // Pages scale down slightly while off-centre, which gives the swipe a sense of
                // depth without the cost of a full page transform.
                val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val scale by animateFloatAsState(
                    targetValue = (1f - (offset.absoluteValue / 8f)).coerceIn(0.92f, 1f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "page-scale",
                )

                val pageOuterPadding = PaddingValues(
                    bottom = padding.calculateBottomPadding(),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                ) {
                    when (Tab.entries[page]) {
                        Tab.Control -> ControlPage(
                            state = tunnel,
                            modifier = Modifier.fillMaxSize(),
                            outerPadding = pageOuterPadding,
                            onToggle = controller::toggleTunnel,
                            onRefresh = controller::refreshAll,
                            onOpenLogs = onOpenLogs,
                            onOpenNetworkSettings = {
                                context.startActivity(NetworkSettingsActivity::class.intent)
                            },
                        )

                        Tab.Proxy -> ProxyPage(
                            state = proxies,
                            modifier = Modifier.fillMaxSize(),
                            outerPadding = pageOuterPadding,
                            onSelect = controller::selectProxy,
                        )

                        Tab.Profiles -> ProfilesPage(
                            state = profiles,
                            modifier = Modifier.fillMaxSize(),
                            outerPadding = pageOuterPadding,
                            onActivate = controller::activateProfile,
                        )

                        Tab.Settings -> SettingsPage(
                            state = SettingsUiState(),
                            modifier = Modifier.fillMaxSize(),
                            outerPadding = pageOuterPadding,
                            onNavigate = { route ->
                                when (route) {
                                    SettingsRoute.About -> subPage = SubPage.About
                                    SettingsRoute.Theme -> subPage = SubPage.Theme
                                    SettingsRoute.Network ->
                                        context.startActivity(NetworkSettingsActivity::class.intent)
                                    else ->
                                        context.startActivity(SettingsActivity::class.intent)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Resolve a [ThemeMode] to the boolean the theme composable wants.
 *
 * Kept here rather than inside the theme so the enum stays a pure value object that can be
 * persisted without dragging Compose into it.
 */
@Composable
private fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

/** Bottom navigation bar shared by every tab. */
@Composable
private fun MiuixBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    NavigationBar {
        Tab.entries.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = tab.icon,
                label = stringResource(tab.labelRes),
            )
        }
    }
}
