package codetrail.app

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import codetrail.app.sound.WebSoundPlayer
import codetrail.app.ui.FitScene
import codetrail.app.ui.Root

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
