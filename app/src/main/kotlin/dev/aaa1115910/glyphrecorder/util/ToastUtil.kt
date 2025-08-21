package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import android.widget.Toast

fun String.toast(context: Context) {
    Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
}

fun Int.toast(context: Context) {
    context.resources.getString(this).toast(context)
}