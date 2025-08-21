package dev.aaa1115910.glyphrecorder.activities

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.AndroidViewModel
import dev.aaa1115910.glyphrecorder.ui.GlyphRecorderTheme
import dev.aaa1115910.glyphrecorder.ui.createFloatingToolbox
import dev.aaa1115910.glyphrecorder.ui.screens.HomeScreen
import kotlin.getValue


class MainActivity : ComponentActivity() {
    private val floatingWindowViewModel: FloatingWindowViewModel by viewModels()
    private val floatingWindow get() = floatingWindowViewModel.floatingWindow
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlyphRecorderTheme {
                HomeScreen(
                    onShowFloatingToolbox = { floatingWindow.show() },
                    onCloseFloatingToolbox = { floatingWindow.hide() }
                )
            }
        }
    }
}

class FloatingWindowViewModel(application: Application) : AndroidViewModel(application) {
    val floatingWindow = createFloatingToolbox(application.applicationContext)
}