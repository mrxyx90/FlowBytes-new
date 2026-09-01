package com.ray.flowmeter

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.ray.flowmeter.data.AlertRepository
import com.ray.flowmeter.data.AppLimitRepository
import com.ray.flowmeter.data.FlowMeterDatabase
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.receiver.WidgetUpdateScheduler
import com.ray.flowmeter.service.AppBlockVpnService
import com.ray.flowmeter.service.NetworkMonitoringService
import com.ray.flowmeter.ui.dialogs.ChangelogDialog
import com.ray.flowmeter.ui.dialogs.UpdateDialog
import com.ray.flowmeter.ui.screens.Destination
import com.ray.flowmeter.ui.screens.MainScreen
import com.ray.flowmeter.ui.screens.OnboardingScreen
import com.ray.flowmeter.ui.theme.FlowMeterTheme
import com.ray.flowmeter.ui.viewmodels.AlertsViewModel
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel
import com.ray.flowmeter.ui.viewmodels.AppUsageViewModel
import com.ray.flowmeter.ui.viewmodels.HomeViewModel
import com.ray.flowmeter.ui.viewmodels.OnboardingViewModel
import com.ray.flowmeter.ui.viewmodels.SettingsViewModel
import com.ray.flowmeter.utils.AppUpdateHelper
import com.ray.flowmeter.utils.LocaleHelper
import com.ray.flowmeter.utils.UpdateResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Main entry activity. Handles app startup, database/repository initialization,
// Compose UI hosting, and orchestration of background monitoring and VPN blocking services.
class MainActivity : ComponentActivity() {

