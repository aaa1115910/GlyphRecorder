package dev.aaa1115910.glyphrecorder.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.glyphrecorder.ui.GlyphRecorderTheme
import dev.aaa1115910.glyphrecorder.ui.components.GlyphHackState
import dev.aaa1115910.glyphrecorder.ui.components.GlyphImage
import dev.aaa1115910.glyphrecorder.ui.components.HackEmulator
import dev.aaa1115910.glyphrecorder.ui.components.testPrefsCircles
import dev.aaa1115910.glyphrecorder.util.GlyphData
import dev.aaa1115910.glyphrecorder.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun HackEmulatorScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var hackMode by remember { mutableStateOf(HackMode.Normal) }
    var hackState by remember { mutableStateOf(GlyphHackState.Idle) }
    val circlePoints = remember { mutableStateListOf<Pair<Float, Float>>() }
    val glyphSequences =
    //remember { mutableStateListOf("more", "less", "complex", "enlightenment", "capture") }
        //remember { mutableStateListOf("balance","balance","safety","all") }
        remember { mutableStateListOf("complex", "shapers", "civilization", "strong") }
    var currentGlyphIndex by remember { mutableIntStateOf(-1) }
    var showGlyphSequencesEditor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        circlePoints.clear()
        circlePoints.addAll(Prefs.circles.map { (x, y) -> x.toFloat() to y.toFloat() })
    }

    val process: suspend () -> Unit = {
        // idle
        hackState = GlyphHackState.Idle
        currentGlyphIndex = -1

        // command opening
        hackState = GlyphHackState.CommandOpening
        delay(5000)

        // receive
        hackState = GlyphHackState.Receiving
        delay(1000)

        while (currentGlyphIndex < glyphSequences.size * 2) {
            // show glyph
            currentGlyphIndex++
            when (hackMode) {
                HackMode.Normal -> delay(870)
                HackMode.Complex -> delay(280)
            }
            // show null
            currentGlyphIndex++
            when (hackMode) {
                HackMode.Normal -> delay(300)
                HackMode.Complex -> delay(100)
            }
        }

        // end
        hackState = GlyphHackState.Idle
        //currentGlyphIndex = -1
    }

    val onStart = {
        scope.launch(Dispatchers.IO) { process() }
    }

    HackEmulatorScreenContent(
        modifier = modifier,
        hackState = hackState,
        hackMode = hackMode,
        circlePoints = circlePoints,
        glyphSequences = glyphSequences,
        currentGlyphIndex = currentGlyphIndex,
        showGlyphSequencesEditor = showGlyphSequencesEditor,
        onHackModeChange = { hackMode = it },
        onGlyphSequencesSizeChange = { newSize ->
            if (newSize > glyphSequences.size) {
                glyphSequences.addAll(Array(newSize - glyphSequences.size) { "" })
            } else {
                val newList = glyphSequences.dropLast(glyphSequences.size - newSize)
                glyphSequences.clear()
                glyphSequences.addAll(newList)
            }
        },
        onStart = { onStart() },
        onStop = {},
        onShowGlyphSequencesEditor={ showGlyphSequencesEditor = true },
        onHideGlyphSequencesEditor = { showGlyphSequencesEditor = false },
        onEditGlyphSequences = { index, newValue -> glyphSequences[index] = newValue }
    )
}

