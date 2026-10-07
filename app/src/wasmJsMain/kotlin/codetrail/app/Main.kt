package codetrail.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import codetrail.app.sound.WebSoundPlayer
import codetrail.app.ui.Root
import kotlin.math.min

/** The layout is designed for this logical size; the browser scales it to fit the window. */
private val SceneWidth = 1280.dp
private val SceneHeight = 800.dp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("root") {
        val storage = remember { BrowserStorage() }
        val app = remember { AppState(storage, storage, WebSoundPlayer()) }
        FitScene {
            Root(app, onQuit = { app.exitToMenu() })
        }
    }
}

/**
 * Letterboxes a fixed 1280x800 scene into whatever size the browser gives us by scaling the
 * density, so the desktop layout works unchanged on any window and on tablets.
 */
@androidx.compose.runtime.Composable
private fun FitScene(content: @androidx.compose.runtime.Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF163B4B)), contentAlignment = Alignment.Center) {
        val base = LocalDensity.current
        val scale = min(maxWidth / SceneWidth, maxHeight / SceneHeight)
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
            Box(Modifier.size(SceneWidth, SceneHeight)) { content() }
        }
    }
}