    private var currentAppliedLanguage: String = ""
    private lateinit var appUpdateHelper: AppUpdateHelper
    private var appUpdateManager: AppUpdateManager? = null

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateCompletedToast()
        }
    }

    private fun showUpdateCompletedToast() {
        Toast.makeText(
            this,
            "An update has been downloaded. Restarting app in 3 seconds to complete install...",
            Toast.LENGTH_LONG,
        ).show()
        lifecycleScope.launch {
            delay(3000.milliseconds)
            appUpdateManager?.completeUpdate()
        }
    }

    // --- VPN Permission & Startup Orchestration ---
    
    // Result launcher to handle the user's response to the system VPN permission dialog.
    private val vpnRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        } else {
            // Revert the setting if permission was denied by the user.
            val repository = UserPreferencesRepository(applicationContext)
            kotlinx.coroutines.MainScope().launch {
                repository.setAppBlockingMasterEnabled(enabled = false)
            }
        }
    }

    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.e("MainActivity", "Update flow failed! Result code: ${result.resultCode}")
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, AppBlockVpnService::class.java)
        startService(intent)
    }

    // Requests system VPN permission if needed, otherwise starts the VPN directly.
    private fun prepareVpn() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnRequestLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Lay out UI components edge-to-edge behind system status/navigation bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val repository = UserPreferencesRepository(applicationContext)

        // Keep the splash screen visible until theme settings are loaded to prevent content overlap.
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }
        appUpdateHelper = AppUpdateHelper(this, repository)
        if (appUpdateHelper.getInstallerPackageName(this) == "com.android.vending") {
            val manager = AppUpdateManagerFactory.create(this)
            appUpdateManager = manager
            manager.registerListener(installStateUpdatedListener)
        }

        // Initialize and observe language configuration changes dynamically.
        lifecycleScope.launch {
            repository.language.collect { languageCode ->
                if (languageCode != currentAppliedLanguage) {
                    currentAppliedLanguage = languageCode
                    LocaleHelper.applyLocale(this@MainActivity, languageCode)
                }
            }
        }

        val database = FlowMeterDatabase.getDatabase(applicationContext)
        val alertRepository = AlertRepository(database.appAlertDao())
        val appLimitRepository = AppLimitRepository(database.appLimitDao())


        setContent {
            val (gitHubUpdate, setGitHubUpdate) = remember { mutableStateOf<UpdateResult.GitHubUpdateAvailable?>(null) }
            // Load user theme preferences asynchronously before rendering the app theme.
            val themeSettingsState = produceState<ThemeSettings?>(initialValue = null) {
                val themeMode = repository.themeMode.first()
                val useMaterialYou = repository.useMaterialYou.first()
                val useAmoled = repository.useAmoled.first()
                val accentColor = repository.accentColor.first()
                value = ThemeSettings(themeMode, useMaterialYou, useAmoled, accentColor)
                isReady = true
            }

            val settings = themeSettingsState.value
            if (settings == null) {
                // Show a matching blank background during the brief preference loading phase to prevent screen flash.
                val isDark = isSystemInDarkTheme()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xFF1A1B1E) else Color(0xFFFDFBFF))
                )
            } else {
                // --- ViewModel Provisioning ---
                // ViewModels are created with custom factories to inject repositories and application contexts.
                
                val onboardingViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return OnboardingViewModel(repository) as T
                            }
                        },
                    )[OnboardingViewModel::class.java]
                }

                val settingsViewModel = remember(settings) {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return SettingsViewModel(
                                    repository = repository,
                                    initialTheme = settings.themeMode,
                                    initialMaterialYou = settings.useMaterialYou,
                                    initialAMOLED = settings.useAmoled,
                                    initialAccent = settings.accentColor
                                ) as T
                            }
                        },
                    )[SettingsViewModel::class.java]
                }

                val homeViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return HomeViewModel(applicationContext, repository) as T
                            }
                        },
                    )[HomeViewModel::class.java]
                }

                val appUsageViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return AppUsageViewModel(repository, applicationContext) as T
                            }
                        },
                    )[AppUsageViewModel::class.java]
                }

                val alertsViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return AlertsViewModel(alertRepository, repository) as T
                            }
                        },
                    )[AlertsViewModel::class.java]
                }

                val appLimitsViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return AppLimitsViewModel(appLimitRepository, repository, applicationContext) as T
                            }
                        },
                    )[AppLimitsViewModel::class.java]
                }

                val themeMode by settingsViewModel.themeMode.collectAsState()
                val languageCode by settingsViewModel.language.collectAsState()
                val useMaterialYou by settingsViewModel.useMaterialYou.collectAsState()
                val useAMOLED by settingsViewModel.useAMOLED.collectAsState()
                val accentColor by settingsViewModel.accentColor.collectAsState()
                val onboardingCompleted by repository.onboardingCompleted.collectAsState(false)

                val currentContext = LocalContext.current
                val localizedContext = remember(languageCode, currentContext) {
                    LocaleHelper.applyLocale(currentContext, languageCode)
                }
                val layoutDirection = if (localizedContext.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                    LayoutDirection.Rtl
                } else {
                    LayoutDirection.Ltr
                }

                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalConfiguration provides localizedContext.resources.configuration,
                    LocalLayoutDirection provides layoutDirection
                ) {
                    val context = LocalContext.current

                    val currentVersionCode = BuildConfig.VERSION_CODE
                    val (showChangelog, setShowChangelog) = remember { mutableStateOf(false) }

                    val lastVersionCode by repository.lastVersionCode.collectAsState(-1)

                    val checkUpdatesAutomatically by repository.checkUpdatesAutomatically.collectAsState(false)
                    val lastUpdateCheckTime by repository.lastUpdateCheckTime.collectAsState(0L)

                    LaunchedEffect(onboardingCompleted) {
                        if (onboardingCompleted) {
                            val now = System.currentTimeMillis()
                            val oneDay = 24 * 60 * 60 * 1000L
                            if (checkUpdatesAutomatically && ((now - lastUpdateCheckTime) >= oneDay)) {
                                repository.setLastUpdateCheckTime(now)
                                appUpdateHelper.checkForUpdates { result ->
                                    when (result) {
                                        is UpdateResult.PlayStoreUpdateAvailable -> {
                                            appUpdateManager?.startUpdateFlowForResult(
                                                result.appUpdateInfo,
                                                updateLauncher,
                                                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                                            )
                                        }
                                        is UpdateResult.GitHubUpdateAvailable -> {
                                            setGitHubUpdate(result)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }


                    // Check for version code changes to determine if we should update settings or trigger release changes.
                    LaunchedEffect(onboardingCompleted, lastVersionCode) {
                        if (onboardingCompleted && (lastVersionCode != -1)) {
                            if (lastVersionCode < currentVersionCode) {
                                delay(1000.milliseconds)
                                repository.updateLastVersionCode(currentVersionCode)
                            }
                        }
                    }

                    FlowMeterTheme(
                        themeMode = themeMode,
                        useMaterialYou = useMaterialYou,
                        useAmoled = useAMOLED,
                        accentColor = accentColor,
                    ) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.background
                        ) {
                            if (onboardingCompleted) {
                                val monitoringEnabled by settingsViewModel.monitoringEnabled.collectAsState()
                                val appBlockingMasterEnabled by settingsViewModel.appBlockingMasterEnabled.collectAsState()

                                // Monitor the foreground tracking service lifecycle based on user settings.
                                LaunchedEffect(monitoringEnabled) {
                                    if (monitoringEnabled != null) {
                                        val serviceIntent = Intent(this@MainActivity, NetworkMonitoringService::class.java)
                                        if (monitoringEnabled == true) {
                                            if (!NetworkMonitoringService.isRunning) {
                                                startForegroundService(serviceIntent)
                                            }
                                        } else {
                                            stopService(serviceIntent)
                                            stopService(Intent(this@MainActivity, AppBlockVpnService::class.java))
                                        }
                                    }
                                }

                                val widgetUpdateInterval by settingsViewModel.widgetUpdateInterval.collectAsState()

                                LaunchedEffect(widgetUpdateInterval) {
                                    WidgetUpdateScheduler.schedule(applicationContext, widgetUpdateInterval)
                                }

                                // Automatically start VPN blocking service if master controls are toggled on.
                                LaunchedEffect(monitoringEnabled, appBlockingMasterEnabled) {
                                    if (monitoringEnabled == true && appBlockingMasterEnabled == true) {
                                        prepareVpn()
                                    }
                                }

                                val (currentIntent, setCurrentIntent) = remember { mutableStateOf(intent) }

                                // Listen for resume lifecycle events to update current intent (e.g. user clicked notification while app is running).
                                val lifecycleOwner = LocalLifecycleOwner.current
                                DisposableEffect(lifecycleOwner) {
                                    val observer = LifecycleEventObserver { _, event ->
                                        if (event == Lifecycle.Event.ON_RESUME) {
                                            if (currentIntent != intent) {
                                                setCurrentIntent(intent)
                                            }
                                        }
                                    }
                                    lifecycleOwner.lifecycle.addObserver(observer)
                                    onDispose {
                                        lifecycleOwner.lifecycle.removeObserver(observer)
                                    }
                                }

                                // --- Notification Extra / Deep Link Handling ---
                                val navigateToAlerts = currentIntent?.getBooleanExtra(NetworkMonitoringService.EXTRA_NAVIGATE_TO_ALERTS, false) ?: false
                                val navigateToLimits = currentIntent?.getBooleanExtra(NetworkMonitoringService.EXTRA_NAVIGATE_TO_LIMITS, false) ?: false

                                val initialDestination = when {
                                    navigateToLimits -> Destination.Limits
                                    navigateToAlerts -> Destination.Alerts
                                    else -> Destination.Home
                                }

                                val muteAppName = currentIntent?.getStringExtra(NetworkMonitoringService.EXTRA_MUTE_APP_NAME)
                                val dismissNotificationId = currentIntent?.getIntExtra(NetworkMonitoringService.EXTRA_DISMISS_NOTIFICATION_ID, -1) ?: -1
                                val isIgnoreAction = currentIntent?.action == NetworkMonitoringService.ACTION_IGNORE_APP

                                // Process incoming intent actions (such as clicking "Ignore App" directly from an alert notification).
                                LaunchedEffect(currentIntent) {
                                    if (muteAppName != null) {
                                        if (isIgnoreAction) {
                                            if (dismissNotificationId != -1) {
                                                try {
                                                    val manager = getSystemService(android.app.NotificationManager::class.java)
                                                    manager?.cancel(dismissNotificationId)
                                                } catch (e: Exception) {
                                                    Log.e("MainActivity", "Failed to cancel notification", e)
                                                }
                                            }
                                        }
                                        alertsViewModel.onMuteRequested(muteAppName)

                                        // Clear extras to avoid re-triggering the action if the activity is recreated.
                                        intent.removeExtra(NetworkMonitoringService.EXTRA_MUTE_APP_NAME)
                                        intent.removeExtra(NetworkMonitoringService.EXTRA_DISMISS_NOTIFICATION_ID)
                                        if (intent.action == NetworkMonitoringService.ACTION_IGNORE_APP) {
                                            intent.action = null
                                        }

                                        setCurrentIntent(null)
                                    }
                                }

                                MainScreen(
                                    homeViewModel = homeViewModel,
                                    appUsageViewModel = appUsageViewModel,
                                    alertsViewModel = alertsViewModel,
                                    appLimitsViewModel = appLimitsViewModel,
                                    settingsViewModel = settingsViewModel,
                                    initialDestination = initialDestination,
                                    onCheckForUpdates = {
                                        Toast.makeText(context, R.string.toast_checking_updates, Toast.LENGTH_SHORT).show()
                                        appUpdateHelper.checkForUpdates { result ->
                                            when (result) {
                                                is UpdateResult.PlayStoreUpdateAvailable -> {
                                                    appUpdateManager?.startUpdateFlowForResult(
                                                        result.appUpdateInfo,
                                                        updateLauncher,
                                                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                                                    )
                                                }
                                                is UpdateResult.GitHubUpdateAvailable -> {
                                                    setGitHubUpdate(result)
                                                }
                                                is UpdateResult.NoUpdateAvailable -> {
                                                    Toast.makeText(context, R.string.toast_app_up_to_date, Toast.LENGTH_SHORT).show()
                                                }
                                                is UpdateResult.Error -> {
                                                    Toast.makeText(context, R.string.toast_update_check_failed, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )

                                if (showChangelog) {
                                    ChangelogDialog { setShowChangelog(false) }
                                }

                                gitHubUpdate?.let { update ->
                                    UpdateDialog(
                                        tagName = update.tag,
                                        releaseNotes = update.releaseNotes,
                                        onDismiss = { setGitHubUpdate(null) },
                                        onIgnore = {
                                            lifecycleScope.launch {
                                                repository.setIgnoredUpdateVersion(update.tag)
                                            }
                                            setGitHubUpdate(null)
                                        },
                                        onUpdate = {
                                            val updateIntent = Intent(Intent.ACTION_VIEW, update.downloadUrl.toUri())
                                            try {
                                                startActivity(updateIntent)
                                            } catch (_: Exception) {}
                                            setGitHubUpdate(null)
                                        }
                                    )
                                }
                            } else {
                                OnboardingScreen(
                                    onComplete = {
                                        onboardingViewModel.completeOnboarding()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateCompletedToast()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager?.unregisterListener(installStateUpdatedListener)
    }
}

private data class ThemeSettings(
    val themeMode: String,
    val useMaterialYou: Boolean,
    val useAmoled: Boolean,
    val accentColor: Long?
)
