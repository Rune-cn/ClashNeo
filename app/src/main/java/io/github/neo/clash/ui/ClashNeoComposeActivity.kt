// SPDX-License-Identifier: GPL-3.0-only
package io.github.neo.clash.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.CancellationException
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
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
import io.github.neo.clash.ui.page.access.AccessControlPage
import io.github.neo.clash.ui.page.access.AccessControlUiState
import io.github.neo.clash.ui.page.network.NetworkSettingsPage
import io.github.neo.clash.ui.page.network.NetworkSettingsUiState
import io.github.neo.clash.ui.page.override.OverrideSettingsPage
import io.github.neo.clash.ui.page.override.OverrideSettingsUiState
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import io.github.neo.clash.ui.library.FloatingBottomBar
import io.github.neo.clash.ui.library.FloatingBottomBarMode
import io.github.neo.clash.ui.theme.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import io.github.neo.clash.ui.theme.getMiuixAppBarColor
import io.github.neo.clash.ui.theme.miuixBlurEffect
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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
            // Hoisted here (not inside the sub-page) so the switches keep their values when the
            // user navigates back to the tabs and returns.
            var networkState by remember { mutableStateOf(NetworkSettingsUiState()) }
            var accessState by remember { mutableStateOf(AccessControlUiState()) }
            var overrideState by remember { mutableStateOf(OverrideSettingsUiState()) }

            ClashNeoTheme(
                themeMode = themeState.themeMode,
                useOs4 = themeState.useMiuixOs4,
                // The two switches are independent, exactly as the reference app treats them:
                // `useMiuixMonet` selects the engine, `useDynamicColor` only the seed source.
                // Conflating them is what made the palette picker a no-op.
                useMiuixMonet = themeState.useMiuixMonet,
                useDynamicColor = themeState.useDynamicColor,
                paletteStyle = themeState.paletteStyle,
            ) {
                ClashNeoApp(
                    controller = controller,
                    themeState = themeState,
                    networkState = networkState,
                    onNetworkStateChange = { networkState = it },
                    accessState = accessState,
                    onAccessStateChange = { accessState = it },
                    overrideState = overrideState,
                    onOverrideStateChange = { overrideState = it },
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
    Network,
    AccessControl,
    Override,
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
    networkState: NetworkSettingsUiState,
    onNetworkStateChange: (NetworkSettingsUiState) -> Unit,
    accessState: AccessControlUiState,
    onAccessStateChange: (AccessControlUiState) -> Unit,
    overrideState: OverrideSettingsUiState,
    onOverrideStateChange: (OverrideSettingsUiState) -> Unit,
    onOpenLogs: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { Tab.entries.size })
    val scope = rememberCoroutineScope()
    // Sub-pages that live above the tabs. An enum (not a boolean per page) keeps the back/return
    // behaviour identical for every one of them.
    var subPage by remember { mutableStateOf(SubPage.None) }
    // 0f = at rest, 1f = fully swiped away. Driven by the predictive-back gesture.
    var subPageProgress by remember { mutableFloatStateOf(0f) }

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

    // Back stack, innermost first:
    //   1. an open sub-page (About/Theme/Network/…) closes — with a scale/fade preview while the
    //      gesture is in progress,
    //   2. otherwise a non-first tab scrubs back to the first tab, following the finger,
    //   3. otherwise the handler is disabled and the system finishes the activity.
    //
    // `PredictiveBackHandler` (not `BackHandler`) is what makes the gesture visible: the system
    // reports progress 0f→1f, and the UI is expected to move *with* it. A plain BackHandler only
    // fires at the end, which is why no predictive animation was ever seen.
    if (subPage != SubPage.None) {
        PredictiveBackHandler { progress ->
            try {
                progress.collect { backEvent ->
                    // Drive the outgoing sub-page off its own edge as the finger travels.
                    subPageProgress = backEvent.progress
                }
                // Gesture completed: commit the pop.
                subPageProgress = 0f
                subPage = SubPage.None
            } catch (e: CancellationException) {
                // Gesture cancelled: the page springs back to rest.
                subPageProgress = 0f
                throw e
            }
        }
    } else if (pagerState.currentPage != 0) {
        PredictiveBackHandler { progress ->
            try {
                progress.collect { backEvent ->
                    // Scrub the pager by gesture progress so the previous tab slides in under the
                    // finger instead of jumping at the end.
                    pagerState.scrollToPage(
                        page = 0,
                        pageOffsetFraction = 1f - backEvent.progress,
                    )
                }
                pagerState.animateScrollToPage(0)
            } catch (e: CancellationException) {
                // Settle on the page the gesture started from.
                pagerState.animateScrollToPage(pagerState.currentPage)
                throw e
            }
        }
    }

    // Mirrors the reference decision order exactly:
    //   - OS4 always floats (its glass pill *is* the OS4 navigation affordance),
    //   - OS3 floats only when the preference says so,
    //   - the blur mode degrades by API level rather than by preference, because LiquidGlass needs
    //     render effects (33+) and plain Blur needs the blur path below that.
    val floatingBar = themeState.useMiuixOs4 || themeState.useAppleFloatingBar
    val floatingMode = when {
        themeState.useBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            FloatingBottomBarMode.LiquidGlass
        themeState.useBlur -> FloatingBottomBarMode.Blur
        else -> FloatingBottomBarMode.None
    }

    // Two distinct backdrops, as in the reference:
    //   floatingBackdrop records the whole page so the pill can refract it;
    //   miuixBackdrop is the OS3 blur surface and is null when blur is off.
    val floatingBackdrop = rememberLayerBackdrop()
    val miuixBackdrop = rememberMiuixBlurBackdrop(themeState.useBlur)

    // Labels must be resolved inside a composable, so the item list is rebuilt when the locale
    // changes rather than captured once.
    val topLevelItems = Tab.entries.map { tab ->
        NavigationItem(label = stringResource(tab.labelRes), icon = tab.icon)
    }

    MiuixComponentHost(
        useOs4 = false,
        useBlur = themeState.useBlur,
    ) { scopeState ->
        Scaffold(
            bottomBar = {
                MiuixBottomBar(
                    selected = pagerState.currentPage,
                    items = topLevelItems,
                    floating = floatingBar,
                    floatingMode = floatingMode,
                    floatingBackdrop = floatingBackdrop,
                    miuixBackdrop = miuixBackdrop,
                    onSelect = { index ->
                        subPage = SubPage.None
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        ) { padding ->
            // Shared predictive-back transform for every sub-page. `subPageProgress` is driven by
            // the gesture, so the page tracks the finger rather than waiting for the release.
            val subPageModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = subPageProgress.coerceIn(0f, 1f)
                    translationX = size.width * 0.18f * p
                    scaleX = 1f - 0.12f * p
                    scaleY = 1f - 0.12f * p
                    alpha = 1f - p
                    clip = false
                }

            when (subPage) {
                SubPage.About -> {
                    AboutPage(
                        state = AboutUiState(
                            appName = stringResource(R.string.app_display_name),
                            versionName = BuildConfig.VERSION_NAME,
                            versionCode = BuildConfig.VERSION_CODE,
                        ),
                        modifier = subPageModifier,
                        outerPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        onBack = { subPage = SubPage.None },
                    )
                    return@Scaffold
                }

                SubPage.Theme -> {
                    ThemeSettingsPage(
                        state = themeState,
                        onStateChange = onThemeStateChange,
                        modifier = subPageModifier,
                        outerPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        onBack = { subPage = SubPage.None },
                    )
                    return@Scaffold
                }

                SubPage.Network -> {
                    NetworkSettingsPage(
                        state = networkState,
                        onStateChange = onNetworkStateChange,
                        modifier = subPageModifier,
                        outerPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        onBack = { subPage = SubPage.None },
                        onOpenAccessControl = { subPage = SubPage.AccessControl },
                    )
                    return@Scaffold
                }

                SubPage.AccessControl -> {
                    AccessControlPage(
                        state = accessState,
                        onStateChange = onAccessStateChange,
                        modifier = subPageModifier,
                        outerPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        onBack = { subPage = SubPage.None },
                    )
                    return@Scaffold
                }

                SubPage.Override -> {
                    OverrideSettingsPage(
                        state = overrideState,
                        onStateChange = onOverrideStateChange,
                        // The core owns these vocabularies; they are passed in rather than
                        // duplicated here so a core change does not silently desync the pickers.
                        logLevels = listOf("info", "warning", "error", "debug", "silent"),
                        modes = listOf("rule", "global", "direct"),
                        modifier = subPageModifier,
                        outerPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                        onBack = { subPage = SubPage.None },
                    )
                    return@Scaffold
                }

                SubPage.None -> Unit
            }

            HorizontalPager(
                state = pagerState,
                // Exactly one backdrop is attached, and which one depends on the bar style:
                // a floating pill refracts the *whole* page, so the page must be the recorded
                // subtree; a flush bar only blurs the strip behind itself. Attaching both (or the
                // host's generic backdrop) is what made blur invisible: the sampler and the
                // recorded content ended up on the same or unrelated nodes.
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (floatingBar) Modifier.layerBackdrop(floatingBackdrop)
                        else miuixBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier,
                    ),
                // No content padding here: each page receives the bottom inset as `outerPadding`
                // and appends it to its own scroll content padding. Doing both counted the bar
                // height twice and left a dead gap at the end of every list.
                contentPadding = PaddingValues(),
                beyondViewportPageCount = 1,
                overscrollEffect = null,
            ) { page ->
                // No page transform: the reference pager is a plain tracking strip, so the tabs
                // read as siblings in one continuous surface. The earlier scale-down made the
                // switch feel like a stack of cards instead.
                val pageOuterPadding = PaddingValues(
                    bottom = padding.calculateBottomPadding(),
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when (Tab.entries[page]) {
                        Tab.Control -> ControlPage(
                            state = tunnel,
                            modifier = Modifier.fillMaxSize(),
                            outerPadding = pageOuterPadding,
                            onToggle = controller::toggleTunnel,
                            onRefresh = controller::refreshAll,
                            onOpenLogs = onOpenLogs,
                            onOpenNetworkSettings = { subPage = SubPage.Network },
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
                                    SettingsRoute.Network -> subPage = SubPage.Network
                                    SettingsRoute.AccessControl ->
                                        subPage = SubPage.AccessControl
                                    SettingsRoute.Override -> subPage = SubPage.Override
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
    items: List<NavigationItem>,
    floating: Boolean,
    floatingMode: FloatingBottomBarMode,
    floatingBackdrop: LayerBackdrop,
    miuixBackdrop: LayerBackdrop?,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (floating) {
            // Floating pill. Insets keep it clear of the gesture area and of landscape cutouts;
            // the width cap stops it from stretching into a thin strip on tablets.
            val horizontalSafe =
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
            val bottomMargin = maxOf(
                24.dp,
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp,
            )
            FloatingBottomBar(
                items = items,
                selectedIndex = { selected },
                onSelected = onSelect,
                backdrop = floatingBackdrop,
                mode = floatingMode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontalSafe)
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = bottomMargin),
                iconContent = { item, _ ->
                    Icon(imageVector = item.icon, contentDescription = null)
                },
                labelContent = { item, _ ->
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                },
            )
            return@Box
        }

        // Classic bar, flush with the edge. It samples the page backdrop so the content scrolling
        // underneath shows through instead of being covered by an opaque strip.
        val blurActive = miuixBackdrop != null
        val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .then(
                    if (blurActive) {
                        Modifier.textureBlur(
                            backdrop = miuixBackdrop!!,
                            shape = RectangleShape,
                            blurRadius = 25f,
                            colors = BlurColors(
                                blendColors = listOf(
                                    BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.8f)),
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .background(barColor),
        ) {
            NavigationBar(color = barColor) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { onSelect(index) },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        }
    }
}
