package dev.aaa1115910.glyphrecorder.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opencv.android.Utils
import org.opencv.android.Utils.bitmapToMat
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot

object OpenCvUtil {
    private val logger = KotlinLogging.logger { }
    private val prefCircles = mutableListOf<Pair<Int, Int>>()

    /**
     * 更新预设点的位置
     * @param circles 包含圆心坐标的列表，每个圆心以 (x, y) 的形式表示
     */
    fun updatePrefCircles(circles: List<Pair<Int, Int>>) {
        prefCircles.clear()
        prefCircles.addAll(circles)
    }

    /**
     * 识别圆心坐标
     * @return 返回包含圆心坐标的列表，每个圆心以 (x, y) 的形式表示
     */
    fun detectCircles(
        bitmap: Bitmap,
        dp: Double = 1.2,
        minDist: Double = 30.0,
        param1: Double = 50.0,
        param2: Double = 30.0,
        minRadius: Int = 25,
        maxRadius: Int = 30
    ): List<Pair<Int, Int>> {
        val image = bitmap.toMat()
        val result = detectCircles(image, dp, minDist, param1, param2, minRadius, maxRadius)
        image.release()
        return result
    }

    /**
     * 检测图像中的路径
     * @param bitmap 输入的图像位图
     * @return 返回检测到的路径列表，每个路径由两个圆的枚举值组成
     */
    fun detectLines(bitmap: Bitmap): List<Pair<Circle, Circle>> {
        val image = bitmap.toMat()
        val result = detectLines(image)
        image.release()
        return result
    }

    /**
     * 检测过滤后的路径
     * @param bitmap 输入的图像位图
     * @return 返回过滤后的路径列表，每个路径由两个圆的枚举值组成
     */
    fun detectFilteredLines(
        bitmap: Bitmap,
    ): List<Pair<Circle, Circle>> {
        val image = bitmap.toMat()
        val result = detectFilteredLines(image)
        image.release()
        return result
    }

    /**
     * 检测字形
     * @param bitmap 输入的图像位图
     * @return 返回检测到的字形名称列表，如果没有检测到字形则返回空列表
     */
    fun detectGlyphs(bitmap: Bitmap): List<String> {
        val image = bitmap.toMat()
        val filteredPaths = detectFilteredLines(image)
        image.release()
        return matchGlyph(filteredPaths)
    }

    /**
     * 检测图像中的六边形
     * @param bitmap 输入的图像位图
     * @return 返回检测到的六边形数量
     */
    fun detectHexagons(bitmap: Bitmap): Int {
        val image = bitmap.toMat()
        val result = detectHexagonsWithColor(image).size
        image.release()
        return result
    }

    /**
     * 检测图像中的六边形
     * @param bitmap 输入的图像位图
     * @return 返回检测到的六边形列表，每项为(中心点, Color)
     */
    fun detectHexagonsWithColor(bitmap: Bitmap): List<Pair<Point, Color>> {
        val image = bitmap.toMat()
        val result = detectHexagonsWithColor(image)
        println("Detected hexagons with color: $result")
        image.release()
        return result
    }

    /**
     * 使用HoughCircles检测图像中的圆
     * @param mat 输入的图像矩阵
     * @param dp 累加器分辨率与图像分辨率的反比
     * @param minDist 检测到的圆心之间的最小距离，
     * @param param1 边缘检测时使用的高阈值
     * @param param2 中心检测时使用的累加器阈值
     * @param minRadius 圆的最小半径
     * @param maxRadius 圆的最大半径
     * @return 返回检测到的圆的中心坐标列表
     * 每个圆的坐标以 (x, y) 的形式表示
     * 如果没有检测到圆，则返回空列表
     */
    fun detectCircles(
        mat: Mat,
        dp: Double = 1.2,
        minDist: Double = 30.0,
        param1: Double = 50.0,
        param2: Double = 30.0,
        minRadius: Int = 25,
        maxRadius: Int = 30
    ): List<Pair<Int, Int>> {
        val grayMat = cvtColor(mat, Imgproc.COLOR_BGR2GRAY)
        val blurredMat = gaussianBlur(grayMat, Size(9.0, 9.0), 2.0)
        grayMat.release()
        val circles =
            houghCircles(
                blurredMat,
                Imgproc.HOUGH_GRADIENT,
                dp,
                minDist,
                param1,
                param2,
                minRadius,
                maxRadius
            )
        blurredMat.release()

        if (circles.cols() > 0) {
            val circlePositions = mutableListOf<Pair<Int, Int>>()
            for (i in 0 until circles.cols()) {
                val data = circles.get(0, i)
                val x = data[0].toInt()
                val y = data[1].toInt()
                val r = data[2].toInt()
                circlePositions.add(Pair(x, y))
                logger.debug { "Detect circle: ($x, $y), radius: $r" }
            }

            val sortedPositions = circlePositions.sortedWith(compareBy({ it.second }, { it.first }))
            return sortedPositions
        } else {
            logger.debug { "No circles were detected" }
        }
        circles.release()
        return emptyList()
    }

