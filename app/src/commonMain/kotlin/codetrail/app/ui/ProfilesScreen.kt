package codetrail.app.ui

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
import codetrail.app.AppState
import codetrail.app.res.Res
import codetrail.app.res.back
import codetrail.app.res.hero_title
import codetrail.app.res.levels_won
import codetrail.app.res.menu_play
import codetrail.app.res.profiles_confirm_delete
import codetrail.app.res.profiles_create
import codetrail.app.res.profiles_current
import codetrail.app.res.profiles_delete
import codetrail.app.res.profiles_empty
import codetrail.app.res.profiles_name_hint
import codetrail.app.res.profiles_new
import codetrail.app.res.profiles_select
import codetrail.app.res.profiles_stats
import codetrail.app.res.profiles_title
import codetrail.app.res.profiles_export
import codetrail.app.res.profiles_import
import codetrail.app.res.export_title
import codetrail.app.res.export_body
import codetrail.app.res.copy
import codetrail.app.res.copied
import codetrail.app.res.save_file
import codetrail.app.res.open_file
import codetrail.app.res.import_title
import codetrail.app.res.import_hint
import codetrail.app.res.import_invalid
import codetrail.app.res.import_exists
import codetrail.app.res.import_replace
import codetrail.app.res.import_copy
import codetrail.app.res.close
import codetrail.app.res.cancel
import codetrail.app.Platform
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import codetrail.app.theme.Character
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfilesScreen(app: AppState) {
    var exporting by remember { mutableStateOf<Profile?>(null) }
    var importing by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MenuBackground)) {
        Column(Modifier.fillMaxSize().padding(40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScreenTitle(stringResource(Res.string.profiles_title))
                Spacer(Modifier.weight(1f))
                MenuButton(stringResource(Res.string.profiles_import), width = 160.dp) { importing = true }
                if (app.current != null) MenuButton(stringResource(Res.string.back), width = 160.dp) { app.goMenu() }
            }
            Spacer(Modifier.height(24.dp))

            if (app.allProfiles.isEmpty()) {
                // First launch: one big form in the middle instead of a thin row at the top of an empty screen.
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(stringResource(Res.string.profiles_empty), color = Color.White.copy(alpha = 0.85f), fontSize = 20.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    NewProfileRow(app, Modifier.width(560.dp), big = true)
                    Spacer(Modifier.weight(0.6f))
                }
            } else
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                // Left: profile list + create
                Column(Modifier.weight(1.1f)) {
                    NewProfileRow(app)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(app.allProfiles, key = { it.id }) { p -> ProfileCard(app, p, onExport = { exporting = p }) }
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
        exporting?.let { p -> ExportDialog(app, p) { exporting = null } }
        if (importing) ImportDialog(app) { importing = false }
    }
}

/** Shows the transfer code of [p] with copy and, where the host has files, save. */
@Composable
private fun ExportDialog(app: AppState, p: Profile, onClose: () -> Unit) {
    val code = remember(p) { app.exportProfile(p) }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    DialogFrame(stringResource(Res.string.export_title) + " · " + p.name, onClose) {
        Text(stringResource(Res.string.export_body), fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        SelectionContainer {
            Text(
                code,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton(stringResource(if (copied) Res.string.copied else Res.string.copy), color = Accent, textColor = Color.White, width = 180.dp) {
                if (Platform.ownsClipboard) Platform.copyToClipboard(code)
                else @Suppress("DEPRECATION") clipboard.setText(AnnotatedString(code))
                copied = true
            }
            if (Platform.canUseFiles) {
                MenuButton(stringResource(Res.string.save_file), width = 200.dp) { Platform.saveTextFile("${p.name}.properties", app.exportProfileText(p)) }
            }
            MenuButton(stringResource(Res.string.close), width = 140.dp, onClick = onClose)
        }
    }
}

/** Paste (or open) a profile code; asks what to do when a profile with that name is already here. */
@Composable
private fun ImportDialog(app: AppState, onClose: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf<AppState.Import.Conflict?>(null) }
    fun apply(result: AppState.Import) {
        when (result) {
            AppState.Import.Invalid -> invalid = true
            is AppState.Import.Conflict -> conflict = result
            is AppState.Import.Done -> onClose()
        }
    }
    DialogFrame(stringResource(Res.string.import_title), onClose) {
        val c = conflict
        if (c != null) {
            Text(stringResource(Res.string.import_exists, c.existing.name), fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuButton(stringResource(Res.string.cancel), width = 150.dp) { conflict = null }
                MenuButton(stringResource(Res.string.import_copy), width = 190.dp) { app.importAsCopy(c); onClose() }
                MenuButton(stringResource(Res.string.import_replace), color = Color(0xFFE53935), textColor = Color.White, width = 180.dp) { app.importReplace(c); onClose() }
            }
        } else {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; invalid = false },
                placeholder = { Text(stringResource(Res.string.import_hint)) },
                minLines = 4,
                maxLines = 6,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                isError = invalid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
                    cursorColor = Color.White,
                    errorTextColor = Color.White,
                    errorCursorColor = Color.White,
                    errorBorderColor = Color(0xFFFF8A80),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (invalid) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.import_invalid), color = Color(0xFFFF8A80), fontSize = 14.sp)
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuButton(stringResource(Res.string.cancel), width = 150.dp, onClick = onClose)
                if (Platform.canUseFiles) {
                    MenuButton(stringResource(Res.string.open_file), width = 190.dp) { Platform.openTextFile { apply(app.importProfile(it)) } }
                }
                MenuButton(stringResource(Res.string.profiles_import), color = Accent, textColor = Color.White, enabled = input.isNotBlank(), width = 170.dp) { apply(app.importProfile(input)) }
            }
        }
    }
}

