package codetrail.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.core.engine.Failure
import codetrail.desktop.AppState
import codetrail.desktop.GameState
import codetrail.desktop.Phase
import codetrail.desktop.res.Res
import codetrail.desktop.res.clear
import codetrail.desktop.res.commands_count
import codetrail.desktop.res.fail_bad_landing
import codetrail.desktop.res.fail_bumped
import codetrail.desktop.res.fail_fell
import codetrail.desktop.res.fail_not_allowed
import codetrail.desktop.res.fail_not_at_goal
import codetrail.desktop.res.fail_too_many
import codetrail.desktop.res.game_level
import codetrail.desktop.res.game_menu
import codetrail.desktop.res.new_level
import codetrail.desktop.res.pause_exit
import codetrail.desktop.res.pause_new_level
import codetrail.desktop.res.pause_restart
import codetrail.desktop.res.pause_resume
import codetrail.desktop.res.pause_title
import codetrail.desktop.res.reset
import codetrail.desktop.res.stars_total
import codetrail.desktop.res.status_goal
import codetrail.desktop.res.status_running
import codetrail.desktop.res.status_won
import codetrail.desktop.res.unlock_great
import codetrail.desktop.res.unlock_title
import codetrail.desktop.res.unlocked_message
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val Pill = Color(0x26FFFFFF)

@Composable
fun GameScreen(app: AppState, state: GameState) {
    val theme = state.theme
    val ink = if (theme.dark) Color(0xFFF2F2F2) else Ink

    Box(Modifier.fillMaxSize().background(theme.background)) {
        CompositionLocalProvider(LocalContentColor provides ink) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TopBar(app, state)

                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1.45f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Board(
                            state.level, state.hero, theme, state.character, Modifier.fillMaxWidth(),
                            effectKey = state.runId.takeIf { it > 0 },
                            won = state.phase == Phase.WON,
                            stars = state.stars,
                            failure = state.failure,
                        )
                        SpeechBubble(state)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            SmallAction("↺  " + stringResource(Res.string.reset), enabled = state.phase != Phase.RUNNING) { state.resetRun() }
                            SmallAction("✕  " + stringResource(Res.string.clear), enabled = state.canEdit && state.program.isNotEmpty()) { state.clearProgram() }
                            SmallAction("✦  " + stringResource(Res.string.new_level), enabled = state.phase != Phase.RUNNING, filled = true) { state.newLevel() }
                        }
                    }
                    ProgramPanel(state, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        if (app.paused) PauseOverlay(app, state)
        val unlocked = state.justUnlocked
        if (state.unlockSplash && unlocked != null) UnlockOverlay(unlocked) { state.dismissUnlock() }
    }
}

@Composable
private fun TopBar(app: AppState, state: GameState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Pill)
                .clickable(enabled = state.phase != Phase.RUNNING) { app.pause() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("☰", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.game_menu), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Crumb(stringResource(state.theme.name))
        Crumb(stringResource(Res.string.game_level, state.tier))
        Crumb(stringResource(DifficultyNames[state.tier - 1]))

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(Pill).padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.stars_total, state.profile.progress.totalStars), color = Star, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Avatar(state.character, 40.dp, selected = true)
            Text(state.profile.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Crumb(text: String) {
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Pill).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun SmallAction(text: String, enabled: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = if (filled) {
            ButtonDefaults.buttonColors(containerColor = Accent2, disabledContainerColor = Accent2.copy(alpha = 0.35f))
        } else {
            ButtonDefaults.buttonColors(containerColor = Pill, contentColor = LocalContentColor.current, disabledContainerColor = Pill.copy(alpha = 0.3f), disabledContentColor = LocalContentColor.current.copy(alpha = 0.4f))
        },
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

/** The hero "talks": a white bubble with a tail pointing at the hero portrait. */
@Composable
private fun SpeechBubble(state: GameState) {
    val text = statusText(state)
    val won = state.phase == Phase.WON
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(state.character, 52.dp, selected = true)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(14.dp)
                .graphicsLayer { rotationZ = 45f; translationX = 10f }
                .background(if (won) Star else Color.White, RoundedCornerShape(2.dp)),
        )
        Box(
            Modifier
                .weight(1f)
                .background(if (won) Star else Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text(text, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun statusText(state: GameState): String = when (state.phase) {
    Phase.EDITING -> stringResource(
        Res.string.status_goal,
        stringResource(state.character.name),
        stringResource(state.theme.goalName),
        pluralStringResource(Res.plurals.commands_count, state.level.optimalLength, state.level.optimalLength),
    )
    Phase.RUNNING -> stringResource(Res.string.status_running)
    Phase.WON -> {
        val base = stringResource(Res.string.status_won) + " " + "★".repeat(state.stars) + "☆".repeat(3 - state.stars)
        val unlocked = state.justUnlocked
        if (unlocked != null) base + "  " + stringResource(Res.string.unlocked_message, stringResource(unlocked.name)) else base
    }
    Phase.FAILED -> when (state.failure) {
        is Failure.Fell -> stringResource(Res.string.fail_fell)
        is Failure.Bumped -> stringResource(Res.string.fail_bumped)
        is Failure.BadLanding -> stringResource(Res.string.fail_bad_landing)
        is Failure.NotAtGoal -> stringResource(Res.string.fail_not_at_goal)
        is Failure.TooManyCommands -> stringResource(Res.string.fail_too_many)
        is Failure.NotAllowed -> stringResource(Res.string.fail_not_allowed)
        null -> ""
    }
}

@Composable
private fun PauseOverlay(app: AppState, state: GameState) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(
            Modifier.background(MenuBackground, RoundedCornerShape(24.dp)).padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.pause_title), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            MenuButton(stringResource(Res.string.pause_resume), color = Accent, textColor = Color.White) { app.resume() }
            MenuButton(stringResource(Res.string.pause_restart)) { state.restartLevel(); app.resume() }
            MenuButton(stringResource(Res.string.pause_new_level)) { state.newLevel(); app.resume() }
            MenuButton(stringResource(Res.string.pause_exit), color = Color.White.copy(alpha = 0.15f), textColor = Color.White) { app.exitToMenu() }
        }
    }
}

@Composable
private fun UnlockOverlay(c: codetrail.desktop.theme.Character, onDismiss: () -> Unit) {
    val pop = remember { Animatable(0.2f) }
    LaunchedEffect(c.id) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    val wobble = rememberInfiniteTransition()
    val tilt by wobble.animateFloat(-4f, 4f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse))

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
        Column(
            Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }.background(MenuBackground, RoundedCornerShape(28.dp)).padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.unlock_title), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Star)
            Spacer(Modifier.height(16.dp))
            Image(characterPainter(c), null, Modifier.size(240.dp).graphicsLayer { rotationZ = tilt })
            Spacer(Modifier.height(12.dp))
            Text(stringResource(c.name), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))
            MenuButton(stringResource(Res.string.unlock_great), color = Accent, textColor = Color.White, width = 240.dp, onClick = onDismiss)
        }
    }
}
