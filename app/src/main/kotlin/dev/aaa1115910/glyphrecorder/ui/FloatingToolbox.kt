package dev.aaa1115910.glyphrecorder.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SystemAlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.github.only52607.compose.window.ComposeFloatingWindow
import com.github.only52607.compose.window.LocalFloatingWindow
import dev.aaa1115910.glyphrecorder.BuildConfig
import dev.aaa1115910.glyphrecorder.R
import dev.aaa1115910.glyphrecorder.ui.components.GlyphImage
import dev.aaa1115910.glyphrecorder.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun createFloatingToolbox(applicationContext: Context): ComposeFloatingWindow =
    ComposeFloatingWindow(applicationContext).apply {

        // 设置初始位置
        val resetPosition = {
            windowParams.x = Prefs.floatingWindowPositionX
            windowParams.y = Prefs.floatingWindowPositionY
        }
        resetPosition()

        setContent {
            GlyphRecorderTheme(
                containerColor = Color.Transparent
            ) {
                FloatingToolbox(
                    resetPosition = resetPosition
                )
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingToolbox(
    resetPosition: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val floatingWindow = LocalFloatingWindow.current
    var expanded by remember { mutableStateOf(false) }
    var autoCapturing by remember { mutableStateOf(false) }
    var showCloseTip by remember { mutableStateOf(false) }

    val state = rememberFloatingToolboxState(
        onStopAutoCapture = { autoCapturing = false }
    )

    FloatingToolboxContent(
        expanded = expanded,
        autoCapture = autoCapturing,
        showCloseTip = showCloseTip,
        capturedGlyphs = state.capturedGlyphs,
        matchedGlyphs = state.matchedGlyphs,
        onExpandedChange = {
            haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            expanded = it
        },
        onCapture = {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            scope.launch(Dispatchers.Default) {
                state.takeScreenshot()
            }
        },
        onStartAutoCapture = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            autoCapturing = true
            state.startAutoCapture()
        },
        onStopAutoCapture = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
            autoCapturing = false
            state.stopAutoCapture()
        },
        onClear = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            state.capturedGlyphs.clear()
        },
        onClose = {
            floatingWindow.hide()
            resetPosition()
            Prefs.working = false
        },
        onCloseAreaStateChange = { showCloseTip = it }
    )

    Guild()
}

@Composable
private fun FloatingToolboxContent(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    autoCapture: Boolean,
    showCloseTip: Boolean,
    capturedGlyphs: List<String>,
    matchedGlyphs: List<String>,
    onExpandedChange: (Boolean) -> Unit,
    onCapture: () -> Unit,
    onStartAutoCapture: () -> Unit,
    onStopAutoCapture: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onCloseAreaStateChange: (Boolean) -> Unit
) {
    val view = LocalView.current

    if (showCloseTip) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(y = -100, x = 0)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    modifier = Modifier.padding(8.dp, 4.dp),
                    text = stringResource(R.string.floating_window_close_tip),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = if (view.isInEditMode) Modifier else Modifier.dragFloatingWindow(
                onClose = onClose,
                onCloseAreaStateChange = onCloseAreaStateChange
            ),
            horizontalArrangement = Arrangement.spacedBy((-4).dp)
        ) {
            FilledTonalIconButton(
                onClick = {
                    onExpandedChange(!expanded)
                }) {
                Icon(
                    imageVector = Icons.Rounded.Build,
                    contentDescription = "Toolbox"
                )
            }
            if (expanded) {
                if (!autoCapture) {
                    FilledTonalIconButton(onClick = {
                        onStartAutoCapture()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Start Auto Capture"
                        )
                    }
                }
                if (autoCapture) {
                    FilledTonalIconButton(onClick = {
                        onStopAutoCapture()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Stop Auto Capture"
                        )
                    }
                }
                //if (!autoCapture) {
                if (false) {
                    FilledTonalIconButton(onClick = {
                        onCapture()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Screenshot,
                            contentDescription = "Take Screenshot"
                        )
                    }
                }
                if (capturedGlyphs.isNotEmpty()) {
                    FilledTonalIconButton(onClick = {
                        onClear()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ClearAll,
                            contentDescription = "Clear All"
                        )
                    }
                }
            }
        }
        if (expanded) {
            GlyphRow(
                glyphs = matchedGlyphs.ifEmpty { capturedGlyphs }
            )
        }
    }
}

