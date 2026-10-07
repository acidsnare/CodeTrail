package codetrail.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import codetrail.app.sound.AndroidSoundPlayer
import codetrail.app.ui.FitScene
import codetrail.app.ui.Root

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A game wants the whole screen: hide the bars, a swipe from the edge brings them back briefly.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            val storage = remember { AndroidStorage(filesDir) }
            val app = remember { AppState(storage, storage, AndroidSoundPlayer()) }
            FitScene {
                Root(app, onQuit = { app.exitToMenu(); finish() })
            }
        }
    }
}
