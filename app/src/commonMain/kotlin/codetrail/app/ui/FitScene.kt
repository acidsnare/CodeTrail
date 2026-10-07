package codetrail.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** The layout is designed for at least this logical size. */
val SceneWidth = 1280.dp
val SceneHeight = 800.dp

/**
 * Scales density so the host's viewport is at least 1280x800 logical pixels, then lets the
 * scene fill the whole viewport. A wider screen gets a wider scene (panels spread out), a
 * taller one a taller scene; nothing is letterboxed and nothing falls off the edge.
 */
@Composable
fun FitScene(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF163B4B))) {
        val base = LocalDensity.current
        val scale = min(maxWidth / SceneWidth, maxHeight / SceneHeight)
        val sceneWidth = maxWidth / scale
        val sceneHeight = maxHeight / scale
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
            Box(Modifier.size(sceneWidth, sceneHeight)) { content() }
        }
    }
}
