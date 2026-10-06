package codetrail.desktop.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.desktop.AppState
import codetrail.desktop.res.Res
import codetrail.desktop.res.back
import codetrail.desktop.res.stats_all_heroes
import codetrail.desktop.res.stats_by_difficulty
import codetrail.desktop.res.stats_by_world
import codetrail.desktop.res.stats_heroes
import codetrail.desktop.res.stats_levels
import codetrail.desktop.res.stats_next_hero
import codetrail.desktop.res.stats_stars
import codetrail.desktop.res.stats_title
import codetrail.desktop.theme.Character
import codetrail.desktop.theme.WorldTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatsScreen(app: AppState) {
    val profile = app.current ?: return
    val progress = profile.progress
    val unlocked = Character.All.count { progress.totalStars >= it.unlockStars }
    val next = Character.All.firstOrNull { progress.totalStars < it.unlockStars }
    val maxTier = (progress.wonPerTier.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val maxWorld = (progress.wonPerWorld.values.maxOrNull() ?: 0).coerceAtLeast(1)

    Box(Modifier.fillMaxSize().background(MenuBackground)) {
        Column(Modifier.fillMaxSize().padding(40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile.hero(), 64.dp, selected = true)
                Spacer(Modifier.width(16.dp))
                ScreenTitle(stringResource(Res.string.stats_title, profile.name))
                Spacer(Modifier.weight(1f))
                MenuButton(stringResource(Res.string.back), width = 160.dp) { app.goMenu() }
            }
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatTile(stringResource(Res.string.stats_stars), "${progress.totalStars} ★", Star)
                StatTile(stringResource(Res.string.stats_levels), "${progress.levelsWon}", Color.White)
                StatTile(stringResource(Res.string.stats_heroes), "$unlocked / ${Character.All.size}", Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (next == null) stringResource(Res.string.stats_all_heroes) else stringResource(Res.string.stats_next_hero, stringResource(next.name), next.unlockStars),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
            )

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1f).background(Panel, RoundedCornerShape(20.dp)).padding(20.dp)) {
                    Text(stringResource(Res.string.stats_by_difficulty), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    for (t in 1..5) {
                        BarRow("$t  ${stringResource(DifficultyNames[t - 1])}", progress.wonPerTier[t] ?: 0, maxTier, Accent)
                    }
                }
                Column(Modifier.weight(1f).background(Panel, RoundedCornerShape(20.dp)).padding(20.dp)) {
                    Text(stringResource(Res.string.stats_by_world), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    for (w in WorldTheme.All) {
                        BarRow(stringResource(w.name), progress.wonPerWorld[w.id] ?: 0, maxWorld, w.seaTop)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, valueColor: Color) {
    Column(Modifier.width(200.dp).background(Panel, RoundedCornerShape(18.dp)).padding(18.dp)) {
        Text(label, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
        Text(value, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

@Composable
private fun BarRow(label: String, value: Int, max: Int, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(170.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(value / max.toFloat()).height(16.dp).background(color, RoundedCornerShape(8.dp)))
        }
        Spacer(Modifier.width(12.dp))
        Text("$value", Modifier.width(36.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
