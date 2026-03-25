package dev.aaa1115910.glyphrecorder.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.aaa1115910.glyphrecorder.ui.GlyphRecorderTheme
import dev.aaa1115910.glyphrecorder.ui.screens.HomeScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlyphRecorderTheme {
                HomeScreen()
            }
        }
    }
}
