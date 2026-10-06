package codetrail.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import codetrail.desktop.AppState
import codetrail.desktop.ProvideAppLanguage
import codetrail.desktop.Screen

/** Routes between screens and applies language. */
@Composable
fun Root(app: AppState, onQuit: () -> Unit) {
    ProvideAppLanguage(app.language) {
        MaterialTheme {
            AnimatedContent(
                targetState = app.screen,
                transitionSpec = {
                    (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith fadeOut(tween(200))
                },
            ) { screen ->
                when (screen) {
                    Screen.MENU -> MenuScreen(app, onQuit)
                    Screen.PROFILES -> ProfilesScreen(app)
                    Screen.PLAY -> PlayScreen(app)
                    Screen.SETTINGS -> SettingsScreen(app)
                    Screen.GAME -> {
                        val game = app.game
                        if (game == null) MenuScreen(app, onQuit) else GameScreen(app, game)
                    }
                }
            }
        }
    }
}
