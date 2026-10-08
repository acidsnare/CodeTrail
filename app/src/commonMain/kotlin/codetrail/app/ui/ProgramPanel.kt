package codetrail.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import codetrail.app.sound.Sfx
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import codetrail.app.GameMode
import codetrail.app.GameState
import codetrail.app.Phase
import codetrail.app.Selection
import codetrail.app.Slot
import codetrail.app.res.Res
import codetrail.app.res.block_hint
import codetrail.app.res.predict_check
import codetrail.app.res.program_title
import codetrail.app.res.run
import codetrail.app.res.undo
import androidx.compose.foundation.layout.Spacer
import codetrail.core.command.Command
import codetrail.core.command.CommandSet
import codetrail.core.model.Dir
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val Tray = Color(0xFFFFD83D)
private val SlotCard = Color(0x26FFFFFF)
private val LoopFill = Color(0xFFEDE7F6)
private val LoopOpenFill = Color(0xFFD1C4E9)
private val BlockColor = Color(0xFF00897B)
private val BlockFill = Color(0xFFE0F2F1)
private val BlockOpenFill = Color(0xFFB2DFDB)
private val Danger = Color(0xFFE53935)

/**
 * Right-hand HUD card: program slots on top, block A when the tier has it, the yellow command
 * tray and a big Run button below.
 *
 * Editing: tap a tray card to add it after the selected card (or at the end); tap a program
 * card to select it, which shows a delete badge and, for numbered cards, a -/+ stepper; drag
 * cards from the tray or within the program to place them exactly.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramPanel(state: GameState, modifier: Modifier = Modifier, onLegend: () -> Unit = {}) {
    val level = state.level
    val scope = rememberCoroutineScope()
    val slotSize = 58
    val editable = state.mode == GameMode.FORWARD
    val dnd = remember(state) { DragController(state, scope) }
    // The controller keeps bounds of cards that are gone after an edit; refresh from scratch each time.
    LaunchedEffect(state.program.toList(), state.function.toList()) { dnd.cards.clear() }

    Box(modifier.onGloballyPositioned { dnd.root = it }) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(SlotCard, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.program_title, state.slotsUsed, level.maxSlots), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (editable) {
                    // Undo lives next to what it undoes.
                    val enabled = state.canEdit && state.canUndo
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(LocalContentColor.current.copy(alpha = if (enabled) 0.12f else 0.05f))
                            .clickable(enabled = enabled) { state.undo() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("↶  " + stringResource(Res.string.undo), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.4f))
                    }
                }
            }

            val rows = (level.maxSlots + 4) / 5
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = (rows * (slotSize + 8)).dp)
                    .onGloballyPositioned { c -> dnd.toRoot(c)?.let { dnd.containers[Container.Top] = it } },
            ) {
                for ((i, cmd) in state.program.withIndex()) {
                    val running = state.phase == Phase.RUNNING
                    val active = state.activeCommand?.takeIf { it.index == i }
                    val failedHere = state.activeCommand?.index == i && state.phase == Phase.FAILED && editable
                    val removeLast = state.hintRemoveLast && i == state.program.lastIndex
                    key(i, cmd) {
                        val pop = remember { Animatable(0.4f) }
                        LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f)) }
                        val popMod = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                        if (cmd is Command.Repeat) {
                            LoopCard(
                                state, dnd, i, cmd, slotSize, popMod,
                                iteration = if (running) active?.iteration else null,
                                activeBody = if (running) active?.bodyIndex else null,
                                failedBody = if (failedHere) state.activeCommand?.bodyIndex else null,
                                failedWhole = removeLast,
                                editable = editable,
                            )
                        } else {
                            ProgramCard(
                                state, dnd, Slot.Top(i), cmd, slotSize, popMod,
                                highlighted = active != null && running,
                                failed = failedHere || removeLast,
                                editable = editable,
                            )
                        }
                    }
                }
                for (k in state.slotsUsed until level.maxSlots) EmptySlot(k, slotSize)
            }

            if (level.commandSet.functions) {
                FunctionPanel(state, dnd, slotSize, editable)
            }

            if (editable && !state.completed) CommandTray(state, dnd, level.commandSet, onLegend)

            Button(
                onClick = { scope.launch { state.run() } },
                enabled = state.phase != Phase.RUNNING && state.program.isNotEmpty() && !state.completed &&
                    (editable || state.guess != null),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = Accent.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                val label = if (state.mode == GameMode.PREDICT) stringResource(Res.string.predict_check) else stringResource(Res.string.run)
                Text("▶  $label", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }

        DragOverlay(dnd, slotSize)
    }
}

/** Insertion marker under the dragged card, the ghost card itself, and the fly-back animation. */
@Composable
private fun DragOverlay(dnd: DragController, slotSize: Int) {
    val density = LocalDensity.current
    val drag = dnd.drag
    if (drag != null) {
        Canvas(Modifier.fillMaxSize()) {
            val t = drag.target ?: return@Canvas
            val (rects, container) = when (t) {
                is Slot.Top -> dnd.topCards() to dnd.containers[Container.Top]
                is Slot.InLoop -> dnd.loopCards(t.loop) to dnd.containers[Container.Loop(t.loop)]
                is Slot.InFunction -> dnd.functionCards() to dnd.containers[Container.Function]
            }
            val index = when (t) { is Slot.Top -> t.index; is Slot.InLoop -> t.index; is Slot.InFunction -> t.index }
            val gap = 4.dp.toPx()
            val bar: Rect = when {
                index < rects.size -> rects[index].let { Rect(it.left - gap - 2.dp.toPx(), it.top, it.left - gap + 2.dp.toPx(), it.bottom) }
                rects.isNotEmpty() -> rects.last().let { Rect(it.right + gap - 2.dp.toPx(), it.top, it.right + gap + 2.dp.toPx(), it.bottom) }
                container != null -> {
                    val h = (slotSize - 10).dp.toPx()
                    val x = container.left + (if (t is Slot.Top) 8.dp.toPx() else 10.dp.toPx() + (slotSize - 10).dp.toPx() + 5.dp.toPx())
                    val y = container.center.y - h / 2
                    Rect(x - 2.dp.toPx(), y, x + 2.dp.toPx(), y + h)
                }
                else -> return@Canvas
            }
            drawRoundRect(if (drag.valid) Accent2 else Danger, bar.topLeft, bar.size, androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        }
        Ghost(drag.command, drag.topLeft, drag.size, lifted = true)
    }
    dnd.flyBack?.let { (cmd, pos) -> Ghost(cmd, pos, dnd.flyBackCardSize, lifted = false) }
}

@Composable
private fun Ghost(command: Command, topLeft: Offset, size: Size, lifted: Boolean) {
    val density = LocalDensity.current
    val w = with(density) { size.width.toDp() }
    val h = with(density) { size.height.toDp() }
    Box(
        Modifier
            .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            .size(w, h)
            .graphicsLayer {
                val s = if (lifted) 1.08f else 1f
                scaleX = s; scaleY = s
                alpha = if (lifted) 0.95f else 0.8f
            }
            .shadow(if (lifted) 12.dp else 0.dp, RoundedCornerShape(12.dp)),
    ) {
        CommandCard(command, size = with(density) { size.width.toDp() }.value.toInt(), modifier = Modifier.fillMaxSize())
    }
}

/**
 * A card in the program: tap selects, long-press (touch) or drag (mouse) moves. A selected card
 * shows the delete badge; "forward N" and "repeat xN" also get a -/+ stepper.
 */
@Composable
private fun ProgramCard(
    state: GameState,
    dnd: DragController,
    slot: Slot,
    cmd: Command,
    size: Int,
    modifier: Modifier = Modifier,
    highlighted: Boolean,
    failed: Boolean,
    editable: Boolean,
) {
    val selected = editable && state.selection == Selection.Card(slot)
    val dragging = dnd.drag?.source == slot
    Box(modifier.then(if (selected) Modifier.zIndex(2f) else Modifier)) {
        CommandCard(
            cmd,
            size = size,
            highlighted = highlighted,
            failed = failed,
            selected = selected,
            modifier = Modifier
                .graphicsLayer { alpha = if (dragging) 0.3f else 1f }
                .dragSource(dnd, { cmd }, slot, { editable && state.canEdit }, onBounds = { dnd.cards[slot] = it }),
            onClick = if (editable) ({ state.select(Selection.Card(slot)) }) else null,
        )
        if (selected && state.canEdit) SelectionControls(cmd, size, onDelete = { state.removeSelected() }, onAdjust = { state.adjustSelected(it) })
    }
}

/** Delete badge in the top-right corner and, for numbered cards, -/+ at the bottom corners. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.SelectionControls(cmd: Command, size: Int, onDelete: () -> Unit, onAdjust: (Int) -> Unit) {
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .offset(x = 8.dp, y = (-8).dp)
            .size(24.dp)
            .shadow(3.dp, CircleShape)
            .background(Danger, CircleShape)
            .clickable(onClick = onDelete),
        contentAlignment = Alignment.Center,
    ) { Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    if (cmd is Command.Forward || cmd is Command.Repeat) {
        Row(
            Modifier.align(Alignment.BottomCenter).offset(y = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniStep("−") { onAdjust(-1) }
            MiniStep("+") { onAdjust(+1) }
        }
    }
}

@Composable
private fun MiniStep(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(22.dp).shadow(3.dp, CircleShape).background(Ink, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Tray, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
}

/**
 * A loop in the program: badge with "×N" on the left, the body cards inside a tinted frame.
 * Tapping the frame selects the loop (tray cards then go inside, the badge gets delete and -/+);
 * dragging the badge moves the whole loop.
 */
@Composable
private fun LoopCard(
    state: GameState,
    dnd: DragController,
    index: Int,
    loop: Command.Repeat,
    size: Int,
    modifier: Modifier = Modifier,
    iteration: Int?,
    activeBody: Int?,
    failedBody: Int?,
    failedWhole: Boolean,
    editable: Boolean,
) {
    val shape = RoundedCornerShape(14.dp)
    val running = iteration != null
    val selected = editable && state.selection == Selection.Loop(index)
    val dropHere = dnd.drag?.target.let { it is Slot.InLoop && it.loop == index }
    val dragging = dnd.drag?.source == Slot.Top(index)
    val frame = when {
        failedWhole -> Color(0xFFFFCDD2)
        running -> Color(0xFFFFF59D)
        selected || dropHere -> LoopOpenFill
        else -> LoopFill
    }
    val inner = size - 10
    Box(modifier.then(if (selected) Modifier.zIndex(2f) else Modifier)) {
        Row(
            Modifier
                .height(size.dp)
                .graphicsLayer { alpha = if (dragging) 0.3f else 1f }
                .clip(shape)
                .background(frame, shape)
                .border(if (selected || running || dropHere) 3.dp else 2.dp, if (failedWhole) Ink else Accent2, shape)
                .then(if (editable) Modifier.clickable { state.select(Selection.Loop(index)) } else Modifier)
                .onGloballyPositioned { c -> dnd.toRoot(c)?.let { dnd.containers[Container.Loop(index)] = it; dnd.cards[Slot.Top(index)] = it } }
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                Modifier
                    .size(inner.dp)
                    .background(if (selected) Accent2 else Color.Transparent, RoundedCornerShape(10.dp))
                    .dragSource(dnd, { loop }, Slot.Top(index), { editable && state.canEdit }),
                contentAlignment = Alignment.Center,
            ) {
                RepeatGlyph(loop.times, iteration, inner, tint = if (selected) Color.White else Ink)
            }
            for ((j, c) in loop.body.withIndex()) {
                ProgramCard(
                    state, dnd, Slot.InLoop(index, j), c, inner,
                    highlighted = activeBody == j,
                    failed = failedBody == j,
                    editable = editable,
                )
            }
            if (loop.body.isEmpty()) {
                Box(
                    Modifier.size(inner.dp).border(2.dp, Accent2.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("+", color = Accent2, fontWeight = FontWeight.ExtraBold, fontSize = (inner * 0.4f).sp) }
            }
        }
        if (selected && state.canEdit) SelectionControls(loop, size, onDelete = { state.removeSelected() }, onAdjust = { state.adjustSelected(it) })
    }
}

/**
 * Block A: defined once under the program, called from anywhere with the "A" card.
 * Tap the frame to select it (tray cards then go inside), tap a card to select the card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FunctionPanel(state: GameState, dnd: DragController, size: Int, editable: Boolean) {
    val body = state.function
    val active = state.activeCommand
    val activeIndex = if (state.phase == Phase.RUNNING) active?.fnIndex else null
    val failedIndex = if (state.phase == Phase.FAILED && editable) active?.fnIndex else null
    val shape = RoundedCornerShape(14.dp)
    val running = activeIndex != null
    val selected = editable && state.selection == Selection.Function
    val dropHere = dnd.drag?.target is Slot.InFunction
    val fill = when {
        running -> Color(0xFFFFF59D)
        selected || dropHere -> BlockOpenFill
        else -> BlockFill
    }
    val inner = size - 10
    FlowRow(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill, shape)
            .border(if (selected || running || dropHere) 3.dp else 2.dp, BlockColor, shape)
            .then(if (editable) Modifier.clickable { state.select(Selection.Function) } else Modifier)
            .onGloballyPositioned { c -> dnd.toRoot(c)?.let { dnd.containers[Container.Function] = it } }
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier.size(inner.dp).background(if (selected) BlockColor else Color.Transparent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CallGlyph((inner * 0.8f).toInt(), tint = if (selected) Color.White else Ink)
                Text("=", color = if (selected) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = (inner * 0.22f).sp)
            }
        }
        for ((j, c) in body.withIndex()) {
            ProgramCard(
                state, dnd, Slot.InFunction(j), c, inner,
                highlighted = activeIndex == j,
                failed = failedIndex == j || (state.hintRemoveLastFunction && j == body.lastIndex),
                editable = editable,
            )
        }
        if (body.isEmpty()) {
            Box(
                Modifier.size(inner.dp).border(2.dp, BlockColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("+", color = BlockColor, fontWeight = FontWeight.ExtraBold, fontSize = (inner * 0.4f).sp) }
            if (!selected) {
                Box(Modifier.height(inner.dp), contentAlignment = Alignment.CenterStart) {
                    // Two lines in every language so the panel keeps one height whether the hint wraps or not.
                    Text(stringResource(Res.string.block_hint), color = Ink.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, minLines = 2, maxLines = 2, modifier = Modifier.padding(horizontal = 6.dp))
                }
            }
        }
    }
}

/** Yellow tray with the available command cards. Tap adds, drag places. */
@Composable
private fun CommandTray(state: GameState, dnd: DragController, set: CommandSet, onLegend: () -> Unit) {
    val add: (Command) -> Unit = { state.addCommand(it) }
    val canAdd = { state.canEdit && state.slotsUsed < state.level.maxSlots }
    CompositionLocalProvider(LocalContentColor provides Ink) {
      Box {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Tray, RoundedCornerShape(18.dp))
                .border(3.dp, Ink, RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                @Composable
                fun tray(c: Command, hinted: Boolean = state.hintCommand == c) {
                    CommandCard(c, size = 62, hinted = hinted, modifier = Modifier.dragSource(dnd, { c }, null, canAdd), onClick = { add(c) })
                }
                if (!set.relative) {
                    for (d in listOf(Dir.NORTH, Dir.SOUTH, Dir.WEST, Dir.EAST)) tray(Command.Move(d))
                } else {
                    val maxRun = minOf(maxOf(state.level.grid.width, state.level.grid.height) - 1, set.maxForward)
                    val hintForward = state.hintCommand as? Command.Forward
                    if (hintForward != null && state.forwardCount != hintForward.cells) state.forwardCount = hintForward.cells
                    Picker(
                        card = { tray(Command.Forward(state.forwardCount), hinted = hintForward != null) },
                        canDec = state.forwardCount > 1, canInc = state.forwardCount < maxRun,
                        onChange = { state.forwardCount += it },
                    )
                    tray(Command.TurnLeft)
                    tray(Command.TurnRight)
                    if (set.jump) tray(Command.Jump)
                    if (set.functions) tray(Command.Call)
                    if (set.loops) {
                        val hintLoop = state.hintCommand as? Command.Repeat
                        if (hintLoop != null && state.repeatCount != hintLoop.times) state.repeatCount = hintLoop.times
                        Picker(
                            card = { tray(Command.Repeat(state.repeatCount, emptyList()), hinted = hintLoop != null) },
                            canDec = state.repeatCount > 2, canInc = state.repeatCount < CommandSet.MAX_REPEAT,
                            onChange = { state.repeatCount += it },
                        )
                    }
                }
            }
        }
        // "What do these cards do?" sits on the tray's corner, right next to the cards it explains.
        val sounds = LocalSounds.current
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 12.dp, y = (-12).dp)
                .size(34.dp)
                .background(Ink, CircleShape)
                .border(2.dp, Tray, CircleShape)
                .clickable { sounds.play(Sfx.CLICK); onLegend() },
            contentAlignment = Alignment.Center,
        ) {
            Text("?", color = Tray, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
      }
    }
}

/** A tray card with a - / + stepper underneath, so the number is set right where the card is. */
@Composable
private fun Picker(card: @Composable () -> Unit, canDec: Boolean, canInc: Boolean, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        card()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepButton("−", enabled = canDec) { onChange(-1) }
            StepButton("+", enabled = canInc) { onChange(+1) }
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
