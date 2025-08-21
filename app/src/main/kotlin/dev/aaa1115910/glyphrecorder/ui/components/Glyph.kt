package dev.aaa1115910.glyphrecorder.ui.components

import android.graphics.CornerPathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.aaa1115910.glyphrecorder.util.Circle
import dev.aaa1115910.glyphrecorder.util.GlyphData
import dev.aaa1115910.glyphrecorder.util.OpenCvUtil.splitOverlappingPaths
import kotlin.math.sqrt


@Composable
fun GlyphImage(
    modifier: Modifier = Modifier,
    name: String = "",
    contentColor: Color = Color.White,
    containerColor: Color = Color.DarkGray,
    showBackground: Boolean = true
) {
    val paths = remember { mutableStateListOf<Pair<Circle, Circle>>() }

    LaunchedEffect(name) {
        val glyphPath = GlyphData.glyphLines[name]!!
        val glyphPathTuples = glyphPath.windowed(2)
        val glyphPathList = glyphPathTuples.map {
            var start = it[0]
            var end = it[1]
            if (start > end) {
                start = end.also { end = start }
            }
            Pair(Circle.fromId(start.toString()), Circle.fromId(end.toString()))
        }
        val glyphPathSet = splitOverlappingPaths(glyphPathList).toSet()
        paths.clear()
        paths.addAll(glyphPathSet)
    }

    GlyphImage(
        modifier = modifier,
        paths = paths,
        contentColor = contentColor,
        containerColor = containerColor,
        showBackground = showBackground
    )
}

@Composable
fun GlyphImage(
    modifier: Modifier = Modifier,
    paths: List<Pair<Circle, Circle>>,
    contentColor: Color = Color.White,
    containerColor: Color = Color.DarkGray,
    showBackground: Boolean = true
) {
    val contentScale = if (showBackground) 0.8f else 1f

    Canvas(
        modifier = modifier
            .aspectRatio(sqrt(3f) / 2)
    ) {
        val path = Path().apply {
            moveTo(this@Canvas.size.width / 2, 0f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height / 4)
            lineTo(this@Canvas.size.width, (3 * this@Canvas.size.height) / 4)
            lineTo(this@Canvas.size.width / 2, this@Canvas.size.height)
            lineTo(0f, (3 * this@Canvas.size.height) / 4)
            lineTo(0f, this@Canvas.size.height / 4)
            close()
        }

        if (showBackground) {
            drawIntoCanvas {
                it.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = containerColor.toArgb()
                        pathEffect = CornerPathEffect(this@Canvas.size.width / 15)
                    }
                    drawPath(path.asAndroidPath(), paint)
                }
            }
        }

        withTransform({
            scale(contentScale, contentScale)
        }) {
            paths.forEach { (start, end) ->
                drawGlyphLine(
                    start = start,
                    end = end,
                    color = contentColor
                )
            }
        }
    }
}

fun DrawScope.drawGlyphLine(
    start: Circle,
    end: Circle,
    color: Color
) {
    val getPointPosition: (Circle) -> Pair<Float, Float> = { circle ->
        when (circle) {
            Circle.C0 -> size.width / 2 to 0f
            Circle.C1 -> 0f to size.height / 4
            Circle.C2 -> size.width to size.height / 4
            Circle.C3 -> size.width / 4 to (3 * size.height) / 8
            Circle.C4 -> (3 * size.width) / 4 to (3 * size.height) / 8
            Circle.C5 -> size.width / 2 to size.height / 2
            Circle.C6 -> size.width / 4 to (5 * size.height) / 8
            Circle.C7 -> (3 * size.width) / 4 to (5 * size.height) / 8
            Circle.C8 -> 0f to (3 * size.height) / 4
            Circle.C9 -> size.width to (3 * size.height) / 4
            Circle.Ca -> size.width / 2 to size.height
        }
    }

    val (startX, startY) = getPointPosition(start)
    val (endX, endY) = getPointPosition(end)

    drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = size.width / 14,
        cap = StrokeCap.Round
    )
}

@Preview
@Composable
private fun GlyphPreview() {
    MaterialTheme {
        GlyphImage(
            modifier = Modifier
                .width(60.dp),
            paths = listOf(
                Circle.C0 to Circle.C1,
                Circle.C5 to Circle.C9
            )
        )
    }
}


@Preview
@Composable
private fun GlyphsPreview() {
    val glyphs = listOf(
        "star", "weak", "save", "see"
    )
    MaterialTheme {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            glyphs.forEach {
                GlyphImage(
                    modifier = Modifier
                        .width(40.dp),
                    name = it
                )
            }
        }
    }
}
