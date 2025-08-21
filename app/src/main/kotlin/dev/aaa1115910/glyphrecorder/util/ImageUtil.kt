package dev.aaa1115910.glyphrecorder.util

import android.graphics.Bitmap
import android.media.Image
import androidx.core.graphics.createBitmap

fun Image.toBitmap(): Bitmap {
    val planes = planes
    val buffer = planes[0].buffer
    val pixelStride = planes[0].pixelStride
    val rowStride = planes[0].rowStride
    val width = width
    val height = height

    val bitmap = createBitmap(width + (rowStride - width * pixelStride) / pixelStride, height)

    bitmap.copyPixelsFromBuffer(buffer)
    return bitmap
}