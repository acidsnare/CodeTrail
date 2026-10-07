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

/** The layout is designed for this logical size. */
val SceneWidth = 1280.dp
val SceneHeight = 800.dp

/**
 * Letterboxes the fixed 1280x800 scene into whatever size the host gives us by scaling the
 * density, so the desktop layout works unchanged in a browser window or on a tablet.
 */
@Composable
fun FitScene(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF163B4B)), contentAlignment = Alignment.Center) {
        val base = LocalDensity.current
        val scale = min(maxWidth / SceneWidth, maxHeight / SceneHeight)
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
            Box(Modifier.size(SceneWidth, SceneHeight)) { content() }
        }
    }
}
