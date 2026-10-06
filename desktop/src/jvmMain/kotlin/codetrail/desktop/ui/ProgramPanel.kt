package codetrail.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.core.command.Command
import codetrail.core.command.CommandSet
import codetrail.core.model.Dir
import codetrail.desktop.GameMode
import codetrail.desktop.GameState
import codetrail.desktop.res.predict_check
import codetrail.desktop.Phase
import codetrail.desktop.res.Res
import codetrail.desktop.res.program_title
import codetrail.desktop.res.run
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val Tray = Color(0xFFFFD83D)
private val SlotCard = Color(0x26FFFFFF)

/**
 * Right-hand HUD card: program slots on top, the yellow command tray and a big Run button below,
 * echoing the worksheet's layout.
 */
@Composable
fun ProgramPanel(state: GameState, modifier: Modifier = Modifier) {
    val level = state.level
    val scope = rememberCoroutineScope()
    val slotSize = 58

    Column(
        modifier
            .background(SlotCard, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(Res.string.program_title, state.program.size, level.maxSlots), fontSize = 20.sp, fontWeight = FontWeight.Bold)

        val rows = (level.maxSlots + 4) / 5
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
            modifier = Modifier.fillMaxWidth().height((rows * (slotSize + 8)).dp),
        ) {
            items(level.maxSlots) { i ->
                val cmd = state.program.getOrNull(i)
                if (cmd == null) {
                    EmptySlot(i, slotSize)
                } else {
                    key(i, cmd) {
                        val pop = remember { Animatable(0.4f) }
                        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
                        CommandCard(
                            cmd,
                            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                            size = slotSize,
                            highlighted = state.activeCommand == i && state.phase == Phase.RUNNING,
                            failed = (state.activeCommand == i && state.phase == Phase.FAILED && state.mode == GameMode.FORWARD) ||
                                (state.hintRemoveLast && i == state.program.lastIndex),
                            onClick = if (state.mode == GameMode.FORWARD) ({ state.removeCommand(i) }) else null,
                        )
                    }
                }
            }
        }

        if (state.mode == GameMode.FORWARD && !state.completed) CommandTray(state, level.commandSet)

        Button(
            onClick = { scope.launch { state.run() } },
            enabled = state.phase != Phase.RUNNING && state.program.isNotEmpty() && !state.completed &&
                (state.mode == GameMode.FORWARD || state.guess != null),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = Accent.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            val label = if (state.mode == GameMode.PREDICT) stringResource(Res.string.predict_check) else stringResource(Res.string.run)
            Text("▶  $label", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

/** Yellow tray with the available command cards, like the bar at the bottom of the worksheet. */
@Composable
private fun CommandTray(state: GameState, set: CommandSet) {
    val add: (Command) -> Unit = { state.addCommand(it) }
    CompositionLocalProvider(LocalContentColor provides Ink) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Tray, RoundedCornerShape(18.dp))
                .border(3.dp, Ink, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!set.relative) {
                    for (d in listOf(Dir.NORTH, Dir.SOUTH, Dir.WEST, Dir.EAST)) {
                        CommandCard(Command.Move(d), size = 62, hinted = state.hintCommand == Command.Move(d), onClick = { add(Command.Move(d)) })
                    }
                } else {
                    val maxRun = minOf(maxOf(state.level.grid.width, state.level.grid.height) - 1, set.maxForward)
                    val hintForward = state.hintCommand as? Command.Forward
                    if (hintForward != null && state.forwardCount != hintForward.cells) state.forwardCount = hintForward.cells
                    ForwardPicker(state.forwardCount, maxRun, hinted = hintForward != null, onChange = { state.forwardCount = it }) { add(Command.Forward(state.forwardCount)) }
                    if (set.degreeTurns) {
                        for (deg in listOf(90, 180, 270)) CommandCard(Command.Turn(deg), size = 62, hinted = state.hintCommand == Command.Turn(deg), onClick = { add(Command.Turn(deg)) })
                    } else {
                        CommandCard(Command.TurnLeft, size = 62, hinted = state.hintCommand == Command.TurnLeft, onClick = { add(Command.TurnLeft) })
                        CommandCard(Command.TurnRight, size = 62, hinted = state.hintCommand == Command.TurnRight, onClick = { add(Command.TurnRight) })
                    }
                    if (set.jump) CommandCard(Command.Jump, size = 62, hinted = state.hintCommand == Command.Jump, onClick = { add(Command.Jump) })
                }
            }
        }
    }
}

/** "Forward X" card with a - / + stepper underneath, so the number is set right where the card is. */
@Composable
private fun ForwardPicker(count: Int, max: Int, hinted: Boolean = false, onChange: (Int) -> Unit, onAdd: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CommandCard(Command.Forward(count), size = 62, hinted = hinted, onClick = onAdd)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepButton("−", enabled = count > 1) { onChange(count - 1) }
            StepButton("+", enabled = count < max) { onChange(count + 1) }
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .background(if (enabled) Ink else Ink.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Tray, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}
