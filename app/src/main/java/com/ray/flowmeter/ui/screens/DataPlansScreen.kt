// Configuration screen for general limits (daily/monthly limits, Wi-Fi vs mobile,
// and custom billing period schedules).
package com.ray.flowmeter.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ray.flowmeter.R
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.ui.theme.StaggeredEntrance
import com.ray.flowmeter.ui.theme.bounceClick
import com.ray.flowmeter.ui.viewmodels.AppLimitsViewModel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.drawBehind

@Composable
fun AppLimitsScreen(
    viewModel: AppLimitsViewModel,
    modifier: Modifier = Modifier
) {
    var limitToDelete by remember { mutableStateOf<AppLimit?>(null) }
    var showAddGeneralLimitDialog by remember { mutableStateOf(false) }

    val dataDailyLimitConfigured by viewModel.dataDailyLimitConfigured.collectAsState()
    val dataMonthlyLimitConfigured by viewModel.dataMonthlyLimitConfigured.collectAsState()
    val wifiDailyLimitConfigured by viewModel.wifiDailyLimitConfigured.collectAsState()
    val fourGDailyLimitConfigured by viewModel.fourGDailyLimitConfigured.collectAsState()
    val wifiMonthlyLimitConfigured by viewModel.wifiMonthlyLimitConfigured.collectAsState()
    val dataCustomLimitConfigured by viewModel.dataCustomLimitConfigured.collectAsState()
    val wifiCustomLimitConfigured by viewModel.wifiCustomLimitConfigured.collectAsState()

    val dataDailyLimitEnabled by viewModel.dataDailyLimitEnabled.collectAsState()
    val dataMonthlyLimitEnabled by viewModel.dataMonthlyLimitEnabled.collectAsState()
    val wifiDailyLimitEnabled by viewModel.wifiDailyLimitEnabled.collectAsState()
    val fourGDailyLimitEnabled by viewModel.fourGDailyLimitEnabled.collectAsState()
    val wifiMonthlyLimitEnabled by viewModel.wifiMonthlyLimitEnabled.collectAsState()
    val dataCustomLimitEnabled by viewModel.dataCustomLimitEnabled.collectAsState()
    val wifiCustomLimitEnabled by viewModel.wifiCustomLimitEnabled.collectAsState()

    val dataDailyLimit by viewModel.dataDailyLimit.collectAsState()
    val wifiDailyLimit by viewModel.wifiDailyLimit.collectAsState()
    val fourGDailyLimit by viewModel.fourGDailyLimit.collectAsState()
    val dataMonthlyLimit by viewModel.dataMonthlyLimit.collectAsState()
    val wifiMonthlyLimit by viewModel.wifiMonthlyLimit.collectAsState()
    val dataCustomLimit by viewModel.dataCustomLimit.collectAsState()
    val wifiCustomLimit by viewModel.wifiCustomLimit.collectAsState()

    val dataCustomLimitStart by viewModel.dataCustomLimitStart.collectAsState()
    val dataCustomLimitEnd by viewModel.dataCustomLimitEnd.collectAsState()
    val wifiCustomLimitStart by viewModel.wifiCustomLimitStart.collectAsState()
    val wifiCustomLimitEnd by viewModel.wifiCustomLimitEnd.collectAsState()

    val currentMobileUsage by viewModel.currentMobileUsage.collectAsState()
    val currentWifiUsage by viewModel.currentWifiUsage.collectAsState()
    val currentFourGDailyUsage by viewModel.currentFourGDailyUsage.collectAsState()
    val currentMonthlyMobileUsage by viewModel.currentMonthlyMobileUsage.collectAsState()
    val currentMonthlyWifiUsage by viewModel.currentMonthlyWifiUsage.collectAsState()
    val currentCustomMobileUsage by viewModel.currentCustomMobileUsage.collectAsState()
    val currentCustomWifiUsage by viewModel.currentCustomWifiUsage.collectAsState()

    val appLimits by viewModel.appLimits.collectAsState()
    // No longer used: val appBlockingMasterEnabled by viewModel.appBlockingMasterEnabled.collectAsState()

    val unconfiguredPlans = remember(dataDailyLimitConfigured, dataMonthlyLimitConfigured, wifiDailyLimitConfigured, fourGDailyLimitConfigured, wifiMonthlyLimitConfigured, dataCustomLimitConfigured, wifiCustomLimitConfigured) {
        buildList {
            if (!wifiDailyLimitConfigured) add("daily_wifi")
            if (!dataDailyLimitConfigured) add("daily_mobile")
            if (!fourGDailyLimitConfigured) add("daily_four_g")
            if (!wifiMonthlyLimitConfigured) add("monthly_wifi")
            if (!dataMonthlyLimitConfigured) add("monthly_mobile")
            if (!wifiCustomLimitConfigured) add("custom_wifi")
            if (!dataCustomLimitConfigured) add("custom_mobile")
        }
    }

    val allGeneralLimitsConfigured = unconfiguredPlans.isEmpty()

    if (showAddGeneralLimitDialog) {
        AddGeneralLimitDialog(
            unconfiguredPlans = unconfiguredPlans,
            onDismiss = { showAddGeneralLimitDialog = false },
            onPlanSelected = { planType ->
                viewModel.configuringGeneralLimitType = planType
                showAddGeneralLimitDialog = false
            }
        )
    }

    val filteredAppLimits = remember(appLimits, viewModel.searchQuery) {
        if (viewModel.searchQuery.isBlank()) {
            appLimits.asReversed()
        } else {
            appLimits.asReversed().filter {
                it.appName.contains(viewModel.searchQuery, ignoreCase = true) ||
                it.packageName.contains(viewModel.searchQuery, ignoreCase = true)
            }
        }
    }

    fun formatRange(start: Long, end: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return "${sdf.format(java.util.Date(start))} - ${sdf.format(java.util.Date(end))}"
    }

    data class SystemPlanItem(
        val type: String,
        val title: String,
        val limit: Long,
        val usage: Long,
        val enabled: Boolean,
        val networkType: String, // "wi-fi", "mobile", "four_g"
        val subtitle: String? = null,
        val onToggle: (Boolean) -> Unit,
    )

    val dailyWifiTitle = stringResource(R.string.label_daily_wifi)
    val dailyMobileTitle = stringResource(R.string.label_daily_mobile)
    val dailyFourGTitle = stringResource(R.string.label_daily_four_g)
    val monthlyWifiTitle = stringResource(R.string.label_monthly_wifi)
    val monthlyMobileTitle = stringResource(R.string.label_monthly_mobile)
    val customWifiTitle = stringResource(R.string.label_custom_wifi)
    val customMobileTitle = stringResource(R.string.label_custom_mobile)

    val activePlansList = remember(
        dataDailyLimitConfigured, dataMonthlyLimitConfigured, wifiDailyLimitConfigured, fourGDailyLimitConfigured, wifiMonthlyLimitConfigured, dataCustomLimitConfigured, wifiCustomLimitConfigured,
        dataDailyLimitEnabled, dataMonthlyLimitEnabled, wifiDailyLimitEnabled, fourGDailyLimitEnabled, wifiMonthlyLimitEnabled, dataCustomLimitEnabled, wifiCustomLimitEnabled,
        dataDailyLimit, wifiDailyLimit, fourGDailyLimit, dataMonthlyLimit, wifiMonthlyLimit, dataCustomLimit, wifiCustomLimit,
        currentMobileUsage, currentWifiUsage, currentFourGDailyUsage, currentMonthlyMobileUsage, currentMonthlyWifiUsage, currentCustomMobileUsage, currentCustomWifiUsage,
        wifiCustomLimitStart, wifiCustomLimitEnd, dataCustomLimitStart, dataCustomLimitEnd,
        dailyWifiTitle, dailyMobileTitle, dailyFourGTitle, monthlyWifiTitle, monthlyMobileTitle, customWifiTitle, customMobileTitle
    ) {
        buildList {
            if (wifiDailyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "daily_wifi",
                        title = dailyWifiTitle,
                        limit = wifiDailyLimit,
                        usage = currentWifiUsage,
                        enabled = wifiDailyLimitEnabled,
                        networkType = "wifi",
                        onToggle = { viewModel.setWifiDailyLimitEnabled(it) }
                    )
                )
            }
            if (dataDailyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "daily_mobile",
                        title = dailyMobileTitle,
                        limit = dataDailyLimit,
                        usage = currentMobileUsage,
                        enabled = dataDailyLimitEnabled,
                        networkType = "mobile",
                        onToggle = { viewModel.setDataDailyLimitEnabled(it) }
                    )
                )
            }
            if (fourGDailyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "daily_four_g",
                        title = dailyFourGTitle,
                        limit = fourGDailyLimit,
                        usage = currentFourGDailyUsage,
                        enabled = fourGDailyLimitEnabled,
                        networkType = "four_g",
                        onToggle = { viewModel.setFourGDailyLimitEnabled(it) }
                    )
                )
            }
            if (wifiMonthlyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "monthly_wifi",
                        title = monthlyWifiTitle,
                        limit = wifiMonthlyLimit,
                        usage = currentMonthlyWifiUsage,
                        enabled = wifiMonthlyLimitEnabled,
                        networkType = "wifi",
                        onToggle = { viewModel.setWifiMonthlyLimitEnabled(it) }
                    )
                )
            }
            if (dataMonthlyLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "monthly_mobile",
                        title = monthlyMobileTitle,
                        limit = dataMonthlyLimit,
                        usage = currentMonthlyMobileUsage,
                        enabled = dataMonthlyLimitEnabled,
                        networkType = "mobile",
                        onToggle = { viewModel.setDataMonthlyLimitEnabled(it) }
                    )
                )
            }
            if (wifiCustomLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "custom_wifi",
                        title = customWifiTitle,
                        limit = wifiCustomLimit,
                        usage = currentCustomWifiUsage,
                        enabled = wifiCustomLimitEnabled,
                        networkType = "wifi",
                        subtitle = formatRange(wifiCustomLimitStart, wifiCustomLimitEnd),
                        onToggle = { viewModel.setWifiCustomLimitEnabled(it) }
                    )
                )
            }
            if (dataCustomLimitConfigured) {
                add(
                    SystemPlanItem(
                        type = "custom_mobile",
                        title = customMobileTitle,
                        limit = dataCustomLimit,
                        usage = currentCustomMobileUsage,
                        enabled = dataCustomLimitEnabled,
                        networkType = "mobile",
                        subtitle = formatRange(dataCustomLimitStart, dataCustomLimitEnd),
                        onToggle = { viewModel.setDataCustomLimitEnabled(it) }
                    )
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.title_general_limits),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (!allGeneralLimitsConfigured) {
                        TextButton(
                            onClick = {
                                if (unconfiguredPlans.size == 1) {
                                    viewModel.configuringGeneralLimitType = unconfiguredPlans.first()
                                } else {
                                    showAddGeneralLimitDialog = true
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_add_plan), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (activePlansList.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SetupPlanPlaceholderTile(
                            onClick = { showAddGeneralLimitDialog = true }
                        )
                    }
                }
            } else {
                itemsIndexed(activePlansList) { index, plan ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        StaggeredEntrance(index = index) {
                            SystemPlanCard(
                                title = plan.title,
                                usage = plan.usage,
                                limit = plan.limit,
                                enabled = plan.enabled,
                                networkType = plan.networkType,
                                subtitle = plan.subtitle,
                                onToggle = plan.onToggle,
                                onCardClick = { viewModel.configuringGeneralLimitType = plan.type },
                                onDelete = {
                                    val title = plan.title
                                    val pType = plan.type
                                    val limitVal = plan.limit
                                    val netType = plan.networkType
                                    val periodType = if (pType.startsWith("daily")) "daily" else if (pType.startsWith("monthly")) "monthly" else "custom"
                                    limitToDelete = AppLimit(
                                        packageName = "system.${netType}.${periodType}",
                                        appName = title,
                                        dataLimit = limitVal,
                                        limitType = periodType,
                                        networkType = netType
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_app_limits),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    TextButton(
                        onClick = {
                            viewModel.loadInstalledApps()
                            viewModel.isPickerOpen = true
                        }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_add_app_limit), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (appLimits.isEmpty()) {
                item {
                    EmptyAppLimitsPlaceholder(
                        onAddClick = {
                            viewModel.loadInstalledApps()
                            viewModel.isPickerOpen = true
                        }
                    )
                }
            } else {
                itemsIndexed(filteredAppLimits) { index, limit ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        StaggeredEntrance(index = activePlansList.size + index) {
                            AppLimitItem(
                                limit = limit,
                                onToggle = { enabled -> viewModel.updateAppLimit(limit.copy(isEnabled = enabled)) },
                                onToggleManualBlock = { blocked -> viewModel.updateAppLimit(limit.copy(isManuallyBlocked = blocked)) },
                                onDelete = { limitToDelete = limit },
                                onEdit = { viewModel.editingLimit = limit }
                            )
                        }
                    }
                }
            }
        }

    }

    if (limitToDelete != null) {
        AlertDialog(
            onDismissRequest = { limitToDelete = null },
            title = { Text(stringResource(R.string.title_delete_limit)) },
            text = { Text(stringResource(R.string.msg_confirm_delete_limit, limitToDelete?.appName ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        limitToDelete?.let {
                            when(it.packageName) {
                                "system.wifi.daily" -> {
                                    viewModel.setWifiDailyLimitEnabled(false)
                                    viewModel.setWifiDailyLimitConfigured(false)
                                }
                                "system.mobile.daily" -> {
                                    viewModel.setDataDailyLimitEnabled(false)
                                    viewModel.setDataDailyLimitConfigured(false)
                                }
                                "system.four_g.daily" -> {
                                    viewModel.setFourGDailyLimitEnabled(false)
                                    viewModel.setFourGDailyLimitConfigured(false)
                                }
                                "system.wifi.monthly" -> {
                                    viewModel.setWifiMonthlyLimitEnabled(false)
                                    viewModel.setWifiMonthlyLimitConfigured(false)
                                }
                                "system.mobile.monthly" -> {
                                    viewModel.setDataMonthlyLimitEnabled(false)
                                    viewModel.setDataMonthlyLimitConfigured(false)
                                }
                                "system.wifi.custom" -> {
                                    viewModel.setWifiCustomLimitEnabled(false)
                                    viewModel.setWifiCustomLimitConfigured(false)
                                }
                                "system.mobile.custom" -> {
                                    viewModel.setDataCustomLimitEnabled(false)
                                    viewModel.setDataCustomLimitConfigured(false)
                                }
                                else -> viewModel.removeAppLimit(it)
                            }
                        }
                        limitToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { limitToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun AppLimitsOverlay(viewModel: AppLimitsViewModel) {
    val editingLimit = viewModel.editingLimit
    val configuringGeneralLimitType = viewModel.configuringGeneralLimitType

    val dataDailyLimit by viewModel.dataDailyLimit.collectAsState()
    val wifiDailyLimit by viewModel.wifiDailyLimit.collectAsState()
    val fourGDailyLimit by viewModel.fourGDailyLimit.collectAsState()
    val dataMonthlyLimit by viewModel.dataMonthlyLimit.collectAsState()
    val wifiMonthlyLimit by viewModel.wifiMonthlyLimit.collectAsState()
    val dataCustomLimit by viewModel.dataCustomLimit.collectAsState()
    val wifiCustomLimit by viewModel.wifiCustomLimit.collectAsState()
    val dataCustomLimitStart by viewModel.dataCustomLimitStart.collectAsState()
    val dataCustomLimitEnd by viewModel.dataCustomLimitEnd.collectAsState()
    val wifiCustomLimitStart by viewModel.wifiCustomLimitStart.collectAsState()
    val wifiCustomLimitEnd by viewModel.wifiCustomLimitEnd.collectAsState()



    if (editingLimit != null) {
        AppLimitEditScreen(
            limit = editingLimit,
            onBack = { viewModel.editingLimit = null },
        ) { updatedLimit ->
            viewModel.updateAppLimit(updatedLimit)
            viewModel.editingLimit = null
        }
    }

    if (configuringGeneralLimitType != null) {
        val initialLimit = when (configuringGeneralLimitType) {
            "daily_wifi" -> wifiDailyLimit
            "daily_mobile" -> dataDailyLimit
            "daily_four_g" -> fourGDailyLimit
            "monthly_wifi" -> wifiMonthlyLimit
            "monthly_mobile" -> dataMonthlyLimit
            "custom_wifi" -> wifiCustomLimit
            "custom_mobile" -> dataCustomLimit
            else -> 0L
        }
        val initialStart = when (configuringGeneralLimitType) {
            "custom_wifi" -> wifiCustomLimitStart
            "custom_mobile" -> dataCustomLimitStart
            else -> 0L
        }
        val initialEnd = when (configuringGeneralLimitType) {
            "custom_wifi" -> wifiCustomLimitEnd
            "custom_mobile" -> dataCustomLimitEnd
            else -> 0L
        }

        GeneralLimitConfigScreen(
            planType = configuringGeneralLimitType,
            initialLimit = initialLimit,
            initialStart = initialStart,
            initialEnd = initialEnd,
            onBack = { viewModel.configuringGeneralLimitType = null },
            onConfirm = { limitBytes, start, end ->
                when (configuringGeneralLimitType) {
                    "daily_wifi" -> {
                        viewModel.setWifiDailyLimit(limitBytes)
                        viewModel.setWifiDailyLimitEnabled(enabled = true)
                        viewModel.setWifiDailyLimitConfigured(configured = true)
                    }
                    "daily_mobile" -> {
                        viewModel.setDataDailyLimit(limitBytes)
                        viewModel.setDataDailyLimitEnabled(enabled = true)
                        viewModel.setDataDailyLimitConfigured(configured = true)
                    }
                    "daily_four_g" -> {
                        viewModel.setFourGDailyLimit(limitBytes)
                        viewModel.setFourGDailyLimitEnabled(enabled = true)
                        viewModel.setFourGDailyLimitConfigured(configured = true)
                    }
                    "monthly_wifi" -> {
                        viewModel.setWifiMonthlyLimit(limitBytes)
                        viewModel.setWifiMonthlyLimitEnabled(enabled = true)
                        viewModel.setWifiMonthlyLimitConfigured(configured = true)
                    }
                    "monthly_mobile" -> {
                        viewModel.setDataMonthlyLimit(limitBytes)
                        viewModel.setDataMonthlyLimitEnabled(enabled = true)
                        viewModel.setDataMonthlyLimitConfigured(configured = true)
                    }
                    "custom_wifi" -> {
                        viewModel.setWifiCustomLimit(limitBytes)
                        viewModel.setWifiCustomLimitRange(start, end)
                        viewModel.setWifiCustomLimitEnabled(enabled = true)
                        viewModel.setWifiCustomLimitConfigured(configured = true)
                    }
                    "custom_mobile" -> {
                        viewModel.setDataCustomLimit(limitBytes)
                        viewModel.setDataCustomLimitRange(start, end)
                        viewModel.setDataCustomLimitEnabled(enabled = true)
                        viewModel.setDataCustomLimitConfigured(configured = true)
                    }
                }
                viewModel.configuringGeneralLimitType = null
            }
        )
    }
}

@Composable
fun AppLimitItem(
    limit: AppLimit,
    onToggle: (Boolean) -> Unit,
    onToggleManualBlock: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appIcon = remember(limit.packageName) {
        try {
            context.packageManager.getApplicationIcon(limit.packageName)
        } catch (_: Exception) {
            null
        }
    }

    val wifiProgress = if (limit.wifiDataLimit > 0) (limit.currentWifiUsage.toFloat() / limit.wifiDataLimit).coerceIn(0f, 1f) else 0f
    val mobileProgress = if (limit.mobileDataLimit > 0) (limit.currentMobileUsage.toFloat() / limit.mobileDataLimit).coerceIn(0f, 1f) else 0f

    val wifiColor = MaterialTheme.colorScheme.secondary
    val mobileColor = MaterialTheme.colorScheme.tertiary

    val isWifiOver = limit.isEnabled && limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit
    val isMobileOver = limit.isEnabled && limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .bounceClick { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                limit.isManuallyBlocked -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                limit.isEnabled -> MaterialTheme.colorScheme.surfaceContainer
                else -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (limit.isEnabled || limit.isManuallyBlocked) 1.5.dp else 1.dp,
            color = when {
                limit.isManuallyBlocked -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                limit.isEnabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                        alpha = if (limit.isEnabled || limit.isManuallyBlocked) 1f else 0.5f,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = if (limit.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = limit.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (limit.isEnabled || limit.isManuallyBlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    
                    if (limit.isEnabled && (isWifiOver || isMobileOver)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_limit_reached),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = { onToggleManualBlock(!limit.isManuallyBlocked) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (limit.isManuallyBlocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        contentColor = if (limit.isManuallyBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (limit.isManuallyBlocked) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (limit.isManuallyBlocked) "Blocked" else "Block",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(4.dp))

                Switch(
                    checked = limit.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }

            if (limit.isEnabled && !limit.isManuallyBlocked) {
                if ((limit.networkType == "both") || (limit.networkType == "wifi")) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = null,
                            tint = wifiColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.label_wifi),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = wifiColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(wifiColor.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(wifiProgress)
                                .background(
                                    color = if (limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit) MaterialTheme.colorScheme.error else wifiColor,
                                    shape = CircleShape
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit) MaterialTheme.colorScheme.errorContainer
                                            else wifiColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(wifiProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (limit.wifiDataLimit > 0 && limit.currentWifiUsage >= limit.wifiDataLimit) MaterialTheme.colorScheme.onErrorContainer else wifiColor
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatUsage(limit.currentWifiUsage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " / ${formatUsage(limit.wifiDataLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }

                if ((limit.networkType == "both") || (limit.networkType == "mobile")) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SignalCellularAlt,
                            contentDescription = null,
                            tint = mobileColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.label_mobile),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = mobileColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(mobileColor.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(mobileProgress)
                                .background(
                                    color = if (limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit) MaterialTheme.colorScheme.error else mobileColor,
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit) MaterialTheme.colorScheme.errorContainer
                                            else mobileColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(mobileProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (limit.mobileDataLimit > 0 && limit.currentMobileUsage >= limit.mobileDataLimit) MaterialTheme.colorScheme.onErrorContainer else mobileColor
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatUsage(limit.currentMobileUsage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " / ${formatUsage(limit.mobileDataLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.btn_edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddGeneralLimitDialog(
    unconfiguredPlans: List<String>,
    onDismiss: () -> Unit,
    onPlanSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.title_select_plan_type),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                unconfiguredPlans.forEach { planType ->
                    val periodText = when {
                        planType.startsWith("daily") -> stringResource(R.string.filter_daily)
                        planType.startsWith("monthly") -> stringResource(R.string.filter_monthly)
                        else -> stringResource(R.string.filter_custom)
                    }
                    val networkText = when {
                        planType.endsWith("wifi") -> stringResource(R.string.label_wifi)
                        planType.endsWith("four_g") -> stringResource(R.string.label_four_g)
                        else -> stringResource(R.string.label_mobile)
                    }
                    val icon = when {
                        planType.endsWith("wifi") -> Icons.Rounded.Wifi
                        else -> Icons.Rounded.SignalCellularAlt
                    }
                    val accentColor = when {
                        planType.endsWith("wifi") -> MaterialTheme.colorScheme.secondary
                        planType.endsWith("four_g") -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                    
                    Surface(
                        onClick = { onPlanSelected(planType) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = accentColor.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "$periodText $networkText",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun SystemPlanCard(
    title: String,
    usage: Long,
    limit: Long,
    enabled: Boolean,
    networkType: String,
    subtitle: String? = null,
    onToggle: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (limit > 0) (usage.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val isOverLimit = enabled && (limit > 0) && (usage >= limit)
    
    val accentColor = when (networkType) {
        "wifi" -> MaterialTheme.colorScheme.secondary
        "four_g" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    val backgroundBrush = if (enabled) {
        Brush.linearGradient(
            colors = listOf(
                accentColor,
                accentColor.copy(alpha = 0.6f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .bounceClick { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = if (enabled) 2.dp else 1.dp,
            color = if (enabled) accentColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(20.dp)
        ) {
            if (enabled) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(x = 180.dp, y = (-40).dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .offset(x = 210.dp, y = 60.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                )
            }

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (enabled) Color.White.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (networkType) {
                                    "wifi" -> Icons.Rounded.Wifi
                                    else -> Icons.Rounded.SignalCellularAlt
                                },
                                contentDescription = null,
                                tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (enabled) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (enabled) Color.White else MaterialTheme.colorScheme.outline,
                            checkedTrackColor = if (enabled) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.scale(0.7f)
                    )
                }

                if (enabled) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formatUsage(usage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = " / ${formatUsage(limit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOverLimit) MaterialTheme.colorScheme.error else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(
                                        color = if (isOverLimit) MaterialTheme.colorScheme.error else Color.White,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_inactive),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.cd_delete_plan),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupPlanPlaceholderTile(
    onClick: () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 145.dp)
            .bounceClick { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 145.dp)
                .drawBehind {
                    val stroke = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                    drawRoundRect(
                        color = outlineColor,
                        style = stroke,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                }
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.label_configure_network_plan),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.desc_prevent_overages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EmptyAppLimitsPlaceholder(
    onAddClick: () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 145.dp)
            .padding(horizontal = 20.dp)
            .bounceClick { onAddClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 145.dp)
                .drawBehind {
                    val stroke = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                    drawRoundRect(
                        color = outlineColor,
                        style = stroke,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                }
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.msg_no_restricted_apps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.desc_restrict_app_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
