// Form dialog allowing user to configure or delete individual app limits,
// including daily Wi-Fi/mobile data bounds and toggling block behavior.
package com.ray.flowmeter.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ray.flowmeter.R
import com.ray.flowmeter.data.AppLimit
import com.ray.flowmeter.ui.dialogs.AnimatedDialogContent
import com.ray.flowmeter.utils.UnitUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitEditScreen(
    limit: AppLimit,
    onBack: () -> Unit,
    onConfirm: (AppLimit) -> Unit,
) {
    val initialData = remember(limit) { UnitUtils.bytesToUiState(limit.dataLimit) }
    val (limitInput, setLimitInput) = remember(limit) { 
        mutableStateOf(if (limit.dataLimit <= 0L) "100" else initialData.first) 
    }
    val (limitUnit, setLimitUnit) = remember(limit) { mutableStateOf(initialData.second) }

    val initialWifi = remember(limit) { UnitUtils.bytesToUiState(limit.wifiDataLimit) }
    val (wifiLimitInput, setWifiLimitInput) = remember(limit) { 
        mutableStateOf(if (limit.wifiDataLimit <= 0L) "100" else initialWifi.first) 
    }
    val (wifiLimitUnit, setWifiLimitUnit) = remember(limit) { mutableStateOf(initialWifi.second) }

    val initialMobile = remember(limit) { UnitUtils.bytesToUiState(limit.mobileDataLimit) }
    val (mobileLimitInput, setMobileLimitInput) = remember(limit) { 
        mutableStateOf(if (limit.mobileDataLimit <= 0L) "100" else initialMobile.first) 
    }
    val (mobileLimitUnit, setMobileLimitUnit) = remember(limit) { mutableStateOf(initialMobile.second) }

    val (limitType, setLimitType) = remember(limit) { mutableStateOf(limit.limitType) }
    val (networkType, setNetworkType) = remember(limit) {
        val initialTypes = if (limit.networkType == "both") {
            setOf("wifi", "mobile")
        } else {
            limit.networkType.split(",").filter { it.isNotBlank() }.toSet()
        }
        mutableStateOf(initialTypes)
    }

    var isManuallyBlocked by remember(limit) { mutableStateOf(limit.isManuallyBlocked) }

    val context = LocalContext.current
    val appIcon = remember(limit.packageName) {
        try {
            context.packageManager.getApplicationIcon(limit.packageName)
        } catch (_: Exception) {
            null
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        AnimatedDialogContent(onBack = onBack) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                ConfigurationContent(
                    selectedAppHeader = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(56.dp),
                            ) {
                                Box(Modifier.padding(10.dp)) {
                                    appIcon?.let {
                                        Image(
                                            bitmap = it.toBitmap(120, 120).asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(limit.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text(limit.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            FilledTonalButton(
                                onClick = { isManuallyBlocked = !isManuallyBlocked },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isManuallyBlocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                    contentColor = if (isManuallyBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isManuallyBlocked) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isManuallyBlocked) "Blocked" else "Block Internet",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    limitInput = limitInput,
                    onLimitInputChange = setLimitInput,
                    limitUnit = limitUnit,
                    onLimitUnitChange = setLimitUnit,
                    limitType = limitType,
                    onLimitTypeChange = setLimitType,
                    networkType = networkType,
                    onNetworkTypeChange = setNetworkType,
                    wifiLimitInput = wifiLimitInput,
                    onWifiLimitInputChange = setWifiLimitInput,
                    wifiLimitUnit = wifiLimitUnit,
                    onWifiLimitUnitChange = setWifiLimitUnit,
                    mobileLimitInput = mobileLimitInput,
                    onMobileLimitInputChange = setMobileLimitInput,
                    mobileLimitUnit = mobileLimitUnit,
                    onMobileLimitUnitChange = setMobileLimitUnit,
                    confirmButtonText = stringResource(R.string.btn_save_config),
                    onCancel = onBack,
                    onConfirm = {
                        if (networkType.isEmpty()) {
                            android.widget.Toast.makeText(context, "${limit.appName} Limit not selected", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirm(
                                limit.copy(
                                    dataLimit = UnitUtils.uiStateToBytes(limitInput, limitUnit),
                                    limitType = limitType,
                                    networkType = networkType.joinToString(","),
                                    wifiDataLimit = UnitUtils.uiStateToBytes(wifiLimitInput, wifiLimitUnit),
                                    mobileDataLimit = UnitUtils.uiStateToBytes(mobileLimitInput, mobileLimitUnit),
                                    isBlocked = false,
                                    isWifiBlocked = false,
                                    isMobileBlocked = false,
                                    isManuallyBlocked = isManuallyBlocked,
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}