    /**
     * 检测图像中的六边形
     * @param mat 输入的图像矩阵
     * @return 返回检测到的六边形数量
     * 如果没有检测到六边形，则返回0
     */
    fun detectHexagons(mat: Mat): Int {
        val hexWithColor = detectHexagonsWithColor(mat)
        return hexWithColor.size
    }

    /**
     * 检测图像中的六边形并识别每个六边形的颜色（基于 HSV 平均值）
     * @param mat 输入的图像矩阵（BGR）
     * @return 返回检测到的六边形列表，每项为(中心点, Color)
     */
    fun detectHexagonsWithColor(mat: Mat): List<Pair<Point, Color>> {
        val hsvMat = cvtColor(mat, Imgproc.COLOR_BGR2HSV)
        val grayMat = cvtColor(mat, Imgproc.COLOR_BGR2GRAY)
        val binary = threshold(grayMat, 128.0, 255.0, Imgproc.THRESH_BINARY)
        grayMat.release()

        val (contours, hierarchy) = findContours(
            binary,
            Imgproc.RETR_TREE,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        hierarchy.release()
        binary.release()

        val detected = mutableListOf<Pair<Point, Color>>()

        for (contour in contours) {
            val contour2f = contour.toMatOfPoint2f()
            val approx = approxPolyDP(contour2f, 0.02 * arcLength(contour2f, true), true)
            contour2f.release()

            val pts = approx.toList()
            if (pts.size == 6 && isRegularHexagon(approx)) {
                // 用多边形创建单通道掩膜，在 HSV 图上计算平均色
                val poly = approx.toMatOfPoint()
                val mask = Mat.zeros(mat.size(), CvType.CV_8UC1)
                Imgproc.fillConvexPoly(mask, poly, Scalar(255.0))

                val meanHSV = Core.mean(hsvMat, mask)
                // Scalar 的字段名是 `val`，用反引号访问
                val h = meanHSV.`val`[0].toInt()
                val s = meanHSV.`val`[1].toInt()
                val v = meanHSV.`val`[2].toInt()

                // 计算中心点
                val arr = poly.toArray()
                val meanX = arr.map { it.x }.average()
                val meanY = arr.map { it.y }.average()
                val center = Point(meanX, meanY)

                val color = hueToColor(h, s, v)
                detected.add(Pair(center, color))

                mask.release()
                poly.release()
            }
            approx.release()
            contour.release()
        }

        hsvMat.release()

        val uniqueDetected = filterOverlappingHexagonsWithColor(detected)
        return uniqueDetected
    }

    /**
     * 判断一个多边形是否为正六边形
     * @param approx 近似的多边形点集
     * @param tol 边长和内角的容差，默认为0.15
     * @return 如果是正六边形则返回true，否则返回false
     */
    fun isRegularHexagon(approx: MatOfPoint2f, tol: Double = 0.15): Boolean {
        val points = approx.toList()
        // 计算六边形每条边的长度
        val edges = DoubleArray(6) { i ->
            val p1 = points[i]
            val p2 = points[(i + 1) % 6]
            hypot(p1.x - p2.x, p1.y - p2.y)
        }
        val meanEdge = edges.average()
        // 判断边长是否接近
        if (edges.any { abs(it - meanEdge) > tol * meanEdge }) return false

        // 计算每个内角
        fun angle(pt1: Point, pt2: Point, pt3: Point): Double {
            val v1x = pt1.x - pt2.x
            val v1y = pt1.y - pt2.y
            val v2x = pt3.x - pt2.x
            val v2y = pt3.y - pt2.y
            val dot = v1x * v2x + v1y * v2y
            val norm1 = hypot(v1x, v1y)
            val norm2 = hypot(v2x, v2y)
            val cosAngle = dot / (norm1 * norm2)
            return Math.toDegrees(acos(cosAngle.coerceIn(-1.0, 1.0)))
        }

        val angles = DoubleArray(6) { i ->
            angle(points[(i + 5) % 6], points[i], points[(i + 1) % 6])
        }
        // 判断内角是否接近120度
        if (angles.any { abs(it - 120.0) > 15.0 }) return false
        return true
    }

    /**
     * 将HSV颜色转换为Color对象
     * @param h 色调，范围0-179
     * @param s 饱和度，范围0-255
     * @param v 明度，范围0-255
     * @return 返回对应基础的Color对象，例如Color.Yellow, Color.Cyan等
     */
    fun hueToColor(h: Int, s: Int, v: Int): Color {
        val hsv = floatArrayOf(h.toFloat() * 2, s.toFloat() / 255, v.toFloat() / 255)
        val colorInt = android.graphics.Color.HSVToColor(hsv)
        val color = Color(colorInt)
        return color
        /*return when {
            v < 50 -> Color.Black
            s < 40 && v > 200 -> Color.White
            s < 40 -> Color.Gray
            h in 10..24 -> Color.Magenta
            h in 25..34 -> Color.Yellow
            h in 35..84 -> Color.Green
            h in 85..99 -> Color.Cyan
            h in 100..155 -> Color.Blue
            h in 156..255 || h in 0..10 -> Color.Red
            else -> Color.White
        }.also {
            val rgbRaw = android.graphics.Color.HSVToColor(floatArrayOf(h.toFloat() * 2, s.toFloat() / 255, v.toFloat() / 255))
            val rgbStr= String.format("#%06X", 0xFFFFFF and rgbRaw)
            logger.debug { "$rgbStr HSV($h, $s, $v) mapped to Color $it"}
        }*/
    }

    /**
     * 过滤重叠的六边形
     * @param hexagons 输入的六边形列表，每个六边形以 MatOfPoint 的形式表示
     * @param minDist 最小距离，默认为20.0
     * @return 返回过滤后的六边形列表
     */
    fun filterOverlappingHexagons(
        hexagons: List<MatOfPoint>,
        minDist: Double = 20.0
    ): List<MatOfPoint> {
        // 计算每个六边形的中心点
        val centers = hexagons.map { hex ->
            val pts = hex.toArray()
            val meanX = pts.map { it.x }.average()
            val meanY = pts.map { it.y }.average()
            Point(meanX, meanY)
        }
        val kept = mutableListOf<MatOfPoint>()
        val used = BooleanArray(hexagons.size) { false }

        for (i in centers.indices) {
            if (used[i]) continue
            val c1 = centers[i]
            val group = mutableListOf(i)
            used[i] = true
            for (j in centers.indices) {
                if (i != j && !used[j]) {
                    val c2 = centers[j]
                    if (hypot(c1.x - c2.x, c1.y - c2.y) < minDist) {
                        used[j] = true
                        group.add(j)
                    }
                }
            }
            // 只保留每组的第一个六边形
            kept.add(hexagons[group[0]])
        }
        return kept
    }

    /**
     * 过滤重叠的六边形
     * @param hexagons 输入的六边形列表，每个六边形以 MatOfPoint 的形式表示
     * @param minDist 最小距离，默认为20.0
     * @return 返回过滤后的六边形列表
     */
    fun filterOverlappingHexagonsWithColor(
        hexagons: List<Pair<Point, Color>>,
        minDist: Double = 20.0
    ): List<Pair<Point, Color>> {
        val kept = mutableListOf<Pair<Point, Color>>()
        val used = BooleanArray(hexagons.size) { false }

        for (i in hexagons.indices) {
            if (used[i]) continue
            val (center1, color1) = hexagons[i]
            val group = mutableListOf(Pair(center1, color1))
            used[i] = true
            for (j in hexagons.indices) {
                if (i != j && !used[j]) {
                    val (center2, color2) = hexagons[j]
                    if (hypot(center1.x - center2.x, center1.y - center2.y) < minDist) {
                        used[j] = true
                        group.add(Pair(center2, color2))
                    }
                }
            }
            // 只保留每组的第一个六边形
            kept.add(group[0])
        }
        return kept
    }

    /**
     * 圆心位置映射
     * @return 返回一个包含圆心位置的映射，每个圆的枚举值对应其坐标 (x, y)
     */
    private val circlePositions: Map<Circle, Pair<Int, Int>>
        get() = prefCircles.mapIndexed { index, pair ->
            val id = if (index == 10) "a" else index.toString()
            Circle.fromId(id) to Pair(pair.first, pair.second)
        }.toMap()

    /**
     * 检测激活的路径
     * @param mat 输入的图像矩阵
     * @return 返回激活的路径列表，每个路径由两个圆的枚举值组成
     */
    fun detectLines(mat: Mat): MutableList<Pair<Circle, Circle>> {
        val hsvMat = cvtColor(mat, Imgproc.COLOR_BGR2HSV)

        // 排除掉不需要的连接
        val excludedConnections = listOf(
            Circle.C1 to Circle.C5,
            Circle.C1 to Circle.C7,
            Circle.C1 to Circle.C9,
            Circle.C3 to Circle.C7,
            Circle.C3 to Circle.C9,
            Circle.C5 to Circle.C9,

            Circle.C2 to Circle.C5,
            Circle.C2 to Circle.C6,
            Circle.C2 to Circle.C8,
            Circle.C4 to Circle.C6,
            Circle.C4 to Circle.C8,
            Circle.C5 to Circle.C8,

            Circle.C0 to Circle.Ca
        )

        val possibleConnections = mutableListOf<Pair<Circle, Circle>>()
        circlePositions.forEach { (i, _) ->
            circlePositions.forEach { (j, _) ->
                if (excludedConnections.contains(i to j) || excludedConnections.contains(j to i)) return@forEach
                if (i != j) possibleConnections.add(Pair(i, j))
            }
        }

        val activatedPaths = mutableListOf<Pair<Circle, Circle>>()
        possibleConnections.forEach { (start, end) ->
            val startPos = circlePositions[start] ?: return@forEach
            val endPos = circlePositions[end] ?: return@forEach
            if (isPathActivated(hsvMat, startPos, endPos, 200, 0.7, 200)) {
                activatedPaths.add(Pair(start, end))
                logger.debug { "Activated path: $start to $end" }
            }
        }
        hsvMat.release()
        return activatedPaths
    }

    /**
     * 拆分重叠的路径
     * @param paths 输入的路径列表，每个路径由两个圆的枚举值组成
     * @return 返回拆分后的路径列表
     */
    fun splitOverlappingPaths(
        paths: List<Pair<Circle, Circle>>
    ): List<Pair<Circle, Circle>> {
        val splitMap = mapOf(
            //0-a
            Circle.C0 to Circle.Ca to listOf(
                Circle.C0 to Circle.C5,
                Circle.C5 to Circle.Ca,
            ),
            //1-9
            Circle.C1 to Circle.C9 to listOf(
                Circle.C1 to Circle.C3,
                Circle.C3 to Circle.C5,
                Circle.C5 to Circle.C7,
                Circle.C7 to Circle.C9
            ),
            Circle.C1 to Circle.C7 to listOf(
                Circle.C1 to Circle.C3,
                Circle.C3 to Circle.C5,
                Circle.C5 to Circle.C7
            ),
            Circle.C1 to Circle.C5 to listOf(
                Circle.C1 to Circle.C3,
                Circle.C3 to Circle.C5
            ),
            Circle.C3 to Circle.C9 to listOf(
                Circle.C3 to Circle.C5,
                Circle.C5 to Circle.C7,
                Circle.C7 to Circle.C9
            ),
            Circle.C3 to Circle.C7 to listOf(
                Circle.C3 to Circle.C5,
                Circle.C5 to Circle.C7
            ),
            Circle.C5 to Circle.C9 to listOf(
                Circle.C5 to Circle.C7,
                Circle.C7 to Circle.C9
            ),
            //2-8
            Circle.C2 to Circle.C8 to listOf(
                Circle.C2 to Circle.C4,
                Circle.C4 to Circle.C5,
                Circle.C5 to Circle.C6,
                Circle.C6 to Circle.C8
            ),
            Circle.C2 to Circle.C6 to listOf(
                Circle.C2 to Circle.C4,
                Circle.C4 to Circle.C5,
                Circle.C5 to Circle.C6
            ),
            Circle.C2 to Circle.C5 to listOf(
                Circle.C2 to Circle.C4,
                Circle.C4 to Circle.C5
            ),
            Circle.C4 to Circle.C8 to listOf(
                Circle.C4 to Circle.C5,
                Circle.C5 to Circle.C6,
                Circle.C6 to Circle.C8
            ),
            Circle.C4 to Circle.C6 to listOf(
                Circle.C4 to Circle.C5,
                Circle.C5 to Circle.C6
            ),
            Circle.C5 to Circle.C8 to listOf(
                Circle.C5 to Circle.C6,
                Circle.C6 to Circle.C8
            )
        )
        val result = mutableListOf<Pair<Circle, Circle>>()
        for (path in paths) {
            val split = splitMap[path]
            if (split != null) {
                logger.debug { "Splitting path: $path into $split" }
                result.addAll(split)
            } else {
                result.add(path)
            }
        }
        return result
    }

    /**
     * 检测过滤后的路径
     * @param mat 输入的图像矩阵
     * @return 返回过滤后的路径列表，每个路径由两个圆的枚举值组成
     */
    fun detectFilteredLines(mat: Mat): List<Pair<Circle, Circle>> {
        val paths = detectLines(mat)
        // 过滤重复路径
        val uniquePaths = mutableSetOf<Pair<Circle, Circle>>()
        paths.forEach { (start, end) ->
            if (!uniquePaths.contains(Pair(end, start))) {
                uniquePaths.add(Pair(start, end))
                logger.debug { "Unique path: $start to $end" }
            }
        }
        // 拆分重叠的路径
        val splitedPaths = splitOverlappingPaths(uniquePaths.toList()).toSet()
        splitedPaths.forEach { (start, end) ->
            logger.debug { "Unique final path: $start to $end" }
        }
        return splitedPaths.toList()
    }

    /**
     * 获取两点之间的像素点
     * @param startPos 起始点坐标 (x, y)
     * @param endPos 结束点坐标 (x, y)
     * @param numSamples 采样点数量，默认为100
     * @return 返回一个包含像素点坐标的列表
     */
    fun getLinePixels(
        startPos: Pair<Int, Int>,
        endPos: Pair<Int, Int>,
        numSamples: Int = 100
    ): MutableList<Pair<Int, Int>> {
        val (x0, y0) = startPos
        val (x1, y1) = endPos
        val pixels = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until numSamples) {
            val t = i.toDouble() / (numSamples - 1)
            val x = (x0 + (x1 - x0) * t).toInt()
            val y = (y0 + (y1 - y0) * t).toInt()
            pixels.add(Pair(x, y))
        }
        return pixels
    }

