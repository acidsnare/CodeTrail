package codetrail.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import codetrail.desktop.ui.Root
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() = application {
    val storage = remember { FileStorage() }
    val app = remember { AppState(storage, storage) }
    val icon: Painter = remember { BitmapPainter(useResource("icon/codetrail_512.png") { loadImageBitmap(it) }) }

    // The dock / taskbar icon is not taken from the window icon when running from Gradle, set it explicitly.
    LaunchedEffect(Unit) {
        runCatching {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                Taskbar.getTaskbar().iconImage = useResource("icon/codetrail_512.png") { ImageIO.read(it) }
            }
        }
    }

    Window(
        onCloseRequest = { app.exitToMenu(); exitApplication() },
        title = "CodeTrail",
        icon = icon,
        state = WindowState(size = DpSize(1280.dp, 800.dp)),
    ) {
        Root(app, onQuit = { app.exitToMenu(); exitApplication() })
    }
}
