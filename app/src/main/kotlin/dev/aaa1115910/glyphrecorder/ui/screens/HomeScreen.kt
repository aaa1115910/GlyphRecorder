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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import dev.aaa1115910.glyphrecorder.util.openHackEmulator
import dev.aaa1115910.glyphrecorder.BuildConfig
import dev.aaa1115910.glyphrecorder.R
import dev.aaa1115910.glyphrecorder.WorkingMode
import dev.aaa1115910.glyphrecorder.activities.CorrectionActivity
import dev.aaa1115910.glyphrecorder.services.MediaProjectionService
import dev.aaa1115910.glyphrecorder.ui.GlyphRecorderTheme
import dev.aaa1115910.glyphrecorder.ui.components.preferences.items.radioPreference
import dev.aaa1115910.glyphrecorder.ui.components.preferences.items.textPreference
import dev.aaa1115910.glyphrecorder.ui.components.preferences.preferenceGroups
import dev.aaa1115910.glyphrecorder.util.GithubApi
import dev.aaa1115910.glyphrecorder.util.PrefKeys
import dev.aaa1115910.glyphrecorder.util.Prefs
import dev.aaa1115910.glyphrecorder.util.RELEASE_URL
import dev.aaa1115910.glyphrecorder.util.ShizukuConfig
import dev.aaa1115910.glyphrecorder.util.dataStore
import dev.aaa1115910.glyphrecorder.util.getPreferenceAsState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onShowFloatingToolbox: () -> Unit,
    onCloseFloatingToolbox: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("HomeScreen")

    var initialized by rememberSaveable { mutableStateOf(false) }
    val working by context.dataStore.getPreferenceAsState(PrefKeys.workingRequest)
    val workingModeOrdinal by context.dataStore.getPreferenceAsState(PrefKeys.workingModeRequest)
    val workingMode by remember {
        derivedStateOf { WorkingMode.entries[workingModeOrdinal] }
    }
    var hasUpdate by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun updateWorking(value: Boolean) {
        Prefs.working = value
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
            //Toast.makeText(context, "ok", Toast.LENGTH_SHORT).show()

            if (working) return@rememberLauncherForActivityResult
            MediaProjectionService.resultCode = it.resultCode
            MediaProjectionService.resultData = it.data
            context.startForegroundService(Intent(context, MediaProjectionService::class.java))
            updateWorking(true)
            onShowFloatingToolbox()
        } else {
            //Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show()
            logger.info { "MediaProjection request failed" }
            showSnackbarMessage(context.getString(R.string.start_service_failed_media_projection))
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // 可在此处处理权限结果
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
                        // 更新 ScreenCaptureService 的坐标信息
                        val circlesRawString =
                            Prefs.circles.joinToString(",") { "${it.first}:${it.second}" }
                        ShizukuConfig.screenCaptureService!!.updateCircles(circlesRawString)
                        // 启动 ScreenCaptureService
                        val startResult = ShizukuConfig.screenCaptureService!!.startScreenCapture()
                        if (startResult) {
                            updateWorking(true)
                            onShowFloatingToolbox()
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
        when (workingMode) {
            WorkingMode.MediaProjection -> {
                updateWorking(false)
                onCloseFloatingToolbox()
            }

            WorkingMode.Shizuku -> {
                updateWorking(false)
                onCloseFloatingToolbox()
            }
        }
    }

    val onWorkingChange: (Boolean) -> Unit = {
        if (Settings.canDrawOverlays(context)) {
            if (it) {
                onStart()
            } else {
                onStop()
            }
        } else {
            requestOverlayPermission()
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

    LaunchedEffect(Unit) {
        if (!initialized) {
            Prefs.working = false
            checkUpdate()
        }
        initialized = true
    }

    HomeScreenContent(
        modifier = modifier,
        working = working,
        hasUpdate = hasUpdate,
        snackbarHostState = snackbarHostState,
        onWorkingChange = onWorkingChange,
        onCorrection = launchCorrectionActivity,
        onOpenReleasePage = onOpenReleasePage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    modifier: Modifier = Modifier,
    working: Boolean,
    hasUpdate: Boolean,
    snackbarHostState: SnackbarHostState,
    onWorkingChange: (Boolean) -> Unit,
    onCorrection: () -> Unit,
    onOpenReleasePage: () -> Unit
) {
    val context = LocalContext.current

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
private fun StartCard(
    modifier: Modifier = Modifier,
    working: Boolean,
    onWorkingChange: (Boolean) -> Unit
) {
    var isFirst by remember { mutableStateOf(true) }
    var triggeredBySelf by remember { mutableStateOf(false) }

    LaunchedEffect(working) {
        // 忽略初次启动时的调用
        if (isFirst) {
            isFirst = false
            return@LaunchedEffect
        }

        if (triggeredBySelf) {
            triggeredBySelf = false
        } else {
            // 外部状态变化时调用，例如通过系统对话框结束屏幕投射
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
    val snackbarHostState = remember { SnackbarHostState() }

    GlyphRecorderTheme {
        HomeScreenContent(
            working = working,
            hasUpdate = true,
            snackbarHostState = snackbarHostState,
            onWorkingChange = { working = it },
            onCorrection = {},
            onOpenReleasePage = {}
        )
    }
}