    /**
     * 检查像素是否被激活
     * @param pixel 像素值数组，单通道或三通道
     * @param threshold 激活阈值，默认为200
     * @return 如果像素值大于阈值，则返回true，否则返回false
     */
    fun isPixelActivated(pixel: DoubleArray, threshold: Int = 200): Boolean {
        if (pixel.size == 1) {
            return pixel[0] > threshold
        } else {
            val (_, _, v) = pixel
            return v > threshold
        }
    }

    /**
     * 计算激活像素的数量
     * @param mat 输入的图像矩阵
     * @param linePixels 包含像素点坐标的列表
     * @param threshold 激活阈值，默认为200
     * @return 返回激活像素的数量
     */
    fun countActivatedPixels(
        mat: Mat,
        linePixels: List<Pair<Int, Int>>,
        threshold: Int = 200
    ): Int {
        var activatedCount = 0
        for ((x, y) in linePixels) {
            if (isPixelActivated(mat.get(y, x), threshold)) {
                activatedCount++
            }
        }
        return activatedCount
    }

    /**
     * 检查从起始点到结束点的路径是否被激活
     * @param mat 输入的图像矩阵
     * @param startPos 起始点坐标 (x, y)
     * @param endPos 结束点坐标 (x, y)
     * @param threshold 激活阈值，默认为200
     * @param activationRatio 激活比例，默认为0.5
     * @param numSamples 采样点数量，默认为100
     * @return 如果激活像素占比超过激活比例，则返回true，否则返回false
     */
    fun isPathActivated(
        mat: Mat,
        startPos: Pair<Int, Int>,
        endPos: Pair<Int, Int>,
        threshold: Int = 200,
        activationRatio: Double = 0.5,
        numSamples: Int = 100
    ): Boolean {
        val linePixels = getLinePixels(startPos, endPos, numSamples)
        val activatedCount = countActivatedPixels(mat, linePixels, threshold)
        return activatedCount.toFloat() / linePixels.size > activationRatio
    }

