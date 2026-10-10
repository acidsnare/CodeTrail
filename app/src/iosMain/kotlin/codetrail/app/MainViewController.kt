package codetrail.app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import codetrail.app.sound.IosSoundPlayer
import codetrail.app.ui.FitScene
import codetrail.app.ui.Root
import platform.UIKit.UIViewController

/** Entry point for the Swift shell in iosApp/. */
@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController = ComposeUIViewController {
    val storage = remember { IosStorage() }
    val app = remember { AppState(storage, storage, IosSoundPlayer()) }
    FitScene {
        Root(app, onQuit = { app.exitToMenu() })
    }
}
