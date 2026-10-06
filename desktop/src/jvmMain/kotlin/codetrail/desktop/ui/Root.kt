package codetrail.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import codetrail.desktop.res.Res
import codetrail.desktop.res.cancel
import codetrail.desktop.res.quit_body
import codetrail.desktop.res.quit_title
import codetrail.desktop.res.quit_yes
import org.jetbrains.compose.resources.stringResource
import codetrail.desktop.AppState
import codetrail.desktop.ProvideAppLanguage
import codetrail.desktop.Screen

/** Routes between screens and applies language. */
@Composable
fun Root(app: AppState, onQuit: () -> Unit) {
    ProvideAppLanguage(app.language) {
        CompositionLocalProvider(LocalSounds provides app.sounds) {
        MaterialTheme(typography = appTypography()) {
            Box {
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
                    Screen.STATS -> StatsScreen(app)
                    Screen.GAME -> {
                        val game = app.game
                        if (game == null) MenuScreen(app, onQuit) else GameScreen(app, game)
                    }
                }
            }
            if (app.quitRequested) {
                ConfirmDialog(
                    title = stringResource(Res.string.quit_title),
                    body = stringResource(Res.string.quit_body),
                    confirmText = stringResource(Res.string.quit_yes),
                    cancelText = stringResource(Res.string.cancel),
                    onConfirm = onQuit,
                    onCancel = { app.cancelQuit() },
                )
            }
            }
        }
        }
    }
}
