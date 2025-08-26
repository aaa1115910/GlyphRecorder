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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

    var showOpenCvParamsPanel by remember { mutableStateOf(false) }
    var openCvHoughCirclesParams by remember {
        mutableStateOf(
            OpenCvHoughCirclesParams(
                dp = 1.2,
                minDist = 30.0,
                param1 = 50.0,
                param2 = 30.0,
                minRadius = 25,
                maxRadius = 30
            )
        )
    }

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

    val updateCircles: (OpenCvHoughCirclesParams) -> Unit = { params ->
        if (screenshotBitmap != null) {
            logger.info { "Updating circles with params: $params" }
            val circles = OpenCvUtil.detectCircles(
                screenshotBitmap!!,
                params.dp,
                params.minDist,
                params.param1,
                params.param2,
                params.minRadius,
                params.maxRadius
            )
            logger.info { "Circles: $circles" }
            for ((index, pair) in circles.withIndex()) {
                logger.info { "Circle $index: x=${pair.first}, y=${pair.second}" }
            }
            screenshotCircles.clear()
            confirmedCircles.clear()
            screenshotCircles.addAll(circles)
            if (circles.size == 11) confirmedCircles.addAll(circles)
        }
    }

    LaunchedEffect(screenshotUri) {
        if (screenshotUri == null) return@LaunchedEffect
        val inputStream = context.contentResolver.openInputStream(screenshotUri!!)
        screenshotBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        updateCircles(openCvHoughCirclesParams)
    }

    LaunchedEffect(openCvHoughCirclesParams) {
        if (screenshotUri == null || screenshotBitmap == null) return@LaunchedEffect
        updateCircles(openCvHoughCirclesParams)
    }

    CorrectionScreenContent(
        modifier = modifier,
        screenAspectRatio = screenAspectRatio,
        screenshotBitmap = screenshotBitmap,
        screenshotCircles = screenshotCircles,
        confirmedCircles = confirmedCircles,
        showOpenCvParamsPanel = showOpenCvParamsPanel,
        openCvHoughCirclesParams = openCvHoughCirclesParams,
        onExit = onExit,
        onChooserImage = onChooserImage,
        onSave = onSave,
        onConfirmClick = onConfirmClick,
        onClearConfirmedCircles = onClearConfirmedCircles,
        onShowOpenCvParamsPanel = { showOpenCvParamsPanel = true },
        onHideOpenCvParamsPanel = { showOpenCvParamsPanel = false },
        onChangeOpenCvHoughCirclesParams = { openCvHoughCirclesParams = it }
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
    showOpenCvParamsPanel: Boolean,
    openCvHoughCirclesParams: OpenCvHoughCirclesParams,
    onExit: () -> Unit,
    onChooserImage: () -> Unit,
    onSave: () -> Unit,
    onConfirmClick: (Int, Int) -> Unit,
    onClearConfirmedCircles: () -> Unit,
    onShowOpenCvParamsPanel: () -> Unit,
    onHideOpenCvParamsPanel: () -> Unit,
    onChangeOpenCvHoughCirclesParams: (OpenCvHoughCirclesParams) -> Unit
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
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
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
            AnimatedVisibility(showOpenCvParamsPanel) {
                OpenCvParamsPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    params = openCvHoughCirclesParams,
                    onParamsChange = onChangeOpenCvHoughCirclesParams
                )
            }
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
                AnimatedVisibility(screenshotBitmap != null) {
                    if (showOpenCvParamsPanel) {
                        FilledTonalButton(onClick = onHideOpenCvParamsPanel) {
                            Text(text = stringResource(R.string.correction_screen_hide_opencv_params))
                        }
                    } else {
                        FilledTonalButton(onClick = onShowOpenCvParamsPanel) {
                            Text(text = stringResource(R.string.correction_screen_opencv_params))
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp)
                    .navigationBarsPadding(),
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
    var screenWidthPx by remember { mutableFloatStateOf(with(density) { screenWidthDp.dp.toPx() }) }
    var screenHeightPx by remember { mutableFloatStateOf(with(density) { screenHeightDp.dp.toPx() }) }
    val bitmapAspectRatio by remember { derivedStateOf { screenWidthPx / screenHeightPx } }

    LaunchedEffect(image) {
        if (image == null) return@LaunchedEffect
        screenWidthPx = image.width.toFloat()
        screenHeightPx = image.height.toFloat()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (image == null) screenAspectRatio else bitmapAspectRatio)
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
                    text = stringResource(R.string.correction_screen_image_placeholder),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OpenCvParamsPanel(
    modifier: Modifier = Modifier,
    params: OpenCvHoughCirclesParams,
    onParamsChange: (OpenCvHoughCirclesParams) -> Unit
) {
    var tab by remember { mutableStateOf(OpenCvHoughCirclesParam.Dp) }

    val dp by remember(params) { derivedStateOf { params.dp.toFloat() } }
    val minDist by remember(params) { derivedStateOf { params.minDist.toFloat() } }
    val param1 by remember(params) { derivedStateOf { params.param1.toFloat() } }
    val param2 by remember(params) { derivedStateOf { params.param2.toFloat() } }
    val minRadius by remember(params) { derivedStateOf { params.minRadius.toFloat() } }
    val maxRadius by remember(params) { derivedStateOf { params.maxRadius.toFloat() } }

    Column(
        modifier = modifier
    ) {
        Row {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = OpenCvHoughCirclesParam.entries) { param ->
                    val value =
                        when (param) {
                            OpenCvHoughCirclesParam.Dp -> dp
                            OpenCvHoughCirclesParam.MinDist -> minDist
                            OpenCvHoughCirclesParam.Param1 -> param1
                            OpenCvHoughCirclesParam.Param2 -> param2
                            OpenCvHoughCirclesParam.MinRadius -> minRadius
                            OpenCvHoughCirclesParam.MaxRadius -> maxRadius
                        }
                    if (tab == param) {
                        Button(
                            modifier = Modifier.height(32.dp),
                            onClick = {},
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {

                            Text(text = "${param.name} ${"%.1f".format(value)}")
                        }
                    } else {
                        TextButton(
                            modifier = Modifier.height(32.dp),
                            onClick = {
                                tab = param
                            }
                        ) {
                            Text(text = "${param.name} ${"%.1f".format(value)}")
                        }
                    }
                }
            }
        }

        when (tab) {
            OpenCvHoughCirclesParam.Dp -> {
                Slider(
                    modifier = Modifier,
                    value = dp,
                    onValueChange = { onParamsChange(params.copy(dp = it.toDouble())) },
                    valueRange = 1f..2f,
                    steps = 9,
                )
            }

            OpenCvHoughCirclesParam.MinDist -> {
                Slider(
                    modifier = Modifier,
                    value = minDist,
                    onValueChange = { onParamsChange(params.copy(minDist = it.toDouble())) },
                    valueRange = 20f..50f,
                    steps = 29,
                )
            }

            OpenCvHoughCirclesParam.Param1 -> {
                Slider(
                    modifier = Modifier,
                    value = param1,
                    onValueChange = { onParamsChange(params.copy(param1 = it.toDouble())) },
                    valueRange = 20f..100f,
                    steps = 79,
                )
            }

            OpenCvHoughCirclesParam.Param2 -> {
                Slider(
                    modifier = Modifier,
                    value = param2,
                    onValueChange = { onParamsChange(params.copy(param2 = it.toDouble())) },
                    valueRange = 10f..50f,
                    steps = 39,
                )
            }

            OpenCvHoughCirclesParam.MinRadius -> {
                Slider(
                    modifier = Modifier,
                    value = minRadius,
                    onValueChange = {
                        if (it < params.maxRadius) onParamsChange(params.copy(minRadius = it.toInt()))
                    },
                    valueRange = 10f..50f,
                    steps = 39,
                )
            }

            OpenCvHoughCirclesParam.MaxRadius -> {
                Slider(
                    modifier = Modifier,
                    value = maxRadius,
                    onValueChange = {
                        if (it > params.minRadius) onParamsChange(params.copy(maxRadius = it.toInt()))
                    },
                    valueRange = 10f..50f,
                    steps = 39,
                )
            }
        }
    }

}

private enum class OpenCvHoughCirclesParam {
    Dp, MinDist, Param1, Param2, MinRadius, MaxRadius
}

private data class OpenCvHoughCirclesParams(
    val dp: Double,
    val minDist: Double,
    val param1: Double,
    val param2: Double,
    val minRadius: Int,
    val maxRadius: Int
)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CorrectionScreenPreview(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

    var showOpenCvParamsPanel by remember { mutableStateOf(false) }
    var openCvHoughCirclesParams by remember {
        mutableStateOf(
            OpenCvHoughCirclesParams(
                dp = 1.2,
                minDist = 30.0,
                param1 = 50.0,
                param2 = 30.0,
                minRadius = 25,
                maxRadius = 30
            )
        )
    }

    GlyphRecorderTheme {
        CorrectionScreenContent(
            screenAspectRatio = screenAspectRatio,
            screenshotBitmap = null,
            screenshotCircles = emptyList(),
            confirmedCircles = emptyList(),
            showOpenCvParamsPanel = showOpenCvParamsPanel,
            openCvHoughCirclesParams = openCvHoughCirclesParams,
            onExit = {},
            onChooserImage = {},
            onSave = {},
            onConfirmClick = { _, _ -> },
            onClearConfirmedCircles = {},
            onShowOpenCvParamsPanel = { showOpenCvParamsPanel = true },
            onHideOpenCvParamsPanel = { showOpenCvParamsPanel = false },
            onChangeOpenCvHoughCirclesParams = { openCvHoughCirclesParams = it }
        )
    }
}

@Preview
@Composable
private fun OpenCvParamsPanelPreview() {
    var params by remember {
        mutableStateOf(
            OpenCvHoughCirclesParams(
                dp = 1.2,
                minDist = 30.0,
                param1 = 50.0,
                param2 = 30.0,
                minRadius = 25,
                maxRadius = 30
            )
        )
    }

    GlyphRecorderTheme {
        OpenCvParamsPanel(
            params = params,
            onParamsChange = { params = it }
        )
    }
}
