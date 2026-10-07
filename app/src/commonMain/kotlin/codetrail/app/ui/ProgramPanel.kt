package codetrail.app.ui

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.core.command.Command
import codetrail.core.command.CommandSet
import codetrail.core.model.Dir
import codetrail.app.GameMode
import codetrail.app.GameState
import codetrail.app.res.predict_check
import codetrail.app.Phase
import codetrail.app.res.Res
import codetrail.app.res.program_title
import codetrail.app.res.block_hint
import codetrail.app.res.run
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val Tray = Color(0xFFFFD83D)
private val SlotCard = Color(0x26FFFFFF)
private val LoopFill = Color(0xFFEDE7F6)
private val LoopOpenFill = Color(0xFFD1C4E9)
private val BlockColor = Color(0xFF00897B)
private val BlockFill = Color(0xFFE0F2F1)
private val BlockOpenFill = Color(0xFFB2DFDB)

/**
 * Right-hand HUD card: program slots on top, the yellow command tray and a big Run button below,
 * echoing the worksheet's layout.
 */
@OptIn(ExperimentalLayoutApi::class)
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
        Text(stringResource(Res.string.program_title, state.slotsUsed, level.maxSlots), fontSize = 20.sp, fontWeight = FontWeight.Bold)

        val rows = (level.maxSlots + 4) / 5
        val editable = state.mode == GameMode.FORWARD
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = (rows * (slotSize + 8)).dp),
        ) {
            for ((i, cmd) in state.program.withIndex()) {
                val running = state.phase == Phase.RUNNING
                val active = state.activeCommand?.takeIf { it.index == i }
                val failedHere = state.activeCommand?.index == i && state.phase == Phase.FAILED && editable
                val removeLast = state.hintRemoveLast && i == state.program.lastIndex
                // While block A runs, the call card stays lit and the block panel shows the moving card.
                key(i, cmd) {
                    val pop = remember { Animatable(0.4f) }
                    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
                    val popMod = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    if (cmd is Command.Repeat) {
                        LoopCard(
                            loop = cmd,
                            size = slotSize,
                            modifier = popMod,
                            open = editable && state.canEdit && state.openLoop == i,
                            canGrow = state.canEdit && state.slotsUsed < level.maxSlots,
                            iteration = if (running) active?.iteration else null,
                            activeBody = if (running) active?.bodyIndex else null,
                            failedBody = if (failedHere) state.activeCommand?.bodyIndex else null,
                            failedWhole = removeLast,
                            onFocus = if (state.canEdit) ({ state.focusLoop(i) }) else null,
                            onRemove = if (state.canEdit) ({ state.removeCommand(i) }) else null,
                            onRemoveBody = if (state.canEdit) ({ j -> state.removeCommand(i, j) }) else null,
                        )
                    } else {
                        CommandCard(
                            cmd,
                            modifier = popMod,
                            size = slotSize,
                            highlighted = active != null && running,
                            failed = failedHere || removeLast,
                            onClick = if (editable) ({ state.removeCommand(i) }) else null,
                        )
                    }
                }
            }
            for (k in state.slotsUsed until level.maxSlots) EmptySlot(k, slotSize)
        }

        if (level.commandSet.functions) {
            val active = state.activeCommand
            FunctionPanel(
                body = state.function,
                size = slotSize,
                open = state.canEdit && state.editingFunction,
                canGrow = state.canEdit && state.slotsUsed < level.maxSlots,
                activeIndex = if (state.phase == Phase.RUNNING) active?.fnIndex else null,
                failedIndex = if (state.phase == Phase.FAILED && editable) active?.fnIndex else null,
                failedLast = state.hintRemoveLastFunction,
                onFocus = if (state.canEdit) ({ state.focusFunction() }) else null,
                onRemove = if (state.canEdit) ({ j -> state.removeFunctionCommand(j) }) else null,
            )
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

/**
 * A loop in the program: badge with "×N" on the left, the body cards inside a tinted frame.
 * Tapping the badge opens the loop so the tray adds cards into it; an open loop shows a "+" slot.
 */
@Composable
private fun LoopCard(
    loop: Command.Repeat,
    size: Int,
    modifier: Modifier = Modifier,
    open: Boolean,
    canGrow: Boolean,
    iteration: Int?,
    activeBody: Int?,
    failedBody: Int?,
    failedWhole: Boolean,
    onFocus: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    onRemoveBody: ((Int) -> Unit)?,
) {
    val shape = RoundedCornerShape(14.dp)
    val running = iteration != null
    val frame = when {
        failedWhole -> Color(0xFFFFCDD2)
        running -> Color(0xFFFFF59D)
        open -> LoopOpenFill
        else -> LoopFill
    }
    val inner = size - 10
    Row(
        modifier
            .height(size.dp)
            .clip(shape)
            .background(frame, shape)
            .border(if (open || running) 3.dp else 2.dp, if (failedWhole) Ink else Accent2, shape)
            // the whole frame takes focus; body cards and the cross sit on top and keep their own taps
            .then(if (onFocus != null) Modifier.clickable(onClick = onFocus) else Modifier)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier
                .size(inner.dp)
                .background(if (open) Accent2 else Color.Transparent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            RepeatGlyph(loop.times, iteration, inner, tint = if (open) Color.White else Ink)
        }
        for ((j, c) in loop.body.withIndex()) {
            CommandCard(
                c,
                size = inner,
                highlighted = activeBody == j,
                failed = failedBody == j,
                onClick = if (onRemoveBody != null) ({ onRemoveBody(j) }) else null,
            )
        }
        if (open && canGrow) {
            Box(
                Modifier
                    .size(inner.dp)
                    .border(2.dp, Accent2.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("+", color = Accent2, fontWeight = FontWeight.ExtraBold, fontSize = (inner * 0.4f).sp) }
        }
        if (onRemove != null && !running) {
            Box(
                Modifier.size(22.dp).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = Ink.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
}

/**
 * Block A: defined once under the program, called from anywhere with the "A" card.
 * Same focus rules as a loop: tap the frame to send new cards here, tap a card to remove it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FunctionPanel(
    body: List<Command>,
    size: Int,
    open: Boolean,
    canGrow: Boolean,
    activeIndex: Int?,
    failedIndex: Int?,
    failedLast: Boolean,
    onFocus: (() -> Unit)?,
    onRemove: ((Int) -> Unit)?,
) {
    val shape = RoundedCornerShape(14.dp)
    val running = activeIndex != null
    val fill = when {
        running -> Color(0xFFFFF59D)
        open -> BlockOpenFill
        else -> BlockFill
    }
    val inner = size - 10
    FlowRow(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill, shape)
            .border(if (open || running) 3.dp else 2.dp, BlockColor, shape)
            .then(if (onFocus != null) Modifier.clickable(onClick = onFocus) else Modifier)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier.size(inner.dp).background(if (open) BlockColor else Color.Transparent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CallGlyph((inner * 0.8f).toInt(), tint = if (open) Color.White else Ink)
                Text("=", color = if (open) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = (inner * 0.22f).sp)
            }
        }
        for ((j, c) in body.withIndex()) {
            CommandCard(
                c,
                size = inner,
                highlighted = activeIndex == j,
                failed = failedIndex == j || (failedLast && j == body.lastIndex),
                onClick = if (onRemove != null) ({ onRemove(j) }) else null,
            )
        }
        if (open && canGrow) {
            Box(
                Modifier.size(inner.dp).border(2.dp, BlockColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("+", color = BlockColor, fontWeight = FontWeight.ExtraBold, fontSize = (inner * 0.4f).sp) }
        }
        if (body.isEmpty() && !open) {
            Box(Modifier.height(inner.dp), contentAlignment = Alignment.CenterStart) {
                Text(stringResource(Res.string.block_hint), color = Ink.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp))
            }
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
                    CommandCard(Command.TurnLeft, size = 62, hinted = state.hintCommand == Command.TurnLeft, onClick = { add(Command.TurnLeft) })
                    CommandCard(Command.TurnRight, size = 62, hinted = state.hintCommand == Command.TurnRight, onClick = { add(Command.TurnRight) })
                    if (set.jump) CommandCard(Command.Jump, size = 62, hinted = state.hintCommand == Command.Jump, onClick = { add(Command.Jump) })
                    if (set.functions) {
                        // Block A cannot call itself, so the card is greyed while the block has focus.
                        val enabled = !state.editingFunction
                        Box(Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.45f }) {
                            CommandCard(Command.Call, size = 62, hinted = state.hintCommand == Command.Call, onClick = if (enabled) ({ add(Command.Call) }) else null)
                        }
                    }
                    if (set.loops) {
                        val hintLoop = state.hintCommand as? Command.Repeat
                        if (hintLoop != null && state.repeatCount != hintLoop.times) state.repeatCount = hintLoop.times
                        // A loop card can only be added at the top level, never inside another loop.
                        RepeatPicker(
                            state.repeatCount,
                            hinted = hintLoop != null,
                            enabled = state.openLoop == null && !state.editingFunction,
                            onChange = { state.repeatCount = it },
                        ) { add(Command.Repeat(state.repeatCount, emptyList())) }
                    }
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

/** "Repeat ×N" card with a - / + stepper, same shape as the forward picker. */
@Composable
private fun RepeatPicker(count: Int, hinted: Boolean, enabled: Boolean, onChange: (Int) -> Unit, onAdd: () -> Unit) {
    Column(
        Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.45f },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CommandCard(Command.Repeat(count, emptyList()), size = 62, hinted = hinted, onClick = if (enabled) onAdd else null)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepButton("−", enabled = enabled && count > 2) { onChange(count - 1) }
            StepButton("+", enabled = enabled && count < CommandSet.MAX_REPEAT) { onChange(count + 1) }
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