    /**
     * 匹配过滤后的路径与字形
     * @param filteredPaths 过滤后的路径列表，每个路径由两个圆的枚举值组成
     * @return 返回匹配的字形名称列表，如果没有匹配的字形则返回空列表
     */
    fun matchGlyph(filteredPaths: List<Pair<Circle, Circle>>): List<String> {
        val matchedGlyphs = mutableListOf<String>()
        val filteredPathsSet = filteredPaths.map {
            if (it.first.id < it.second.id) it else Pair(
                it.second,
                it.first
            )
        }.toSet()
        for ((glyphName, glyphPath) in GlyphData.glyphLines) {
            if (glyphPath.length < 2) continue
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
            if (filteredPathsSet.size != glyphPathSet.size) continue
            val matchedPaths = filteredPathsSet.intersect(glyphPathSet)
            val matchRatio = matchedPaths.size.toDouble() / glyphPathSet.size
            if (matchRatio >= 0.95) {
                matchedGlyphs.add(glyphName)
            }
        }
        return matchedGlyphs
    }
}

enum class Circle(val id: String) {
    C0("0"), C1("1"), C2("2"), C3("3"), C4("4"), C5("5"), C6("6"), C7("7"), C8("8"), C9("9"), Ca("a");

    companion object {
        fun fromId(id: String): Circle {
            return entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Invalid Circle ID: $id")
        }
    }
}

