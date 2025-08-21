package dev.aaa1115910.glyphrecorder.ui.screens

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.glyphrecorder.R
import dev.aaa1115910.glyphrecorder.ui.GlyphRecorderTheme
import dev.aaa1115910.glyphrecorder.util.OpenCvUtil
import dev.aaa1115910.glyphrecorder.util.Prefs
import dev.aaa1115910.glyphrecorder.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.pow


@Composable
fun CorrectionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val logger = KotlinLogging.logger("CorrectionScreen")

    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

    var screenshotUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var screenshotBitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }
    val screenshotCircles = rememberSaveable { mutableStateListOf<Pair<Int, Int>>() }
    val confirmedCircles = rememberSaveable { mutableStateListOf<Pair<Int, Int>>() }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                logger.info { "Photo picker selected uri: $uri" }
                screenshotUri = uri
            } else {
                logger.info { "Photo picker no media selected" }
            }
        }

    val onExit = {
        (context as Activity).finish()
    }

    val onChooserImage = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val onSave = {
        Prefs.circles = confirmedCircles
        OpenCvUtil.updatePrefCircles(confirmedCircles)
        context.getString(R.string.correction_screen_save_success).toast(context)
    }

    val onConfirmClick: (Int, Int) -> Unit = { x, y ->
        logger.info { "on confirm click at ($x, $y)" }
        // 找到最近的自动识别点
        val nearest = screenshotCircles.minByOrNull { (cx, cy) ->
            (cx - x) * (cx - x) + (cy - y) * (cy - y)
        }
        logger.info { "nearest circle: $nearest" }
        // 距离阈值，比如 60 像素
        val threshold = 60.0
        if (nearest != null && !confirmedCircles.contains(nearest)) {
            val dist =
                (nearest.first - x) * (nearest.first - x) + (nearest.second - y) * (nearest.second - y)
            logger.info { "Distance to nearest circle: $dist" }
            if (dist < threshold.pow(2) && confirmedCircles.size < 11) { // 60^2 = 3600
                confirmedCircles.add(nearest)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        }
    }

    val onClearConfirmedCircles = {
        logger.info { "Clearing confirmed circles" }
        confirmedCircles.clear()
    }

    LaunchedEffect(screenshotUri) {
        if (screenshotUri == null) return@LaunchedEffect
        val inputStream = context.contentResolver.openInputStream(screenshotUri!!)
        screenshotBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val circles = OpenCvUtil.detectCircles(screenshotBitmap!!)
        logger.info { "Circles: $circles" }
        for ((index, pair) in circles.withIndex()) {
            logger.info { "Circle $index: x=${pair.first}, y=${pair.second}" }
        }
        screenshotCircles.clear()
        confirmedCircles.clear()
        screenshotCircles.addAll(circles)
        if (circles.size == 11) confirmedCircles.addAll(circles)
    }

    CorrectionScreenContent(
        modifier = modifier,
        screenAspectRatio = screenAspectRatio,
        screenshotBitmap = screenshotBitmap,
        screenshotCircles = screenshotCircles,
        confirmedCircles = confirmedCircles,
        onExit = onExit,
        onChooserImage = onChooserImage,
        onSave = onSave,
        onConfirmClick = onConfirmClick,
        onClearConfirmedCircles = onClearConfirmedCircles
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CorrectionScreenContent(
    modifier: Modifier = Modifier,
    screenAspectRatio: Float,
    screenshotBitmap: Bitmap?,
    screenshotCircles: List<Pair<Int, Int>>,
    confirmedCircles: List<Pair<Int, Int>>,
    onExit: () -> Unit,
    onChooserImage: () -> Unit,
    onSave: () -> Unit,
    onConfirmClick: (Int, Int) -> Unit,
    onClearConfirmedCircles: () -> Unit
) {
    val saveAvailable by remember { derivedStateOf { confirmedCircles.size == 11 } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.activity_title_correction)) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = saveAvailable
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CorrectionPreview(
                modifier = Modifier
                    .fillMaxWidth(0.7f),
                screenAspectRatio = screenAspectRatio,
                image = screenshotBitmap,
                screenshotCircles = screenshotCircles,
                confirmedCircles = confirmedCircles,
                onConfirmClick = onConfirmClick
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onChooserImage) {
                    Text(text = stringResource(R.string.correction_screen_choose_image))
                }
                AnimatedVisibility(confirmedCircles.isNotEmpty()) {
                    FilledTonalButton(onClick = onClearConfirmedCircles) {
                        Text(text = stringResource(R.string.correction_screen_reset_points))
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Tip"
                )
                Text(
                    text = stringResource(R.string.correction_screen_image_preview_tip),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        }
    }
}

@Composable
private fun CorrectionPreview(
    modifier: Modifier = Modifier,
    screenAspectRatio: Float,
    image: Bitmap?,
    screenshotCircles: List<Pair<Int, Int>>,
    confirmedCircles: List<Pair<Int, Int>>,
    onConfirmClick: (Int, Int) -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val textMeasurer = rememberTextMeasurer()

    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { screenHeightDp.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(screenAspectRatio)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .pointerInput(screenshotCircles, confirmedCircles) {
                detectTapGestures { offset ->
                    println("Tapped at: $offset")
                    val scaleX = (image?.width?.toFloat() ?: screenWidthPx) / size.width
                    val scaleY = (image?.height?.toFloat() ?: screenHeightPx) / size.height
                    println("Scale: $scaleX, $scaleY")
                    val x = (offset.x * scaleX).toInt()
                    val y = (offset.y * scaleY).toInt()
                    println("Scaled coordinates: ($x, $y)")
                    onConfirmClick(x, y)
                }
            }
    ) {
        if (image == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                //Text(
                //    text = "未选择图片",
                //    style = MaterialTheme.typography.bodyLarge,
                //    color = MaterialTheme.colorScheme.onSurfaceVariant
                //)
                Text(
                    text = "请选择空白的画符截图",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = image.asImageBitmap(),
                contentDescription = "Correction Preview",
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / image.width
                val scaleY = size.height / image.height
                screenshotCircles.forEachIndexed { index, (x, y) ->
                    // 绘制所有点
                    drawCircle(
                        color = Color.Red,
                        radius = 10.dp.toPx(),
                        center = Offset(
                            x = x * scaleX,
                            y = y * scaleY
                        ),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // 绘制已确认点的序号
                    confirmedCircles.forEachIndexed { idx, (x, y) ->
                        // 在圆圈右上角绘制序号
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${idx + 1}",
                            topLeft = Offset(
                                x = x * scaleX + 12.dp.toPx(),
                                y = y * scaleY - 12.dp.toPx()
                            ),
                            style = TextStyle(
                                color = Color.Red,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CorrectionScreenPreview(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

    GlyphRecorderTheme {
        CorrectionScreenContent(
            screenAspectRatio = screenAspectRatio,
            screenshotBitmap = null,
            screenshotCircles = emptyList(),
            confirmedCircles = emptyList(),
            onExit = {},
            onChooserImage = {},
            onSave = {},
            onConfirmClick = { _, _ -> },
            onClearConfirmedCircles = {}
        )
    }
}