@Composable
private fun DialogFrame(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(640.dp)
                .background(MenuBackground, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun NewProfileRow(app: AppState, modifier: Modifier = Modifier.fillMaxWidth(), big: Boolean = false) {
    var name by remember { mutableStateOf("") }
    val textSize = if (big) 24.sp else 18.sp
    // The Create button lives inside the field so the outline spans the same width as the cards below.
    OutlinedTextField(
        value = name,
        onValueChange = { if (it.length <= 16) name = it },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = textSize, fontWeight = FontWeight.SemiBold),
        placeholder = { Text(stringResource(Res.string.profiles_name_hint), fontSize = textSize) },
        label = { Text(stringResource(Res.string.profiles_new)) },
        trailingIcon = {
            Button(
                onClick = { app.createProfile(name); name = "" },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = Color.White.copy(alpha = 0.15f), disabledContentColor = Color.White.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(end = 8.dp).heightIn(min = if (big) 52.dp else TapTarget),
            ) { Text(stringResource(Res.string.profiles_create), fontSize = if (big) 20.sp else 16.sp, fontWeight = FontWeight.Bold) }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
            focusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.35f),
            cursorColor = Color.White,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ProfileCard(app: AppState, p: Profile, onExport: () -> Unit) {
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
            OutlinedButton(onClick = { app.selectProfile(p) }, modifier = Modifier.padding(end = 8.dp).heightIn(min = TapTarget)) {
                Text(stringResource(Res.string.profiles_select), color = Color.White)
            }
        }
        // Icon only: a fourth text button pushed the stats line onto two rows in Russian.
        OutlinedButton(onClick = onExport, contentPadding = PaddingValues(0.dp), modifier = Modifier.padding(end = 8.dp).size(TapTarget + 4.dp)) {
            Icon(Icons.Default.Share, stringResource(Res.string.profiles_export), Modifier.size(20.dp), tint = Color.White.copy(alpha = 0.8f))
        }
        if (confirmDelete) {
            Button(onClick = { app.deleteProfile(p) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)), modifier = Modifier.heightIn(min = TapTarget)) {
                Text(stringResource(Res.string.profiles_confirm_delete))
            }
        } else {
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.heightIn(min = TapTarget)) { Text(stringResource(Res.string.profiles_delete), color = Color.White.copy(alpha = 0.8f)) }
        }
        Spacer(Modifier.width(4.dp))
    }
}
