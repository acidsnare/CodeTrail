package codetrail.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import codetrail.app.GameState
import codetrail.app.Slot
import codetrail.core.command.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A place cards can be dropped into. */
sealed interface Container {
    data object Top : Container
    data class Loop(val index: Int) : Container
    data object Function : Container
}

/** A card in flight: what, where it came from (null for the tray), where it is, where it would land. */
data class DragState(
    val command: Command,
    val source: Slot?,
    val size: Size,
    val grab: Offset,
    val pointer: Offset,
    val target: Slot?,
    val valid: Boolean,
) {
    val topLeft: Offset get() = pointer - grab
}

/**
 * One drag at a time for the program panel. Cards and containers report their bounds (in the
 * panel's coordinates) while they are laid out; the controller turns the pointer position into
 * an insertion slot and runs the drop or the fly-back.
 *
 * Gestures are our own, not the platform drag and drop, so desktop, web and Android behave the
 * same: a touch drags after a short press, a mouse drags as soon as it moves.
 */
class DragController(private val state: GameState, private val scope: CoroutineScope) {
    var root: LayoutCoordinates? = null
    val cards = mutableStateMapOf<Slot, Rect>()
    val containers = mutableStateMapOf<Container, Rect>()

    var drag by mutableStateOf<DragState?>(null)
        private set

    /** Card flying back to where it was picked up after a drop outside. */
    var flyBack by mutableStateOf<Pair<Command, Offset>?>(null)
        private set
    private val flyBackSize = mutableStateOf(Size.Zero)
    val flyBackCardSize: Size get() = flyBackSize.value

    fun toRoot(coords: LayoutCoordinates): Rect? {
        val r = root ?: return null
        return Rect(r.localPositionOf(coords, Offset.Zero), coords.size.toSize())
    }

    fun start(command: Command, source: Slot?, rect: Rect, pointer: Offset) {
        if (!state.canEdit) return
        if (source == null && state.slotsUsed >= state.level.maxSlots) return
        drag = DragState(command, source, rect.size, pointer - rect.topLeft, pointer, null, false).withTarget()
    }

    fun move(delta: Offset) {
        val d = drag ?: return
        drag = d.copy(pointer = d.pointer + delta).withTarget()
    }

    fun end() {
        val d = drag ?: return
        drag = null
        val done = d.target != null && d.valid && (
            if (d.source == null) state.insertCommand(d.command, d.target) else state.moveCommand(d.source, d.target)
            )
        if (!done) {
            // fly back to where the card came from (tray cards fade out where they are)
            val home = d.source?.let { cards[it]?.topLeft } ?: d.topLeft
            flyBackSize.value = d.size
            flyBack = d.command to d.topLeft
            scope.launch {
                val anim = Animatable(d.topLeft, Offset.VectorConverter)
                anim.animateTo(home, tween(220)) { flyBack = d.command to value }
                flyBack = null
            }
        }
    }

    fun cancel() {
        drag = null
        flyBack = null
    }

    private fun DragState.withTarget(): DragState {
        val t = dropTarget(pointer, command, source)
        return copy(target = t, valid = t != null && state.canPlace(command, t) && !(source is Slot.Top && t is Slot.InLoop && t.loop == source.index))
    }

    /** Innermost container under [p] that may take [c], and the insertion index inside it. */
    private fun dropTarget(p: Offset, c: Command, source: Slot?): Slot? {
        if (c !is Command.Repeat && c != Command.Call) {
            containers[Container.Function]?.takeIf { it.contains(p) }?.let { return Slot.InFunction(indexIn(functionCards(), p)) }
        }
        if (c !is Command.Repeat) {
            for ((k, r) in containers) {
                if (k is Container.Loop && r.contains(p) && !(source is Slot.Top && source.index == k.index)) {
                    return Slot.InLoop(k.index, indexIn(loopCards(k.index), p))
                }
            }
        }
        val top = containers[Container.Top] ?: return null
        if (!top.inflate(12f).contains(p)) return null
        return Slot.Top(indexIn(topCards(), p))
    }

    fun topCards(): List<Rect> = cards.entries.filter { it.key is Slot.Top }.sortedBy { (it.key as Slot.Top).index }.map { it.value }
    fun loopCards(loop: Int): List<Rect> = cards.entries.filter { (it.key as? Slot.InLoop)?.loop == loop }.sortedBy { (it.key as Slot.InLoop).index }.map { it.value }
    fun functionCards(): List<Rect> = cards.entries.filter { it.key is Slot.InFunction }.sortedBy { (it.key as Slot.InFunction).index }.map { it.value }

    /** Insertion index among [rects] (in index order, laid out in rows) for pointer [p]. */
    private fun indexIn(rects: List<Rect>, p: Offset): Int {
        if (rects.isEmpty()) return 0
        // group into rows by top edge
        val rows = ArrayList<MutableList<IndexedValue<Rect>>>()
        for (iv in rects.withIndex()) {
            val row = rows.lastOrNull()
            if (row != null && kotlin.math.abs(row.first().value.top - iv.value.top) < iv.value.height / 2) row += iv else rows += mutableListOf(iv)
        }
        val row = rows.minByOrNull { r -> val c = r.first().value; kotlin.math.abs(c.center.y - p.y) }!!
        for ((i, r) in row) if (p.x < r.center.x) return i
        return row.last().index + 1
    }
}

/**
 * Makes [content] draggable into the program. [source] is null for tray cards. Taps are left
 * alone so a clickable on the same card keeps working; once a drag starts the events are consumed.
 */
fun Modifier.dragSource(
    controller: DragController,
    command: () -> Command,
    source: Slot?,
    enabled: () -> Boolean,
    onBounds: ((Rect) -> Unit)? = null,
): Modifier {
    var bounds: Rect? = null
    return this
        .onGloballyPositioned { coords ->
            controller.toRoot(coords)?.let { bounds = it; onBounds?.invoke(it) }
        }
        .pointerInput(controller, source) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!enabled()) return@awaitEachGesture
                val rect = bounds ?: return@awaitEachGesture
                // Mouse: drag as soon as the pointer moves. Touch: hold briefly first so taps and scrolls stay taps.
                val start = if (down.type == PointerType.Mouse) {
                    awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                } else {
                    awaitLongPressOrCancellation(down.id)
                } ?: return@awaitEachGesture
                controller.start(command(), source, rect, rect.topLeft + start.position)
                if (controller.drag == null) return@awaitEachGesture
                var last = start.position
                drag(start.id) { change ->
                    controller.move(change.position - last)
                    last = change.position
                    change.consume()
                }
                currentEvent.changes.forEach { it.consume() }
                controller.end()
            }
        }
}
