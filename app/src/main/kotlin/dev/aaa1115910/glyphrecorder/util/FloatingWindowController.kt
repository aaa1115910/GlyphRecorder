package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.github.only52607.compose.window.ComposeFloatingWindow
import dev.aaa1115910.glyphrecorder.ui.createFloatingToolbox
import io.github.oshai.kotlinlogging.KotlinLogging

object FloatingWindowController {
    private val logger = KotlinLogging.logger { }
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var floatingWindow: ComposeFloatingWindow? = null

    @Volatile
    private var isVisible = false

    private fun ensureWindow(context: Context): ComposeFloatingWindow {
        if (floatingWindow == null) {
            floatingWindow = createFloatingToolbox(context.applicationContext)
            logger.info { "Floating window initialized" }
        }
        return floatingWindow!!
    }

    fun show(context: Context) {
        runOnMain {
            if (isVisible) return@runOnMain
            val window = ensureWindow(context.applicationContext)
            window.show()
            isVisible = true
        }
    }

    fun hide(context: Context) {
        runOnMain {
            if (!isVisible) return@runOnMain
            floatingWindow?.hide()
            isVisible = false
        }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}
