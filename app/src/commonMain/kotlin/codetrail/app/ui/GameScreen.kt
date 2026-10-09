package codetrail.app.ui

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
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.core.engine.Failure
import codetrail.app.AppState
import codetrail.app.GameMode
import codetrail.app.GameState
import codetrail.app.res.hint
import codetrail.app.res.hint_next
import codetrail.app.res.hint_ready
import codetrail.app.res.hint_remove
import codetrail.app.res.hint_remove_block
import codetrail.app.res.level_done
import codetrail.app.res.predict_correct
import codetrail.app.res.predict_prompt
import codetrail.app.res.predict_wrong
import codetrail.app.Phase
import codetrail.app.res.Res
import codetrail.app.res.clear
import codetrail.app.res.commands_count
import codetrail.app.res.fail_bad_landing
import codetrail.app.res.fail_bumped
import codetrail.app.res.fail_not_allowed
import codetrail.app.res.fail_not_at_goal
import codetrail.app.res.fail_too_many
import codetrail.app.res.game_level
import codetrail.app.res.game_menu
import codetrail.app.res.legend_title
import codetrail.app.res.legend_move
import codetrail.app.res.legend_forward
import codetrail.app.res.legend_turn_left
import codetrail.app.res.legend_turn_right
import codetrail.app.res.legend_jump
import codetrail.app.res.legend_repeat
import codetrail.app.res.legend_call
import codetrail.app.res.legend_goal
import codetrail.app.res.close
import codetrail.core.command.Command
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import codetrail.core.model.Dir
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import codetrail.app.res.new_level
import codetrail.app.res.pause_exit
import codetrail.app.res.pause_new_level
import codetrail.app.res.pause_resume
import codetrail.app.res.pause_title
import codetrail.app.res.stars_total
import codetrail.app.res.status_goal
import codetrail.app.res.status_running
import codetrail.app.res.status_won
import codetrail.app.res.unlock_great
import codetrail.app.res.unlock_title
import codetrail.app.res.unlocked_message
import codetrail.app.res.won_payout
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun GameScreen(app: AppState, state: GameState) {
    val theme = state.theme
    val ink = if (theme.dark) Color(0xFFF2F2F2) else Ink
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(theme.background)
            // a tap on anything that is not a control drops the loop focus
            .pointerInput(Unit) { detectTapGestures { state.clearSelection() } }
            // Desktop keys: Delete removes the selected card, Ctrl/Cmd+Z undoes.
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    e.key == Key.Delete || e.key == Key.Backspace -> { state.removeSelected(); true }
                    e.key == Key.Z && (e.isCtrlPressed || e.isMetaPressed) -> { state.undo(); true }
                    e.key == Key.Escape -> { state.clearSelection(); true }
                    else -> false
                }
            },
    ) {
        var legend by remember { mutableStateOf(false) }
        CompositionLocalProvider(LocalContentColor provides ink) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TopBar(app, state)

                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1.45f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // The board takes what is left above the bubble and buttons, never more:
                        // on a wide screen it is height-bound, on a narrow one width-bound.
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            val ratio = boardAspect(state.level)
                            val width = minOf(maxWidth, maxHeight * ratio)
                        Board(
                            state.level, state.hero, theme, state.character, Modifier.width(width),
                            effectKey = state.runId.takeIf { it > 0 },
                            won = state.phase == Phase.WON,
                            stars = state.stars,
                            failure = state.failure,
                            guess = if (state.mode == GameMode.PREDICT) state.guess else null,
                            answer = if (state.mode == GameMode.PREDICT && state.phase == Phase.WON) state.predict?.answer else null,
                            onCellClick = if (state.mode == GameMode.PREDICT) ({ state.selectGuess(it) }) else null,
                        )
                        }
                        SpeechBubble(state)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (state.mode == GameMode.FORWARD) {
                                // After a hint the button shows the best rating still possible.
                                val cap = if (state.hintsUsed > 0) "  ·  ${"★".repeat(state.maxStars)}" else ""
                                SmallAction("💡  " + stringResource(Res.string.hint) + cap, enabled = state.canEdit) { state.hint() }
                            }
                            Spacer(Modifier.weight(1f))
                            if (state.mode == GameMode.FORWARD) {
                                SmallAction("✕  " + stringResource(Res.string.clear), enabled = state.canEdit && state.program.isNotEmpty()) { state.clearProgram() }
                            }
                            SmallAction("✦  " + stringResource(Res.string.new_level), enabled = state.phase != Phase.RUNNING, filled = true) { state.newLevel() }
                        }
                    }
                    ProgramPanel(state, Modifier.weight(1f), onLegend = { legend = true })
                }
            }
        }
        if (app.paused) PauseOverlay(app, state)
        if (legend) LegendOverlay(state) { legend = false }
        val unlocked = state.justUnlocked
        if (state.unlockSplash && unlocked != null) UnlockOverlay(unlocked) { state.dismissUnlock() }
    }
}