private fun Bitmap.toMat(): Mat {
    val mat = Mat()
    bitmapToMat(this, mat)
    return mat
}

private fun cvtColor(src: Mat, code: Int): Mat {
    val dst = Mat()
    Imgproc.cvtColor(src, dst, code)
    return dst
}

private fun gaussianBlur(src: Mat, ksize: Size, sigmaX: Double): Mat {
    val dst = Mat()
    Imgproc.GaussianBlur(src, dst, ksize, sigmaX)
    return dst
}

private fun houghCircles(
    src: Mat,
    method: Int,
    dp: Double,
    minDist: Double,
    param1: Double,
    param2: Double,
    minRadius: Int,
    maxRadius: Int
): Mat {
    val circles = Mat()
    Imgproc.HoughCircles(
        src,
        circles,
        method,
        dp,
        minDist,
        param1,
        param2,
        minRadius,
        maxRadius
    )
    return circles
}

private fun threshold(
    src: Mat,
    thresh: Double,
    maxval: Double,
    type: Int
): Mat {
    val dst = Mat()
    Imgproc.threshold(src, dst, thresh, maxval, type)
    return dst
}

private fun findContours(
    src: Mat,
    mode: Int,
    method: Int
): Pair<MutableList<MatOfPoint>, Mat> {
    val contours = mutableListOf<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(src, contours, hierarchy, mode, method)
    return Pair(contours, hierarchy)
}

private fun approxPolyDP(
    contour: MatOfPoint2f,
    epsilon: Double,
    closed: Boolean
): MatOfPoint2f {
    val approx = MatOfPoint2f()
    Imgproc.approxPolyDP(contour, approx, epsilon, closed)
    return approx
}

private fun arcLength(
    contour: MatOfPoint2f,
    closed: Boolean
): Double {
    return Imgproc.arcLength(contour, closed)
}

private fun MatOfPoint.toMatOfPoint2f(): MatOfPoint2f {
    val dst = MatOfPoint2f()
    convertTo(dst, CvType.CV_32F)
    return dst
}

private fun MatOfPoint2f.toMatOfPoint(): MatOfPoint {
    val dst = MatOfPoint()
    convertTo(dst, CvType.CV_32S)
    return dst
}

private fun drawContours(
    mat: Mat,
    contours: List<MatOfPoint>,
    contourIdx: Int,
    color: Scalar,
    thickness: Int = 1
) {
    Imgproc.drawContours(mat, contours, contourIdx, color, thickness)
}

private fun Mat.toBitmap(): Bitmap {
    val bitmap = createBitmap(this.cols(), this.rows())
    Utils.matToBitmap(this, bitmap)
    return bitmap
}