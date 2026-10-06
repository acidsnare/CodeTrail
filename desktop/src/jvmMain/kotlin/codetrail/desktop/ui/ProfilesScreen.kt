package codetrail.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.core.progress.Profile
import codetrail.desktop.AppState
import codetrail.desktop.res.Res
import codetrail.desktop.res.back
import codetrail.desktop.res.hero_title
import codetrail.desktop.res.levels_won
import codetrail.desktop.res.menu_play
import codetrail.desktop.res.profiles_confirm_delete
import codetrail.desktop.res.profiles_create
import codetrail.desktop.res.profiles_current
import codetrail.desktop.res.profiles_delete
import codetrail.desktop.res.profiles_empty
import codetrail.desktop.res.profiles_name_hint
import codetrail.desktop.res.profiles_new
import codetrail.desktop.res.profiles_select
import codetrail.desktop.res.profiles_stats
import codetrail.desktop.res.profiles_title
import codetrail.desktop.theme.Character
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfilesScreen(app: AppState) {
    Box(Modifier.fillMaxSize().background(MenuBackground)) {
        Column(Modifier.fillMaxSize().padding(40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle(stringResource(Res.string.profiles_title))
                Spacer(Modifier.weight(1f))
                if (app.current != null) MenuButton(stringResource(Res.string.back), width = 160.dp) { app.goMenu() }
            }
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                // Left: profile list + create
                Column(Modifier.weight(1.1f)) {
                    NewProfileRow(app)
                    Spacer(Modifier.height(16.dp))
                    if (app.allProfiles.isEmpty()) {
                        Text(stringResource(Res.string.profiles_empty), color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(app.allProfiles, key = { it.id }) { p -> ProfileCard(app, p) }
                    }
                }
                // Right: hero picker for the current profile
                val current = app.current
                if (current != null) {
                    Column(Modifier.weight(0.9f).background(Panel, RoundedCornerShape(20.dp)).padding(24.dp)) {
                        Text(stringResource(Res.string.hero_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        HeroPicker(current) { app.chooseCharacter(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewProfileRow(app: AppState) {
    var name by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 16) name = it },
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.profiles_name_hint)) },
            label = { Text(stringResource(Res.string.profiles_new)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                cursorColor = Color.White,
            ),
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { app.createProfile(name); name = "" },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(14.dp),
        ) { Text(stringResource(Res.string.profiles_create), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ProfileCard(app: AppState, p: Profile) {
    val isCurrent = app.current?.id == p.id
    var confirmDelete by remember(p.id) { mutableStateOf(false) }
    val hero = p.hero()

    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isCurrent) Color.White.copy(alpha = 0.22f) else Panel, RoundedCornerShape(18.dp))
            .border(if (isCurrent) 2.dp else 0.dp, if (isCurrent) Color.White else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable { app.selectProfile(p) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(hero, 56.dp)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(p.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                stringResource(Res.string.profiles_stats, p.progress.totalStars, stringResource(Res.string.levels_won, p.progress.levelsWon)),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
            )
        }
        if (isCurrent) {
            Button(
                onClick = { app.goPlay() },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(end = 8.dp),
            ) { Text(stringResource(Res.string.menu_play) + "  ▶", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        } else {
            OutlinedButton(onClick = { app.selectProfile(p) }, modifier = Modifier.padding(end = 8.dp)) {
                Text(stringResource(Res.string.profiles_select), color = Color.White)
            }
        }
        if (confirmDelete) {
            Button(onClick = { app.deleteProfile(p) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) {
                Text(stringResource(Res.string.profiles_confirm_delete))
            }
        } else {
            OutlinedButton(onClick = { confirmDelete = true }) { Text(stringResource(Res.string.profiles_delete), color = Color.White.copy(alpha = 0.8f)) }
        }
        Spacer(Modifier.width(4.dp))
    }
}
