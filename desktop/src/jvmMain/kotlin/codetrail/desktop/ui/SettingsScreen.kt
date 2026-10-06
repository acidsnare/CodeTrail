package codetrail.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import codetrail.desktop.AppLanguage
import codetrail.desktop.AppState
import codetrail.desktop.res.Res
import codetrail.desktop.res.back
import codetrail.desktop.res.cancel
import codetrail.desktop.res.settings_reset
import codetrail.desktop.res.settings_reset_body
import codetrail.desktop.res.settings_reset_confirm
import codetrail.desktop.res.settings_data
import codetrail.desktop.res.settings_language
import codetrail.desktop.res.settings_title
import org.jetbrains.compose.resources.stringResource
import java.io.File

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
                Spacer(Modifier.height(32.dp))

                Text(stringResource(Res.string.settings_language), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (l in AppLanguage.entries) Chip(l.label, selected = app.language == l) { app.setLanguage(l) }
                }

                val profile = app.current
                if (profile != null) {
                    Spacer(Modifier.height(32.dp))
                    Text(stringResource(Res.string.settings_reset), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    MenuButton(stringResource(Res.string.settings_reset), color = Color(0xFFE53935), textColor = Color.White, width = 260.dp) { confirmReset = true }
                }

                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(Res.string.settings_data, File(System.getProperty("user.home"), ".codetrail").path),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
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
