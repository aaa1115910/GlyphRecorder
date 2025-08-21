object AppConfiguration {
    const val appID = "dev.aaa1115910.glyphrecorder"
    const val minSdk = 30
    const val targerSdk = 36
    const val compileSdk = 36
    val versionCode: Int by lazy { "git rev-list --count HEAD".exec().toInt() }
    private val recursionAndLevel = calculateRecursionAndLevel(versionCode)
    val recursion get() = recursionAndLevel.first
    val level get() = recursionAndLevel.second
    val versionName = (if (recursion > 0) "Recursion $recursion " else "") + "Level $level"

    // https://github.com/chibatching/glyph-predictor-data
    const val glyphCsvUrl =
        "https://raw.githubusercontent.com/chibatching/glyph-predictor-data/master/glyph.csv"
    const val glyphCsvLineUrl =
        "https://raw.githubusercontent.com/chibatching/glyph-predictor-data/master/glyph_line.csv"
}

@Suppress("DEPRECATION")
private fun String.exec() = String(Runtime.getRuntime().exec(this).inputStream.readBytes()).trim()


private fun calculateRecursionAndLevel(totalLevel: Int): Pair<Int, Int> {
    val maxLevel = 16
    val recursion = (totalLevel - 1) / maxLevel
    val level = ((totalLevel - 1) % maxLevel) + 1
    return Pair(recursion, level)
}