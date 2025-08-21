package dev.aaa1115910.glyphrecorder

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import dev.aaa1115910.glyphrecorder.util.OpenCvUtil
import dev.aaa1115910.glyphrecorder.util.Prefs
import org.slf4j.impl.HandroidLoggerAdapter

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        initOpenCv()
    }

    private fun initOpenCv() {
        System.loadLibrary("opencv_java4")
        OpenCvUtil.updatePrefCircles(Prefs.circles)
    }
}