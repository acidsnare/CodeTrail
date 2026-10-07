package codetrail.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.app.AppState
import codetrail.core.gen.Difficulty
import codetrail.app.GameMode
import codetrail.app.res.mode_forward
import codetrail.app.res.mode_forward_desc
import codetrail.app.res.mode_predict
import codetrail.app.res.mode_predict_desc
import codetrail.app.res.play_mode
import codetrail.app.res.Res
import codetrail.app.res.back
import codetrail.app.res.diff_1
import codetrail.app.res.diff_2
import codetrail.app.res.diff_3
import codetrail.app.res.diff_4
import codetrail.app.res.diff_5
import codetrail.app.res.diff_6
import codetrail.app.res.play_difficulty
import codetrail.app.res.play_solved
import codetrail.app.res.play_start
import codetrail.app.res.play_title
import codetrail.app.res.play_world
import codetrail.app.res.tier_multiplier
import codetrail.app.theme.WorldTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val DifficultyNames: List<StringResource> = listOf(Res.string.diff_1, Res.string.diff_2, Res.string.diff_3, Res.string.diff_4, Res.string.diff_5, Res.string.diff_6)

@Composable
fun PlayScreen(app: AppState) {
    val profile = app.current ?: return
    var world by remember { mutableStateOf(WorldTheme.byId(profile.worldId) ?: WorldTheme.Islands) }
    var tier by remember { mutableStateOf(profile.tier) }
    var mode by remember { mutableStateOf(GameMode.FORWARD) }

    Box(Modifier.fillMaxSize().background(MenuBackground)) {
        Column(Modifier.fillMaxSize().padding(40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle(stringResource(Res.string.play_title))
                Spacer(Modifier.weight(1f))
                MenuButton(stringResource(Res.string.back), width = 160.dp) { app.goMenu() }
            }
            Spacer(Modifier.height(28.dp))

            Text(stringResource(Res.string.play_world), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (w in WorldTheme.All) WorldCard(w, selected = world.id == w.id) { world = w }
            }

            Spacer(Modifier.height(28.dp))
            Text(stringResource(Res.string.play_difficulty), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (t in 1..Difficulty.TIERS.size) {
                    TierCard(t, stringResource(DifficultyNames[t - 1]), profile.progress.wonPerTier[t] ?: 0, selected = tier == t) { tier = t }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(stringResource(Res.string.play_mode), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard("🧩", stringResource(Res.string.mode_forward), stringResource(Res.string.mode_forward_desc), selected = mode == GameMode.FORWARD) { mode = GameMode.FORWARD }
                ModeCard("🔍", stringResource(Res.string.mode_predict), stringResource(Res.string.mode_predict_desc), selected = mode == GameMode.PREDICT) { mode = GameMode.PREDICT }
            }

            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                MenuButton(stringResource(Res.string.play_start), color = Accent, textColor = Color.White, width = 240.dp) { app.startGame(world, tier, mode) }
            }
        }
    }
}

@Composable
private fun WorldCard(w: WorldTheme, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(186.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(if (selected) 3.dp else 0.dp, if (selected) Color.White else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        // Miniature of the world: sea gradient with a few land tiles.
        Canvas(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp))) {
            drawRect(Brush.verticalGradient(listOf(w.seaTop, w.seaBottom)))
            val cell = size.width / 7
            val land = listOf(1 to 1, 2 to 1, 3 to 1, 3 to 2, 4 to 2, 5 to 2)
            for ((x, y) in land) {
                drawRoundRect(w.halo, Offset(x * cell - cell * 0.09f, y * cell - cell * 0.09f + 6f), Size(cell * 1.18f, cell * 1.18f), CornerRadius(cell * 0.2f))
            }
            for ((x, y) in land) {
                drawRoundRect(w.land, Offset(x * cell, y * cell + 6f), Size(cell, cell), CornerRadius(cell * 0.15f))
            }
            with(w.art) { drawGoal(Offset(5.5f * cell, 2.5f * cell + 6f), cell) }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(w.name), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun TierCard(tier: Int, name: String, solved: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Panel)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$tier", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) Ink else Color.White)
            Spacer(Modifier.weight(1f))
            Text(stringResource(Res.string.tier_multiplier, tier), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Star)
        }
        Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (selected) Ink else Color.White)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(Res.string.play_solved, solved), fontSize = 13.sp, color = (if (selected) Ink else Color.White).copy(alpha = 0.75f))
    }
}

@Composable
private fun ModeCard(icon: String, title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .width(380.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Panel)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 30.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (selected) Ink else Color.White)
            Text(desc, fontSize = 13.sp, color = (if (selected) Ink else Color.White).copy(alpha = 0.75f))
        }
    }
}
