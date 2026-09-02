// Scaffold container coordinating bottom navigation bar destination switching
// and binding activity-level lifecycle events to screen-level parameters.
package com.ray.flowmeter.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.ray.flowmeter.R
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.ui.dialogs.MuteAppDialog
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.viewmodels.AlertsViewModel
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import com.ray.flowmeter.ui.viewmodels.HomeViewModel
import com.ray.flowmeter.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

@Serializable
sealed interface Destination {
    @Serializable
    data object Home : Destination

    @Serializable
    data object Usage : Destination

    @Serializable
    data object Alerts : Destination

    @Serializable
    data object Limits : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object AppPicker : Destination

    @Serializable
    data object Widgets : Destination
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    appUsageViewModel: AppUsageViewModel,
    alertsViewModel: AlertsViewModel,
    appLimitsViewModel: AppLimitsViewModel,
    settingsViewModel: SettingsViewModel,
    initialDestination: Destination = Destination.Home,
    onCheckForUpdates: () -> Unit = {},
) {
    val homeScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val usageScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val alertsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val limitsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val settingsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val systemAppsScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val backStack = remember { mutableStateListOf(initialDestination) }
    val currentDestination = backStack.last()

    var activeLayoutDestination by remember { mutableStateOf(initialDestination) }
    LaunchedEffect(currentDestination) {
        activeLayoutDestination = if ((currentDestination == Destination.AppPicker) || (currentDestination == Destination.Widgets)) {
            delay(500.milliseconds)
            currentDestination
        } else {
            currentDestination
        }
    }

    val currentScrollBehavior = when (currentDestination) {
        Destination.Home -> homeScrollBehavior
        Destination.Usage -> usageScrollBehavior
        Destination.Alerts -> alertsScrollBehavior
        Destination.Limits -> limitsScrollBehavior
        Destination.Settings -> settingsScrollBehavior
        Destination.AppPicker -> limitsScrollBehavior
        Destination.Widgets -> settingsScrollBehavior
    }

    BackHandler(enabled = (currentDestination != Destination.Home)) {
        when (currentDestination) {
            Destination.AppPicker -> appLimitsViewModel.isPickerOpen = false
            Destination.Widgets -> homeViewModel.isWidgetsOpen = false
            else -> {
                backStack.clear()
                backStack.add(Destination.Home)
            }
        }
    }

    val context = LocalContext.current
    val repository = remember { UserPreferencesRepository(context.applicationContext) }
    val showUsageFilters by appUsageViewModel.showFilters.collectAsState()
    var showAlertsFilters by remember { mutableStateOf(value = false) }



    // Update data when switching tabs
    LaunchedEffect(currentDestination) {
        when (currentDestination) {
            Destination.Home -> homeViewModel.updateTotalUsage()
            Destination.Usage -> appUsageViewModel.refreshData(isManual = false)
            Destination.Alerts -> alertsViewModel.refreshData(isManual = false)
            else -> {}
        }
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val isWideScreen = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(600)

    LaunchedEffect(appLimitsViewModel.isPickerOpen) {
        if (appLimitsViewModel.isPickerOpen) {
            if (currentDestination != Destination.AppPicker) {
                backStack.add(Destination.AppPicker)
            }
        } else {
            if (currentDestination == Destination.AppPicker) {
                backStack.removeLastOrNull()
            }
        }
    }

    LaunchedEffect(homeViewModel.isWidgetsOpen) {
        if (homeViewModel.isWidgetsOpen) {
            if (currentDestination != Destination.Widgets) {
                backStack.add(Destination.Widgets)
            }
        } else {
            if (currentDestination == Destination.Widgets) {
                backStack.removeLastOrNull()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            val showGlobalNavRail = isWideScreen && activeLayoutDestination != Destination.AppPicker && activeLayoutDestination != Destination.Widgets
            if (showGlobalNavRail) {
                val navRailAlpha by animateFloatAsState(
                    targetValue = if (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets) 0f else 1f,
                    animationSpec = tween(300),
                    label = "NavRailAlpha",
                )

                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.graphicsLayer { alpha = navRailAlpha },
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.title_home)
                            )
                        },
                        label = { Text(stringResource(R.string.title_home), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Home,
                        onClick = {
                            if (currentDestination != Destination.Home) {
                                backStack.clear()
                                backStack.add(Destination.Home)
                            }
                        }
                    )
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Usage) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                contentDescription = stringResource(R.string.title_app_usage)
                            )
                        },
                        label = { Text(stringResource(R.string.label_usage), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Usage,
                        onClick = {
                            if (currentDestination != Destination.Usage) {
                                backStack.clear()
                                backStack.add(Destination.Usage)
                            }
                        }
                    )
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Alerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.title_alerts)
                            )
                        },
                        label = { Text(stringResource(R.string.label_alerts), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Alerts,
                        onClick = {
                            if (currentDestination != Destination.Alerts) {
                                backStack.clear()
                                backStack.add(Destination.Alerts)
                            }
                        }
                    )
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Limits) Icons.Filled.Security else Icons.Outlined.Security,
                                contentDescription = stringResource(R.string.title_limits)
                            )
                        },
                        label = { Text(stringResource(R.string.title_limits), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Limits,
                        onClick = {
                            if (currentDestination != Destination.Limits) {
                                backStack.clear()
                                backStack.add(Destination.Limits)
                            }
                        }
                    )
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.title_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.title_settings), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Settings,
                        onClick = {
                            if (currentDestination != Destination.Settings) {
                                backStack.clear()
                                backStack.add(Destination.Settings)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                val showGlobalTopBar = activeLayoutDestination != Destination.AppPicker && activeLayoutDestination != Destination.Widgets
                if (showGlobalTopBar) {
                    val containerColor by animateColorAsState(
                        targetValue = if (currentScrollBehavior.state.contentOffset < -1f) {
                            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        animationSpec = tween(durationMillis = 250),
                        label = "TopBarColorAnimation",
                    )

                    val topBarAlpha by animateFloatAsState(
                        targetValue = if (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets) 0f else 1f,
                        animationSpec = tween(300),
                        label = "TopBarAlpha"
                    )

                    Surface(
                        color = containerColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = topBarAlpha },
                    ) {
                        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                            key(activeLayoutDestination) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {


                                    Text(
                                        text = when (activeLayoutDestination) {
                                            Destination.Home -> stringResource(R.string.app_name)
                                            Destination.Usage -> stringResource(R.string.title_app_usage)
                                            Destination.Alerts -> stringResource(R.string.title_alerts)
                                            Destination.Limits -> stringResource(R.string.title_limits)
                                            Destination.Settings -> stringResource(R.string.title_settings)
                                            else -> ""
                                        },
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = if (LocalConfiguration.current.locales[0].language == "ar") 0.sp else (-0.5).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (activeLayoutDestination == Destination.Home) {
                                        IconButton(
                                            onClick = {
                                                homeViewModel.isWidgetsOpen = true
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color.Transparent
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Widgets,
                                                contentDescription = stringResource(R.string.cd_manage_widgets),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }



                                    if (activeLayoutDestination == Destination.Usage) {
                                        IconButton(
                                            onClick = { appUsageViewModel.setShowFilters(!showUsageFilters) },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color.Transparent
                                            )
                                        ) {
                                            Icon(
                                                imageVector = com.ray.flowmeter.ui.components.AppIcons.Filter,
                                                contentDescription = stringResource(R.string.cd_toggle_filters),
                                                tint = if (showUsageFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (activeLayoutDestination == Destination.Alerts) {
                                        IconButton(
                                            onClick = { showAlertsFilters = !showAlertsFilters },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color.Transparent
                                            )
                                        ) {
                                            Icon(
                                                imageVector = com.ray.flowmeter.ui.components.AppIcons.Filter,
                                                contentDescription = stringResource(R.string.cd_toggle_filters),
                                                tint = if (showAlertsFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (activeLayoutDestination == Destination.Limits) {
                                        val appBlockingMasterEnabled by appLimitsViewModel.appBlockingMasterEnabled.collectAsState()
                                        
                                        // Blooming pulse animation for when firewall is OFF
                                        val infiniteTransition = rememberInfiniteTransition(label = "FirewallPulse")
                                        val pulseScale by infiniteTransition.animateFloat(
                                            initialValue = 1.0f,
                                            targetValue = 1.20f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000, easing = EaseInOutCubic),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "PulseScale"
                                        )
                                        val pulseAlpha by infiniteTransition.animateFloat(
                                            initialValue = 0.25f,
                                            targetValue = 0.45f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000, easing = EaseInOutCubic),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "PulseAlpha"
                                        )

                                        val firewallRed = Color(0xFFFF1111)

                                        val buttonBgColor by animateColorAsState(
                                            targetValue = if (appBlockingMasterEnabled) MaterialTheme.colorScheme.primaryContainer
                                                          else Color.Transparent, // Single layer drawn via drawBehind
                                            animationSpec = tween(300),
                                            label = "FirewallBackgroundColor"
                                        )
                                        val buttonContentColor by animateColorAsState(
                                            targetValue = if (appBlockingMasterEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                                          else firewallRed,
                                            animationSpec = tween(300),
                                            label = "FirewallContentColor"
                                        )
                                        
                                        IconButton(
                                            onClick = {
                                                val targetState = !appBlockingMasterEnabled
                                                appLimitsViewModel.setAppBlockingMasterEnabled(targetState)
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = buttonBgColor,
                                                contentColor = buttonContentColor
                                            ),
                                            modifier = Modifier
                                                .size(40.dp)
                                                .drawBehind {
                                                    if (!appBlockingMasterEnabled) {
                                                        drawCircle(
                                                            color = firewallRed.copy(alpha = pulseAlpha),
                                                            radius = (size.minDimension / 2) * pulseScale
                                                        )
                                                    }
                                                }
                                                .clip(CircleShape)
                                        ) {
                                            Crossfade(
                                                targetState = appBlockingMasterEnabled,
                                                animationSpec = tween(200),
                                                label = "FirewallIconTransition"
                                            ) { enabled ->
                                                Icon(
                                                    imageVector = if (enabled) Icons.Rounded.Security else Icons.Rounded.Shield,
                                                    contentDescription = stringResource(R.string.label_block_apps),
                                                    modifier = Modifier.size(20.dp),
                                                    tint = buttonContentColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentDestination == Destination.Alerts) {
                    val alerts by alertsViewModel.alerts.collectAsState()
                    if (alerts.isNotEmpty()) {
                        val (showClearDialog, setShowClearDialog) = remember { mutableStateOf(value = false) }

                        FloatingActionButton(
                            onClick = { setShowClearDialog(true) },
                            modifier = Modifier.padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(20.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.btn_clear_history),
                            )
                        }

                        if (showClearDialog) {
                            AlertDialog(
                                onDismissRequest = { setShowClearDialog(false) },
                                title = { Text(stringResource(R.string.btn_clear_history)) },
                                text = { Text(stringResource(R.string.msg_confirm_clear_history)) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            alertsViewModel.clearHistory()
                                            setShowClearDialog(false)
                                        }
                                    ) {
                                        Text(stringResource(R.string.btn_ok))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { setShowClearDialog(false) }) {
                                        Text(stringResource(R.string.btn_cancel))
                                    }
                                }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                val showGlobalBottomBar = !isWideScreen && activeLayoutDestination != Destination.AppPicker && activeLayoutDestination != Destination.Widgets
                if (showGlobalBottomBar) {
                    val bottomBarAlpha by animateFloatAsState(
                        targetValue = if (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets) 0f else 1f,
                        animationSpec = tween(300),
                        label = "BottomBarAlpha"
                    )

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.graphicsLayer { alpha = bottomBarAlpha }
                    ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.title_home)
                            )
                        },
                        label = { Text(stringResource(R.string.title_home), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Home,
                        onClick = {
                            if (currentDestination != Destination.Home) {
                                backStack.clear()
                                backStack.add(Destination.Home)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Usage) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                contentDescription = stringResource(R.string.title_app_usage)
                            )
                        },
                        label = { Text(stringResource(R.string.label_usage), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Usage,
                        onClick = {
                            if (currentDestination != Destination.Usage) {
                                backStack.clear()
                                backStack.add(Destination.Usage)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Limits) Icons.Filled.Timer else Icons.Outlined.Timer,
                                contentDescription = stringResource(R.string.title_limits)
                            )
                        },
                        label = { Text(stringResource(R.string.label_plans), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Limits,
                        onClick = {
                            if (currentDestination != Destination.Limits) {
                                backStack.clear()
                                backStack.add(Destination.Limits)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Alerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.title_alerts)
                            )
                        },
                        label = { Text(stringResource(R.string.label_alerts), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Alerts,
                        onClick = {
                            if (currentDestination != Destination.Alerts) {
                                backStack.clear()
                                backStack.add(Destination.Alerts)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (activeLayoutDestination == Destination.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.title_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.title_settings), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selected = activeLayoutDestination == Destination.Settings,
                        onClick = {
                            if (currentDestination != Destination.Settings) {
                                backStack.clear()
                                backStack.add(Destination.Settings)
                            }
                        }
                    )
                }
                }
            }
        ) { innerPadding ->

            var lastStablePadding by remember { mutableStateOf(PaddingValues()) }
            val inFullscreenTransition = (currentDestination == Destination.AppPicker || currentDestination == Destination.Widgets)
                    && (activeLayoutDestination != currentDestination)

            LaunchedEffect(innerPadding, inFullscreenTransition) {
                if (!inFullscreenTransition) {
                    lastStablePadding = innerPadding
                }
            }

            val directive = calculatePaneScaffoldDirective(windowAdaptiveInfo)
            val listDetailStrategy = rememberListDetailSceneStrategy<Destination>(directive = directive)

            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(listDetailStrategy),
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                transitionSpec = {
                    val initialDest = initialState.key as? Destination
                    val targetDest = targetState.key as? Destination

                    when {
                        targetDest == Destination.Widgets || targetDest == Destination.AppPicker -> {
                            (scaleIn(initialScale = 0.88f, animationSpec = tween(500, easing = EaseOutCubic)) +
                             fadeIn(animationSpec = tween(400)) +
                             slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(500, easing = EaseOutCubic))
                            ).togetherWith(
                                fadeOut(animationSpec = tween(400))
                            )
                        }
                        initialDest == Destination.Widgets || initialDest == Destination.AppPicker -> {
                            fadeIn(animationSpec = tween(400)) togetherWith (
                                scaleOut(targetScale = 0.88f, animationSpec = tween(500, easing = EaseOutCubic)) +
                                fadeOut(animationSpec = tween(400)) +
                                slideOutVertically(targetOffsetY = { it / 8 }, animationSpec = tween(500, easing = EaseOutCubic))
                            )
                        }
                        else -> {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        }
                    }
                },
                entryProvider = { key ->
                    when (key) {
                        Destination.Home -> NavEntry(key) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToUsage = { date ->
                                    backStack.clear()
                                    backStack.add(Destination.Usage)
                                    appUsageViewModel.isViewingSystemApps = false
                                    appUsageViewModel.loadAppUsageForDate(date.timeInMillis)
                                },
                                onNavigateToTodayUsage = {
                                    backStack.clear()
                                    backStack.add(Destination.Usage)
                                    appUsageViewModel.isViewingSystemApps = false
                                    appUsageViewModel.loadAppUsageForDate(System.currentTimeMillis())
                                },
                                onNavigateToMonthUsage = {
                                    backStack.clear()
                                    backStack.add(Destination.Usage)
                                    appUsageViewModel.isViewingSystemApps = false
                                },
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(homeScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Usage -> NavEntry(key) {
                            AppUsageScreen(
                                viewModel = appUsageViewModel,
                                showFilters = showUsageFilters,
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(usageScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Alerts -> NavEntry(key) {
                            AlertsScreen(
                                viewModel = alertsViewModel,
                                showFilters = showAlertsFilters,
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(alertsScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Limits -> NavEntry(key) {
                            AppLimitsScreen(
                                viewModel = appLimitsViewModel,
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(limitsScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.Settings -> NavEntry(key) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onCheckForUpdates = onCheckForUpdates,
                                modifier = Modifier.fillMaxSize().padding(lastStablePadding).nestedScroll(settingsScrollBehavior.nestedScrollConnection)
                            )
                        }

                        Destination.AppPicker -> NavEntry(key) {
                            AppPickerScreen(
                                viewModel = appLimitsViewModel,
                                onBack = {
                                    appLimitsViewModel.isPickerOpen = false
                                }
                            ) { limits ->
                                val isFirewallOn = appLimitsViewModel.appBlockingMasterEnabled.value
                                if (!isFirewallOn) {
                                    android.widget.Toast.makeText(context, "Firewall is off please enable", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                appLimitsViewModel.addAppLimits(limits)
                                appLimitsViewModel.isPickerOpen = false
                            }
                        }

                        Destination.Widgets -> NavEntry(key) {
                            WidgetsScreen(
                                context = context,
                                repository = repository,
                                onBack = {
                                    homeViewModel.isWidgetsOpen = false
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            )
        }

        AppLimitsOverlay(appLimitsViewModel)

        val context = LocalContext.current
        val muteAppName = alertsViewModel.muteRequestAppName
        muteAppName?.let {
            MuteAppDialog(
                appName = it,
                onDismiss = { alertsViewModel.clearMuteRequest() },
                onConfirm = { durationMs ->
                    alertsViewModel.muteApp(context, it, durationMs)
                }
            )
        }

        val isViewingSystemApps = appUsageViewModel.isViewingSystemApps
        val filteredSystemAppList by appUsageViewModel.filteredSystemAppUsageList.collectAsState()
        val networkFilter by appUsageViewModel.networkFilter.collectAsState()
        val systemListState = rememberLazyListState()

        AnimatedVisibility(
            visible = isViewingSystemApps,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            BackHandler {
                appUsageViewModel.isViewingSystemApps = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .nestedScroll(systemAppsScrollBehavior.nestedScrollConnection)
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                val filterTitle = when (networkFilter) {
                                    "four_g" -> stringResource(R.string.filter_four_g_only)
                                    "mobile" -> stringResource(R.string.filter_mobile_only)
                                    "wifi" -> stringResource(R.string.filter_wifi_only)
                                    else -> stringResource(R.string.title_system_data_usage)
                                }
                                Text(
                                    text = filterTitle,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { appUsageViewModel.isViewingSystemApps = false }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back)
                                    )
                                }
                            },
                            scrollBehavior = systemAppsScrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            )
                        )
                    }
                ) { padding ->
                    val maxUsageBytes = if (filteredSystemAppList.isNotEmpty()) {
                        filteredSystemAppList.maxOf {
                            when (networkFilter) {
                                "mobile" -> it.cellUsage
                                "four_g" -> it.fourGUsage
                                "wifi" -> it.wifiUsage
                                else -> it.totalUsage
                            }
                        }.coerceAtLeast(1L)
                    } else {
                        1L
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.background),
                        state = systemListState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (filteredSystemAppList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.msg_no_usage_data),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = filteredSystemAppList,
                                key = { _, it -> it.packageName }
                            ) { _, appUsage ->
                                val displayUsage = when (networkFilter) {
                                    "mobile" -> appUsage.cellUsage
                                    "four_g" -> appUsage.fourGUsage
                                    "wifi" -> appUsage.wifiUsage
                                    else -> appUsage.totalUsage
                                }

                                StaggeredEntrance {
                                    AppUsageItem(
                                        appUsage = appUsage,
                                        displayUsage = displayUsage,
                                        maxUsageBytes = maxUsageBytes,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

    }
}

