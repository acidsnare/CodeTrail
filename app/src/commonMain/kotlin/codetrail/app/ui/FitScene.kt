package codetrail.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** The layout is designed for at least this logical size. */
val SceneWidth = 1280.dp
val SceneHeight = 720.dp

/** Wider viewports stop stretching the scene here and get side margins instead (phones in landscape). */
val SceneMaxWidth = 1500.dp

/**
 * Scales density so the host's viewport is at least 1280x720 logical pixels, then lets the
 * scene fill the viewport. A taller screen gets a taller scene, a wider one a wider scene up
 * to [SceneMaxWidth]; past that the scene is centred so a 20:9 phone does not spread the
 * menu across the whole width.
 */
@Composable
fun FitScene(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF163B4B)), contentAlignment = Alignment.Center) {
        val base = LocalDensity.current
        val scale = min(maxWidth / SceneWidth, maxHeight / SceneHeight)
        val sceneWidth = minOf(maxWidth / scale, SceneMaxWidth)
        val sceneHeight = maxHeight / scale
        // Phones shrink the scene to ~0.57, which turns 13sp captions into 7 real sp. Text gets a
        // fifth back on touch screens; layouts are checked to absorb it.
        val fontBoost = if (codetrail.app.Platform.touch && scale < 1f) 1.2f else 1f
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale * fontBoost), LocalSceneScale provides scale) {
            Box(Modifier.size(sceneWidth, sceneHeight)) { content() }
        }
    }
}
