package dev.aaa1115910.glyphrecorder.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.SecurityUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.aaa1115910.glyphrecorder.BuildConfig
import dev.aaa1115910.glyphrecorder.R
import dev.aaa1115910.glyphrecorder.WorkingMode
import dev.aaa1115910.glyphrecorder.activities.CorrectionActivity
import dev.aaa1115910.glyphrecorder.services.MediaProjectionService
import dev.aaa1115910.glyphrecorder.services.WorkingQuickSettingsTileService
import dev.aaa1115910.glyphrecorder.ui.GlyphRecorderTheme
import dev.aaa1115910.glyphrecorder.ui.components.preferences.items.radioPreference
import dev.aaa1115910.glyphrecorder.ui.components.preferences.items.switchPreference
import dev.aaa1115910.glyphrecorder.ui.components.preferences.items.textPreference
import dev.aaa1115910.glyphrecorder.ui.components.preferences.preferenceGroups
import dev.aaa1115910.glyphrecorder.util.DetectorPermissionUtil
import dev.aaa1115910.glyphrecorder.util.GithubApi
import dev.aaa1115910.glyphrecorder.util.PrefKeys
import dev.aaa1115910.glyphrecorder.util.Prefs
import dev.aaa1115910.glyphrecorder.util.RELEASE_URL
import dev.aaa1115910.glyphrecorder.util.ShizukuConfig
import dev.aaa1115910.glyphrecorder.util.SmartDisplayCoordinator
import dev.aaa1115910.glyphrecorder.util.WorkingSessionController
import dev.aaa1115910.glyphrecorder.util.dataStore
import dev.aaa1115910.glyphrecorder.util.getPreferenceAsState
import dev.aaa1115910.glyphrecorder.util.openHackEmulator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("HomeScreen")

    var initialized by rememberSaveable { mutableStateOf(false) }
    val working by context.dataStore.getPreferenceAsState(PrefKeys.workingRequest)
    val workingModeOrdinal by context.dataStore.getPreferenceAsState(PrefKeys.workingModeRequest)
    val smartDisplayEnabled by context.dataStore.getPreferenceAsState(PrefKeys.smartDisplayEnabledRequest)
    val workingMode by remember {
        derivedStateOf { WorkingMode.entries[workingModeOrdinal] }
    }
    var hasUpdate by remember { mutableStateOf(false) }
    var showDetectorUnavailableDialog by rememberSaveable { mutableStateOf(false) }
    var detectorAvailable by remember { mutableStateOf(SmartDisplayCoordinator.isCurrentDetectorAvailable()) }
    var batteryOptimizationIgnored by remember {
        mutableStateOf(DetectorPermissionUtil.isIgnoringBatteryOptimizations(context))
    }
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshPermissionState() {
        detectorAvailable = SmartDisplayCoordinator.isCurrentDetectorAvailable()
        batteryOptimizationIgnored = DetectorPermissionUtil.isIgnoringBatteryOptimizations(context)
    }

    fun updateWorking(value: Boolean) {
        Prefs.working = value
        WorkingQuickSettingsTileService.requestStateSync(context)
    }

    fun updateSmartDisplayEnabled(value: Boolean) {
        Prefs.smartDisplayEnabled = value
    }

    fun syncWorkingMode() {
        SmartDisplayCoordinator.onWorkingModeChanged(
            working = Prefs.working,
            smartDisplayEnabled = Prefs.smartDisplayEnabled
        )
    }

    val showSnackbarMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    val mediaProjectionRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        logger.info { "mediaProjectionRequestLauncher result is ok: ${it.resultCode == Activity.RESULT_OK}" }
        if (it.resultCode == Activity.RESULT_OK) {
            if (working) return@rememberLauncherForActivityResult
            MediaProjectionService.resultCode = it.resultCode
            MediaProjectionService.resultData = it.data
            context.startForegroundService(Intent(context, MediaProjectionService::class.java))
            updateWorking(true)
            syncWorkingMode()
        } else {
            logger.info { "MediaProjection request failed" }
            showSnackbarMessage(context.getString(R.string.start_service_failed_media_projection))
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            logger.info { "request overlay permission success" }
            showSnackbarMessage(context.getString(R.string.overlay_permission_success))
        } else {
            logger.info { "request overlay permission failed" }
            showSnackbarMessage(context.getString(R.string.overlay_permission_failed))
        }
    }

    fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri()
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            showSnackbarMessage(context.getString(R.string.overlay_permission_success))
        }
    }

    fun requestAccessibilityPermission() {
        DetectorPermissionUtil.openAccessibilitySettings(context)
    }

    val launchCorrectionActivity = {
        val intent = Intent(context, CorrectionActivity::class.java)
        context.startActivity(intent)
    }

    val onStart = {
        logger.info { "start service, working mode: $workingMode" }
        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)

        if (Prefs.circles.isEmpty()) {
            showSnackbarMessage(context.getString(R.string.start_service_failed_need_correction_position))
        } else if (Prefs.circles.size != 11) {
            showSnackbarMessage(context.getString(R.string.start_service_failed_error_positions))
        } else if (working) {
            showSnackbarMessage(context.getString(R.string.start_service_failed_already_running))
        } else {
            when (workingMode) {
                WorkingMode.MediaProjection -> {
                    val mediaProjectionManager =
                        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent()
                    mediaProjectionRequestLauncher.launch(screenCaptureIntent)
                }

                WorkingMode.Shizuku -> {
                    val launchService = {
                        val circlesRawString =
                            Prefs.circles.joinToString(",") { "${it.first}:${it.second}" }
                        ShizukuConfig.screenCaptureService!!.updateCircles(circlesRawString)
                        val startResult = ShizukuConfig.screenCaptureService!!.startScreenCapture()
                        if (startResult) {
                            updateWorking(true)
                            syncWorkingMode()
                        } else {
                            showSnackbarMessage(context.getString(R.string.start_service_failed_shizuku))
                            logger.error { "Failed to start Shizuku ScreenCaptureService" }
                        }
                    }

                    if (ShizukuConfig.isShizukuConnected) {
                        launchService()
                    } else {
                        ShizukuConfig.initShizuku(
                            onServiceConnected = { launchService() }
                        )
                    }
                }
            }
        }
    }

    val onStop = {
        logger.info { "stop service, working mode: $workingMode" }
        haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
        updateWorking(false)
        WorkingSessionController.stopCurrentSession(context)
        syncWorkingMode()
    }

    val onWorkingChange: (Boolean) -> Unit = {
        if (it) {
            if (Settings.canDrawOverlays(context)) {
                onStart()
            } else {
                requestOverlayPermission()
            }
        } else {
            onStop()
        }
    }

    val onSmartDisplayChange: (Boolean) -> Unit = { newValue ->
        if (newValue && !SmartDisplayCoordinator.isCurrentDetectorAvailable()) {
            showDetectorUnavailableDialog = true
        } else {
            updateSmartDisplayEnabled(newValue)
            syncWorkingMode()
        }
    }

    val checkUpdate: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val latestRelease = GithubApi.getLatestRelease()
                hasUpdate = latestRelease.tagName != "r${BuildConfig.VERSION_CODE}_${
                    BuildConfig.VERSION_NAME.replace(" ", "_")
                }"
            }.onFailure {
                logger.error(it) { "Failed to check for updates" }
                hasUpdate = false
            }
        }
    }

    val onOpenReleasePage: () -> Unit = {
        val releaseUrl = RELEASE_URL
        val intent = Intent(Intent.ACTION_VIEW, releaseUrl.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        logger.info { "Open release page: $releaseUrl" }
    }

    val onOpenBatteryOptimizationSettings: () -> Unit = {
        if (DetectorPermissionUtil.isIgnoringBatteryOptimizations(context)) {
            DetectorPermissionUtil.openBatteryOptimizationSettings(context)
        } else {
            DetectorPermissionUtil.requestIgnoreBatteryOptimization(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!initialized) {
            checkUpdate()
        }
        initialized = true
        refreshPermissionState()
        SmartDisplayCoordinator.onAppStarted()
    }

    LaunchedEffect(working, smartDisplayEnabled) {
        syncWorkingMode()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HomeScreenContent(
        modifier = modifier,
        working = working,
        smartDisplayEnabled = smartDisplayEnabled,
        hasUpdate = hasUpdate,
        detectorAvailable = detectorAvailable,
        batteryOptimizationIgnored = batteryOptimizationIgnored,
        snackbarHostState = snackbarHostState,
        onWorkingChange = onWorkingChange,
        onSmartDisplayChange = onSmartDisplayChange,
        onOpenDetectorPermissionSettings = { requestAccessibilityPermission() },
        onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
        onCorrection = launchCorrectionActivity,
        onOpenReleasePage = onOpenReleasePage
    )

    if (showDetectorUnavailableDialog) {
        DetectorUnavailableDialog(
            onDismissRequest = { showDetectorUnavailableDialog = false },
            onGrantPermission = {
                showDetectorUnavailableDialog = false
                requestAccessibilityPermission()
            },
            onDisableSmartDisplay = {
                showDetectorUnavailableDialog = false
                updateSmartDisplayEnabled(false)
                syncWorkingMode()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    modifier: Modifier = Modifier,
    working: Boolean,
    smartDisplayEnabled: Boolean,
    hasUpdate: Boolean,
    detectorAvailable: Boolean,
    batteryOptimizationIgnored: Boolean,
    snackbarHostState: SnackbarHostState,
    onWorkingChange: (Boolean) -> Unit,
    onSmartDisplayChange: (Boolean) -> Unit,
    onOpenDetectorPermissionSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onCorrection: () -> Unit,
    onOpenReleasePage: () -> Unit
) {
    val context = LocalContext.current
    val detectorPermissionSummary = if (detectorAvailable) {
        context.getString(R.string.detector_permission_granted)
    } else {
        context.getString(R.string.detector_permission_required_accessibility)
    }

    val batterySummary = if (batteryOptimizationIgnored) {
        context.getString(R.string.battery_optimization_ignored)
    } else {
        context.getString(R.string.battery_optimization_required)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StartCard(
                    working = working,
                    onWorkingChange = onWorkingChange
                )
                LazyColumn {
                    preferenceGroups(
                        null to {
                            radioPreference(
                                title = context.getString(R.string.home_screen_working_mode),
                                icon = Icons.Outlined.Screenshot,
                                prefReq = PrefKeys.workingModeRequest,
                                enabled = !working,
                                values = mapOf(
                                    WorkingMode.MediaProjection.ordinal to
                                            context.getString(R.string.working_mode_media_projection),
                                    WorkingMode.Shizuku.ordinal to
                                            context.getString(R.string.working_mode_shizuku)
                                )
                            )
                            switchPreference(
                                title = context.getString(R.string.home_screen_smart_display),
                                summary = context.getString(R.string.home_screen_smart_display_summary),
                                checked = smartDisplayEnabled,
                                onCheckedChange = onSmartDisplayChange
                            )
                            textPreference(
                                title = context.getString(R.string.home_screen_detector_permission),
                                summary = detectorPermissionSummary,
                                onClick = onOpenDetectorPermissionSettings
                            )
                            textPreference(
                                title = context.getString(R.string.home_screen_battery_optimization),
                                summary = batterySummary,
                                onClick = onOpenBatteryOptimizationSettings
                            )
                            textPreference(
                                title = context.getString(R.string.home_screen_correction),
                                icon = Icons.Default.MyLocation,
                                onClick = onCorrection
                            )
                        },
                        null to {
                            @Suppress("KotlinConstantConditions")
                            if (BuildConfig.BUILD_TYPE == "debug") {
                                textPreference(
                                    title = "Hack Emulator",
                                    icon = Icons.Default.BugReport,
                                    onClick = { openHackEmulator(context) }
                                )
                            }
                        },
                        null to {
                            if (hasUpdate) {
                                textPreference(
                                    title = context.getString(R.string.home_screen_update),
                                    summary = context.getString(R.string.home_screen_update_summary),
                                    icon = Icons.Outlined.SecurityUpdate,
                                    onClick = onOpenReleasePage
                                )
                            }
                            textPreference(
                                title = context.getString(R.string.home_screen_version),
                                summary = BuildConfig.VERSION_NAME,
                                icon = Icons.Outlined.Info,
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectorUnavailableDialog(
    onDismissRequest: () -> Unit,
    onGrantPermission: () -> Unit,
    onDisableSmartDisplay: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.detector_unavailable_title))
        },
        text = {
            Text(text = stringResource(R.string.detector_unavailable_accessibility))
        },
        confirmButton = {
            TextButton(onClick = onGrantPermission) {
                Text(text = stringResource(R.string.detector_unavailable_grant_permission))
            }
        },
        dismissButton = {
            TextButton(onClick = onDisableSmartDisplay) {
                Text(text = stringResource(R.string.detector_unavailable_disable_smart_display))
            }
        }
    )
}

@Composable
private fun StartCard(
    modifier: Modifier = Modifier,
    working: Boolean,
    onWorkingChange: (Boolean) -> Unit
) {
    var isFirst by remember { mutableStateOf(true) }
    var triggeredBySelf by remember { mutableStateOf(false) }

    LaunchedEffect(working) {
        if (isFirst) {
            isFirst = false
            return@LaunchedEffect
        }

        if (triggeredBySelf) {
            triggeredBySelf = false
        } else {
            onWorkingChange(working)
        }
    }

    ListItem(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(CircleShape)
            .clickable {
                triggeredBySelf = true
                onWorkingChange(!working)
            },
        headlineContent = {
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = if (working) stringResource(R.string.home_screen_stop_service)
                else stringResource(R.string.home_screen_start_service),
                style = MaterialTheme.typography.titleMedium
            )
        },
        trailingContent = {
            Switch(
                modifier = Modifier.padding(end = 8.dp),
                checked = working,
                onCheckedChange = {
                    triggeredBySelf = true
                    onWorkingChange(it)
                },
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StartCardPreview() {
    var working by remember { mutableStateOf(false) }

    GlyphRecorderTheme {
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
            StartCard(
                working = working,
                onWorkingChange = { working = it }
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    var working by remember { mutableStateOf(false) }
    var smartDisplayEnabled by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    GlyphRecorderTheme {
        HomeScreenContent(
            working = working,
            smartDisplayEnabled = smartDisplayEnabled,
            hasUpdate = true,
            detectorAvailable = false,
            batteryOptimizationIgnored = false,
            snackbarHostState = snackbarHostState,
            onWorkingChange = { working = it },
            onSmartDisplayChange = { smartDisplayEnabled = it },
            onOpenDetectorPermissionSettings = {},
            onOpenBatteryOptimizationSettings = {},
            onCorrection = {},
            onOpenReleasePage = {}
        )
    }
}
