package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import android.content.Intent
import dev.aaa1115910.glyphrecorder.activities.HackEmulatorActivity


fun openHackEmulator(context: Context) {
    val intent = Intent(context, HackEmulatorActivity::class.java)
    context.startActivity(intent)
}