@Composable
private fun GlyphRow(
    modifier: Modifier = Modifier,
    glyphs: List<String>
) {
    LazyRow(
        modifier = modifier.heightIn(max = 50.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(items = glyphs) { glyph ->
            GlyphImage(
                name = glyph
            )
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
private fun Modifier.dragFloatingWindow(
    onClose: () -> Unit,
    onCloseAreaStateChange: (inArea: Boolean) -> Unit
): Modifier = composed {
    val floatingWindow = LocalFloatingWindow.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val windowParams = remember { floatingWindow.windowParams }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    var positionY by remember { mutableIntStateOf(0) }
    val shouldClose by remember { derivedStateOf { (positionY / screenHeight) > 0.7f } }
    var initialized by remember { mutableStateOf(false) }

    val saveFloatingWindowPosition = {
        if (!shouldClose) {
            println("Saving floating window position: x=${floatingWindow.windowParams.x}, y=${floatingWindow.windowParams.y}")
            if (floatingWindow.windowParams.x != 0) Prefs.floatingWindowPositionX =
                floatingWindow.windowParams.x
            if (floatingWindow.windowParams.y != 0) Prefs.floatingWindowPositionY =
                floatingWindow.windowParams.y
        }
    }

    LaunchedEffect(shouldClose) {
        onCloseAreaStateChange(shouldClose)
        if (initialized) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        initialized = true
    }

    pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = { if (shouldClose) onClose() else saveFloatingWindowPosition() }
        ) { change, dragAmount ->
            change.consume()
            val w = floatingWindow.decorView.width
            val h = floatingWindow.decorView.height
            val f = Rect().also { floatingWindow.decorView.getWindowVisibleDisplayFrame(it) }
            windowParams.x =
                (windowParams.x + dragAmount.x.toInt()).coerceIn(0..(f.width() - w))
            windowParams.y =
                (windowParams.y + dragAmount.y.toInt()).coerceIn(0..(f.height() - h))
            floatingWindow.update()

            // 更新悬浮窗位置
            positionY = windowParams.y + h / 2
        }
    }
}

@Composable
private fun Guild(modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!Prefs.floatingWindowFirstTipShown) {
            showDialog = true
            Prefs.floatingWindowFirstTipShown = true
        }
    }

    GuildDialog(
        modifier = modifier,
        showDialog = showDialog,
        onDismissRequest = { showDialog = false }
    )
}

@Composable
private fun GuildDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    onDismissRequest: () -> Unit = {}
) {
    if (showDialog) {
        SystemAlertDialog(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            confirmButton = {
                FilledTonalButton(onClick = onDismissRequest) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = stringResource(R.string.floating_window_guide_dialog_text))
                    Image(
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        painter = painterResource(R.drawable.floating_window_position_example),
                        contentDescription = "Floating Window Position Example"
                    )
                }
            },
            title = {
                Text(text = "使用提示")
            }
        )
    }
}

@Preview
@Composable
private fun FloatingToolboxPreview() {
    var expanded by remember { mutableStateOf(true) }
    val glyphs = remember { mutableStateListOf("", "", "", "", "") }

    Column {
        FloatingToolboxContent(
            modifier = Modifier,
            expanded = expanded,
            autoCapture = false,
            showCloseTip = false,
            capturedGlyphs = glyphs,
            matchedGlyphs = glyphs,
            onExpandedChange = { expanded = it },
            onCapture = {},
            onStartAutoCapture = {},
            onStopAutoCapture = {},
            onClear = {},
            onClose = {},
            onCloseAreaStateChange = {}
        )
    }
}

@Preview
@Composable
private fun GuildDialogPreview() {
    GuildDialog(
        showDialog = true,
        onDismissRequest = {}
    )
}