@Composable
private fun HackEmulatorScreenContent(
    modifier: Modifier = Modifier,
    hackState: GlyphHackState = GlyphHackState.Idle,
    hackMode: HackMode,
    circlePoints: List<Pair<Float, Float>>,
    glyphSequences: List<String>,
    currentGlyphIndex: Int,
    showGlyphSequencesEditor: Boolean,
    onHackModeChange: (HackMode) -> Unit,
    onGlyphSequencesSizeChange: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onShowGlyphSequencesEditor:()->Unit,
    onHideGlyphSequencesEditor:()->Unit,
    onEditGlyphSequences:(Int,String)->Unit
) {
    Surface {
        Box(
            modifier = modifier
        ) {
            HackEmulator(
                state = hackState,
                circlePoints = circlePoints,
                glyphSequences = glyphSequences,
                currentGlyphIndex = currentGlyphIndex,
                onClickGlyphSequences = onShowGlyphSequencesEditor
            )
            FloatingToolbar(
                expanded = hackState == GlyphHackState.Idle,
                hackMode = hackMode,
                glyphSequencesSize = glyphSequences.size,
                onHackModeChange = onHackModeChange,
                onGlyphSequencesSizeChange = onGlyphSequencesSizeChange,
                onStart = onStart,
                onStop = onStop
            )
        }
    }
    GlyphSequencesEditor(
        show = showGlyphSequencesEditor,
        glyphSequences = glyphSequences,
        onDismiss = onHideGlyphSequencesEditor,
        onGlyphChange = onEditGlyphSequences
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoxScope.FloatingToolbar(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    hackMode: HackMode,
    glyphSequencesSize: Int,
    onHackModeChange: (HackMode) -> Unit,
    onGlyphSequencesSizeChange: (Int) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    HorizontalFloatingToolbar(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .offset(y = -ScreenOffset)
            .windowInsetsPadding(WindowInsets.navigationBars),
        expanded = expanded,
        leadingContent = {
            TextButton(
                onClick = {
                    onHackModeChange(
                        when (hackMode) {
                            HackMode.Normal -> HackMode.Complex
                            HackMode.Complex -> HackMode.Normal
                        }
                    )
                },
            ) {
                Text(text = hackMode.name)
            }
        },
        trailingContent = {
            TextButton(
                onClick = {
                    onGlyphSequencesSizeChange(if (glyphSequencesSize >= 5) 2 else glyphSequencesSize + 1)
                },
            ) {
                Text(text = "x$glyphSequencesSize")
            }
        }
    ) {
        FilledIconButton(
            modifier = Modifier.width(64.dp),
            onClick = { if (expanded) onStart() else onStop() }
        ) {
            if (expanded) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start simulation"
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop simulation"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlyphSequencesEditor(
    modifier: Modifier = Modifier,
    show: Boolean,
    glyphSequences: List<String>,
    onDismiss: () -> Unit,
    onGlyphChange: (Int, String) -> Unit
) {
    var skipPartiallyExpanded by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    if (show) {
        ModalBottomSheet(
            modifier = modifier,
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            content = {
                GlyphSequencesEditorContent(
                    glyphSequences = glyphSequences,
                    onGlyphChange = onGlyphChange
                )
            }
        )
    }
}

@Composable
private fun ColumnScope.GlyphSequencesEditorContent(
    glyphSequences: List<String>,
    onGlyphChange: (index: Int, newValue: String) -> Unit
) {
    Box(
        modifier=Modifier.fillMaxSize()
    ){
        LazyColumn (
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ){
            itemsIndexed(glyphSequences) { index, glyph ->
                GlyphTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = glyph,
                    index = index,
                    onValueChange = { newValue -> onGlyphChange(index, newValue) },
                )
            }
        }
    }

}

@Composable
private fun GlyphTextField(
    modifier: Modifier = Modifier,
    value: String,
    index: Int,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = "Glyph ${index + 1}") },
        leadingIcon = {
            GlyphImage(
                modifier = Modifier.size(24.dp),
                name = value,
                showBackground = false,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        },
        shape= MaterialTheme.shapes.large
    )
}

private enum class HackMode {
    Normal,
    Complex
}

@Preview
@Composable
private fun ExpandedToolbar() {
    GlyphRecorderTheme {
        Box(
            modifier = Modifier.height(100.dp)
        ) {
            FloatingToolbar(
                expanded = true,
                hackMode = HackMode.Normal,
                glyphSequencesSize = 4,
                onHackModeChange = {},
                onGlyphSequencesSizeChange = {},
                onStart = {},
                onStop = {}
            )
        }
    }
}

@Preview
@Composable
private fun CollapsedToolbar() {
    GlyphRecorderTheme {
        Box(
            modifier = Modifier.height(100.dp)
        ) {
            FloatingToolbar(
                expanded = false,
                hackMode = HackMode.Normal,
                glyphSequencesSize = 3,
                onHackModeChange = {},
                onGlyphSequencesSizeChange = {},
                onStart = {},
                onStop = {}
            )
        }
    }
}

@Preview(
    device = "spec:width=1440px,height=3120px,dpi=560",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun HackEmulatorScreenPreview() {
    val context = LocalContext.current
    GlyphData.initGlyphData(context)
    var hackMode by remember { mutableStateOf(HackMode.Normal) }
    var hackState by remember { mutableStateOf(GlyphHackState.Receiving) }
    val currentGlyphIndex by remember { mutableIntStateOf(2) }

    GlyphRecorderTheme {
        HackEmulatorScreenContent(
            hackState = hackState,
            hackMode = hackMode,
            circlePoints = testPrefsCircles.map { (x, y) -> x.toFloat() to y.toFloat() },
            glyphSequences = listOf("more", "less", "complex", "enlightenment", "capture"),
            currentGlyphIndex = currentGlyphIndex,
            showGlyphSequencesEditor = false,
            onHackModeChange = { hackMode = it },
            onGlyphSequencesSizeChange = {},
            onStart = {},
            onStop = {},
            onShowGlyphSequencesEditor={},
            onHideGlyphSequencesEditor = {},
            onEditGlyphSequences = { index, newValue -> }
        )
    }
}

@Preview
@Composable
private fun GlyphSequencesEditorPreview() {
    GlyphRecorderTheme {
        Column {
            GlyphSequencesEditorContent(
                glyphSequences = listOf("more", "less", "complex", "enlightenment", "capture"),
                onGlyphChange = { index, newValue -> }
            )
        }
    }
}

@Preview
@Composable
private fun GlyphTextFieldPreview() {
    val context = LocalContext.current
    GlyphData.initGlyphData(context)
    GlyphRecorderTheme {
        GlyphTextField(
            value = "more",
            index = 2,
            onValueChange = {}
        )
    }
}