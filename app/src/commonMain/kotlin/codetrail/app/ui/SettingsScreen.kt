package codetrail.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.app.AppLanguage
import codetrail.app.AppState
import codetrail.app.res.Res
import codetrail.app.res.back
import codetrail.app.res.cancel
import codetrail.app.res.settings_reset
import codetrail.app.res.settings_reset_body
import codetrail.app.res.settings_reset_confirm
import codetrail.app.res.settings_data
import codetrail.app.res.settings_language
import codetrail.app.res.settings_sound
import codetrail.app.res.settings_speed
import codetrail.app.res.speed_slow
import codetrail.app.res.speed_normal
import codetrail.app.res.speed_fast
import codetrail.app.AnimSpeed
import codetrail.app.res.on
import codetrail.app.res.off
import codetrail.app.res.settings_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(app: AppState) {
    var confirmReset by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MenuBackground)) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(Modifier.fillMaxSize().padding(40.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle(stringResource(Res.string.settings_title))
                    Spacer(Modifier.weight(1f))
                    MenuButton(stringResource(Res.string.back), width = 160.dp) { app.goMenu() }
                }
                Spacer(Modifier.height(28.dp))

                // Two panels side by side: the game settings and the data section, so the right half is not empty.
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight().background(Panel, RoundedCornerShape(20.dp)).padding(24.dp)) {
                        Text(stringResource(Res.string.settings_language), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (l in AppLanguage.entries) Chip(l.label, selected = app.language == l) { app.setLanguage(l) }
                        }

                        Spacer(Modifier.height(28.dp))
                        Text(stringResource(Res.string.settings_sound), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Chip(stringResource(Res.string.on), selected = app.settings.sound) { app.setSound(true) }
                            Chip(stringResource(Res.string.off), selected = !app.settings.sound) { app.setSound(false) }
                        }

                        Spacer(Modifier.height(28.dp))
                        Text(stringResource(Res.string.settings_speed), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (sp in AnimSpeed.entries) {
                                val label = when (sp) {
                                    AnimSpeed.SLOW -> Res.string.speed_slow
                                    AnimSpeed.NORMAL -> Res.string.speed_normal
                                    AnimSpeed.FAST -> Res.string.speed_fast
                                }
                                Chip(stringResource(label), selected = app.animSpeed == sp) { app.setSpeed(sp) }
                            }
                        }
                    }

                    Column(Modifier.weight(1f).fillMaxHeight().background(Panel, RoundedCornerShape(20.dp)).padding(24.dp)) {
                        val profile = app.current
                        if (profile != null) {
                            Text(stringResource(Res.string.settings_reset), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(Res.string.settings_reset_body, profile.name),
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.height(14.dp))
                            MenuButton(stringResource(Res.string.settings_reset), color = Color(0xFFE53935), textColor = Color.White, width = 260.dp) { confirmReset = true }
                            Spacer(Modifier.height(28.dp))
                        }
                        Text(
                            stringResource(Res.string.settings_data, codetrail.app.Platform.saveLocation),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
        val profile = app.current
        if (confirmReset && profile != null) {
            ConfirmDialog(
                title = stringResource(Res.string.settings_reset),
                body = stringResource(Res.string.settings_reset_body, profile.name),
                confirmText = stringResource(Res.string.settings_reset_confirm),
                cancelText = stringResource(Res.string.cancel),
                danger = true,
                onConfirm = { app.resetProgress(); confirmReset = false },
                onCancel = { confirmReset = false },
            )
        }
    }
}