@Composable
private fun TopBar(app: AppState, state: GameState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MenuButton(app, state)
        val muted = LocalContentColor.current.copy(alpha = 0.6f)
        // World as the screen title, level and difficulty as a muted subtitle.
        Column(Modifier.padding(start = 4.dp)) {
            Text(stringResource(state.theme.name), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                stringResource(Res.string.game_level, state.tier) + "  ·  " + stringResource(DifficultyNames[state.tier - 1]),
                fontSize = 14.sp,
                color = muted,
                fontWeight = FontWeight.SemiBold,
            )
        }

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
private fun MenuButton(app: AppState, state: GameState) =
    PillButton("☰", stringResource(Res.string.game_menu), enabled = state.phase != Phase.RUNNING) { app.pause() }

@Composable
private fun PillButton(glyph: String, text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Pill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Reference sheet: every card of the level's command set with one line on what it does. */
@Composable
private fun LegendOverlay(state: GameState, onDismiss: () -> Unit) {
    val set = state.level.commandSet
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(620.dp)
                .background(MenuBackground, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.legend_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!set.relative) {
                    LegendRow(stringResource(Res.string.legend_move)) {
                        for (d in listOf(Dir.NORTH, Dir.SOUTH, Dir.WEST, Dir.EAST)) CommandCard(Command.Move(d), size = 48)
                    }
                } else {
                    LegendRow(stringResource(Res.string.legend_forward)) { CommandCard(Command.Forward(3), size = 48) }
                    LegendRow(stringResource(Res.string.legend_turn_left)) { CommandCard(Command.TurnLeft, size = 48) }
                    LegendRow(stringResource(Res.string.legend_turn_right)) { CommandCard(Command.TurnRight, size = 48) }
                }
                if (set.jump) LegendRow(stringResource(Res.string.legend_jump)) { CommandCard(Command.Jump, size = 48) }
                if (set.loops) LegendRow(stringResource(Res.string.legend_repeat)) { CommandCard(Command.Repeat(3, emptyList()), size = 48) }
                if (set.functions) LegendRow(stringResource(Res.string.legend_call)) { CommandCard(Command.Call, size = 48) }
                LegendRow(stringResource(Res.string.legend_goal)) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Flag, null, Modifier.size(30.dp), tint = Star) }
                }
            }
            Spacer(Modifier.height(20.dp))
            MenuButton(stringResource(Res.string.close), width = 200.dp, onClick = onDismiss)
        }
    }
}

@Composable
private fun LegendRow(text: String, cards: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards()
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SmallAction(text: String, enabled: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = if (filled) {
            ButtonDefaults.buttonColors(containerColor = Accent2, disabledContainerColor = Accent2.copy(alpha = 0.5f), disabledContentColor = Color.White.copy(alpha = 0.7f))
        } else {
            ButtonDefaults.buttonColors(containerColor = Pill, contentColor = LocalContentColor.current, disabledContainerColor = Pill.copy(alpha = 0.6f), disabledContentColor = LocalContentColor.current.copy(alpha = 0.55f))
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
private fun statusText(state: GameState): String {
    if (state.completed && state.phase != Phase.WON) return stringResource(Res.string.level_done)
    if (state.mode == GameMode.PREDICT) return predictText(state)
    state.hintCommand?.let { return stringResource(Res.string.hint_next, commandLabel(it)) }
    if (state.hintRemoveLast) return stringResource(Res.string.hint_remove)
    if (state.hintRemoveLastFunction) return stringResource(Res.string.hint_remove_block)
    if (state.hintReady) return stringResource(Res.string.hint_ready)
    return forwardText(state)
}

@Composable
private fun predictText(state: GameState): String = when (state.phase) {
    Phase.WON -> stringResource(Res.string.predict_correct) + " " + "★".repeat(state.stars) + "☆".repeat(3 - state.stars) +
        (if (state.payout > 0) "  " + stringResource(Res.string.won_payout, state.payout) else "")
    Phase.FAILED -> stringResource(Res.string.predict_wrong)
    Phase.RUNNING -> stringResource(Res.string.status_running)
    Phase.EDITING -> stringResource(Res.string.predict_prompt, stringResource(state.character.name))
}

@Composable
private fun forwardText(state: GameState): String = when (state.phase) {
    Phase.EDITING -> stringResource(
        Res.string.status_goal,
        stringResource(state.character.name),
        stringResource(state.theme.goalName),
        pluralStringResource(Res.plurals.commands_count, state.level.optimalLength, state.level.optimalLength),
    )
    Phase.RUNNING -> stringResource(Res.string.status_running)
    Phase.WON -> {
        val base = stringResource(Res.string.status_won) + " " + "★".repeat(state.stars) + "☆".repeat(3 - state.stars) +
            (if (state.payout > 0) "  " + stringResource(Res.string.won_payout, state.payout) else "")
        val unlocked = state.justUnlocked
        if (unlocked != null) base + "  " + stringResource(Res.string.unlocked_message, stringResource(unlocked.name)) else base
    }
    Phase.FAILED -> when (state.failure) {
        is Failure.Fell -> stringResource(state.theme.fallMessage)
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
            MenuButton(stringResource(Res.string.pause_new_level)) { state.newLevel(); app.resume() }
            MenuButton(stringResource(Res.string.pause_exit), color = Color.White.copy(alpha = 0.15f), textColor = Color.White) { app.exitToMenu() }
        }
    }
}

@Composable
private fun UnlockOverlay(c: codetrail.app.theme.Character, onDismiss: () -> Unit) {
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
