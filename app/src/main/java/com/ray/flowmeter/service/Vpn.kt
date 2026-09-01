package com.ray.flowmeter.service

import android.annotation.SuppressLint
import android.content.Context
import com.ray.flowmeter.R
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.data.AppLimitRepository
import com.ray.flowmeter.data.FlowMeterDatabase
import com.ray.flowmeter.data.UserPreferencesRepository
import com.ray.flowmeter.utils.LocaleHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

// VPN Service that intercepts and blocks network traffic for applications
// that have exceeded their configured cellular or Wi-Fi data usage limits.
@SuppressLint("VpnServicePolicy")
class AppBlockVpnService : VpnService() {

    override fun attachBaseContext(newBase: Context) {
        val repository = UserPreferencesRepository(newBase)
        val languageCode = runBlocking {
            try {
                repository.language.first()
            } catch (_: Exception) {
                ""
            }
        }
        val context = LocaleHelper.applyLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var repository: AppLimitRepository
    private lateinit var userPrefs: UserPreferencesRepository

    private var collectionJob: Job? = null
    private val currentNetworkType = MutableStateFlow<Int?>(null)

    // Track active connection type changes (cellular vs Wi-Fi) to apply corresponding block rules.
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
                else -> null
            }
            type?.let { currentNetworkType.value = it }
        }

        override fun onLost(network: Network) {
            val cm = getSystemService(ConnectivityManager::class.java)
            val activeNetwork = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
            if (capabilities == null) {
                currentNetworkType.value = null
            } else {
                val type = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
                    else -> null
                }
                currentNetworkType.value = type
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val database = FlowMeterDatabase.getDatabase(applicationContext)
        repository = AppLimitRepository(database.appLimitDao())
        userPrefs = UserPreferencesRepository(applicationContext)

        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        
        val activeNet = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNet)
        if (caps != null) {
            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
                else -> null
            }
            currentNetworkType.value = type
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private data class VpnBlockConfig(
        val blockAll: Boolean,
        val blockedApps: List<String>
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        collectionJob?.cancel()
        
        val tickerFlow = flow {
            while (currentCoroutineContext().isActive) {
                emit(System.currentTimeMillis())
                delay(5000.milliseconds)
            }
        }

        collectionJob = serviceScope.launch {
            @Suppress("UNCHECKED_CAST")
            combine(
                repository.allAppLimits,
                userPrefs.appBlockingMasterEnabled,
                userPrefs.isFourGBlocked,
                userPrefs.isCellularBlocked,
                userPrefs.isWifiBlocked,
                userPrefs.isOn4G,
                currentNetworkType,
                tickerFlow
            ) { args ->
                val limits = args[0] as List<AppLimit>
                val masterEnabled = args[1] as Boolean
                val is4GBlocked = args[2] as Boolean
                val isCellBlocked = args[3] as Boolean
                val isWifiBlocked = args[4] as Boolean
                val isOn4G = args[5] as Boolean
                val networkType = args[6] as? Int
                
                if (!masterEnabled) null
                else {
                    val systemLimitExceeded = when (networkType) {
                        NetworkCapabilities.TRANSPORT_CELLULAR -> isCellBlocked || is4GBlocked
                        NetworkCapabilities.TRANSPORT_WIFI -> isWifiBlocked
                        else -> false
                    }

                    if (systemLimitExceeded) {
                        VpnBlockConfig(blockAll = true, blockedApps = emptyList())
                    } else {
                        val blockedApps = limits.filter { limit ->
                            limit.isManuallyBlocked || (limit.isEnabled && when (limit.networkType) {
                                "wifi" -> limit.isBlocked && (networkType == NetworkCapabilities.TRANSPORT_WIFI)
                                "mobile" -> limit.isBlocked && (networkType == NetworkCapabilities.TRANSPORT_CELLULAR)
                                "four_g" -> limit.isBlocked && (networkType == NetworkCapabilities.TRANSPORT_CELLULAR && isOn4G)
                                "both" -> {
                                    (limit.isWifiBlocked && (networkType == NetworkCapabilities.TRANSPORT_WIFI)) ||
                                    (limit.isMobileBlocked && (networkType == NetworkCapabilities.TRANSPORT_CELLULAR))
                                }
                                else -> limit.isBlocked
                            })
                        }.map { it.packageName }.toList()
                        VpnBlockConfig(blockAll = false, blockedApps = blockedApps)
                    }
                }
            }.distinctUntilChanged().collectLatest { config ->
                if (config == null) {
                    vpnInterface?.close()
                    vpnInterface = null
                    stopSelf()
                } else {
                    updateVpnInterface(config.blockAll, config.blockedApps)
                }
            }
        }
        return START_STICKY
    }

    private fun updateVpnInterface(blockAll: Boolean, blockedApps: List<String>) {
        vpnInterface?.close()
        vpnInterface = null

        if (!blockAll && blockedApps.isEmpty()) {
            Log.d("AppBlockVpnService", "No apps to block. VPN idle.")
            return
        }

        try {
            val builder = Builder()
                .setSession(getString(R.string.vpn_session_name))
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)

            if (blockAll) {
                builder.addDisallowedApplication("com.ray.flowmeter")
                Log.d("AppBlockVpnService", "VPN established blocking ALL traffic (system plan limit exceeded)")
            } else {
                for (packageName in blockedApps) {
                    try {
                        builder.addAllowedApplication(packageName)
                    } catch (e: Exception) {
                        Log.e("AppBlockVpnService", "Could not add app to VPN: $packageName", e)
                    }
                }
                Log.d("AppBlockVpnService", "VPN established blocking apps: ${blockedApps.joinToString()}")
            }

            vpnInterface = builder.establish()
        } catch (e: Exception) {
            Log.e("AppBlockVpnService", "Failed to establish VPN", e)
        }
    }

    override fun onDestroy() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        serviceJob.cancel()
        vpnInterface?.close()
        super.onDestroy()
    }
}
