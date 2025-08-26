package dev.aaa1115910.glyphrecorder.ui.components

import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import dev.aaa1115910.glyphrecorder.ui.GlyphRecorderTheme
import dev.aaa1115910.glyphrecorder.util.Circle
import dev.aaa1115910.glyphrecorder.util.GlyphData


@Composable
fun HackEmulator(
    modifier: Modifier = Modifier,
    state: GlyphHackState,
    circlePoints: List<Pair<Float, Float>>,
    glyphSequences: List<String>,
    currentGlyphIndex: Int,
    onClickGlyphSequences: () -> Unit
) {
    val paths by remember(glyphSequences, currentGlyphIndex) {
        derivedStateOf {
            val glyph = glyphSequences
                .getOrNull(currentGlyphIndex / 2)
                ?.takeIf { currentGlyphIndex in 0..9 && currentGlyphIndex % 2 == 0 }
                ?: ""
            GlyphData.nameToPaths(glyph)
        }
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = state.text,
                color = Color.White,
                fontSize = 28.sp
            )
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
            )
            if (state != GlyphHackState.CommandOpening) {
                Row(
                    modifier = Modifier
                        .clickable(enabled = state == GlyphHackState.Idle) {
                            onClickGlyphSequences()
                        },
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val glyphIndex = if (currentGlyphIndex in 0..9) currentGlyphIndex / 2 else ""
                    glyphSequences.forEachIndexed { index, glyphName ->
                        GlyphHexagon(
                            modifier = Modifier.size(60.dp),
                            glyphName = glyphName,
                            color = if (glyphIndex == index) Color.Yellow else Color.White
                        )
                    }
                }
            }
        }

        EmulatorGlyphCanvas(
            modifier = modifier,
            paths = paths,
            circlePoints = circlePoints
        )
    }
}

@Composable
private fun EmulatorGlyphCanvas(
    modifier: Modifier = Modifier,
    paths: List<Pair<Circle, Circle>>,
    circlePoints: List<Pair<Float, Float>>
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        // 绘制圆圈
        circlePoints.forEach { (x, y) ->
            drawCircle(
                color = Color.White,
                radius = size.width / 40,
                center = Offset(x, y),
                style = Stroke(width = size.width / 90)
            )
        }

        // 绘制 Glyph 线条
        paths.forEach { (start, end) ->
            drawGlyphLine(
                start = start,
                end = end,
                color = Color.White,
                width = size.width / 30,
                circlePoints = circlePoints
            )
        }
    }
}

private fun DrawScope.drawGlyphLine(
    start: Circle,
    end: Circle,
    color: Color,
    width: Float,
    circlePoints: List<Pair<Float, Float>>
) {
    val getPointPosition: (Circle) -> Pair<Float, Float> = { circle ->
        circlePoints[circle.ordinal]
    }

    val (startX, startY) = getPointPosition(start)
    val (endX, endY) = getPointPosition(end)

    drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = width,
        cap = StrokeCap.Round
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GlyphHexagon(
    modifier: Modifier = Modifier,
    glyphName: String,
    color: Color
) {
    val density = LocalDensity.current
    val paths by remember(glyphName) { mutableStateOf(GlyphData.nameToPaths(glyphName)) }
    BoxWithConstraints(
        modifier = modifier
            .drawWithCache {
                onDrawBehind {
                    val path = Path()
                    RoundedPolygon(
                        numVertices = 6,
                        radius = size.width / 2,
                        centerX = size.width / 2,
                        centerY = size.height / 2,
                        rounding = CornerRounding(
                            radius = 20f,
                        )
                    ).toPath(path)
                    rotate(30f) {
                        drawPath(
                            path = path.asComposePath(),
                            color = color,
                            style = Stroke(width = size.width / 20, cap = StrokeCap.Round)
                        )
                    }
                }
            }
    ) {
        GlyphImage(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(with(density) { this@BoxWithConstraints.constraints.minWidth.toDp() } / 6),
            paths = paths,
            showBackground = false,
            contentColor = color,
        )
    }
}


@Preview
@Composable
private fun GlyphHexagonPreview() {
    val context = LocalContext.current
    GlyphData.initGlyphData(context)
    GlyphRecorderTheme {
        GlyphHexagon(
            modifier = Modifier.padding(10.dp),
            glyphName = "more",
            color = Color.Black
        )
    }
}

@Preview(device = "spec:width=1440px,height=3120px,dpi=560")
@Composable
private fun HackEmulatorPreview() {
    val context = LocalContext.current
    val state = GlyphHackState.Idle
    GlyphData.initGlyphData(context)
    GlyphRecorderTheme {
        Surface(color = Color.Black) {
            HackEmulator(
                modifier = Modifier,
                state = state,
                circlePoints = testPrefsCircles.map { (x, y) -> x.toFloat() to y.toFloat() },
                glyphSequences = listOf("more", "less", "complex", "enlightenment", "capture"),
                currentGlyphIndex = 4,
                onClickGlyphSequences = {}
            )
        }
    }
}


@Preview(
    device = "spec:width=1440px,height=3120px,dpi=560"
)
@Composable
private fun GlyphCanvasPreview() {
    GlyphRecorderTheme {
        Surface(color = Color.Black) {
            EmulatorGlyphCanvas(
                modifier = Modifier,
                paths = listOf(
                    Circle.C0 to Circle.C1,
                    Circle.C5 to Circle.C9
                ),
                circlePoints = testPrefsCircles.map { (x, y) -> x.toFloat() to y.toFloat() }
            )
        }
    }
}

val testPrefsCircles = listOf(
    720 to 1133,
    180 to 1457,
    1260 to 1457,
    450 to 1619,
    990 to 1619,
    717 to 1781,
    450 to 1943,
    990 to 1943,
    180 to 2105,
    1260 to 2105,
    720 to 2429
)

enum class GlyphHackState(val text: String) {
    Idle("空闲"),
    CommandOpening("命令频道正在打开..."),
    Receiving("正在接收符文序列...")
}