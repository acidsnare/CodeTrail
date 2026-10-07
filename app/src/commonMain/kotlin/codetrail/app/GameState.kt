package codetrail.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import codetrail.core.command.Command
import codetrail.core.command.CommandSet
import codetrail.core.command.Program
import codetrail.core.command.slotCount
import codetrail.core.engine.CommandRef
import codetrail.core.engine.PredictPuzzle
import codetrail.core.engine.PredictPuzzles
import codetrail.core.engine.Solver
import codetrail.core.model.Pos
import codetrail.app.sound.Sfx
import codetrail.app.sound.SoundPlayer
import codetrail.core.engine.Failure
import codetrail.core.engine.HeroState
import codetrail.core.engine.Interpreter
import codetrail.core.engine.Step
import codetrail.core.gen.LevelGenerator
import codetrail.core.model.Level
import codetrail.core.progress.Profile
import codetrail.core.progress.SaveSlot
import codetrail.app.theme.Character
import codetrail.app.theme.WorldTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.random.Random

enum class Phase { EDITING, RUNNING, WON, FAILED }

/** A card position: top level of the program, inside a loop body, or inside block A. */
sealed interface Slot {
    data class Top(val index: Int) : Slot
    data class InLoop(val loop: Int, val index: Int) : Slot
    data class InFunction(val index: Int) : Slot
}

/** What the player has selected: a card, a loop frame, or the block A frame. */
sealed interface Selection {
    data class Card(val slot: Slot) : Selection
    data class Loop(val index: Int) : Selection
    data object Function : Selection
}

enum class GameMode(val id: String) { FORWARD("forward"), PREDICT("predict");
    companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } ?: FORWARD }
}

/**
 * Where and how the hero sprite is drawn, in fractional cell coordinates for smooth motion.
 * [lift] raises the sprite above its shadow (jumps), [scale]/[alpha] shrink and fade it (sinking),
 * [tilt] rocks it in degrees (dizzy, head shake).
 */
data class HeroVisual(
    val x: Float,
    val y: Float,
    val dirDegrees: Float,
    val lift: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val tilt: Float = 0f,
) {
    companion object {
        fun of(s: HeroState) = HeroVisual(s.pos.x.toFloat(), s.pos.y.toFloat(), s.dir.ordinal * 90f)
    }
}

/**
 * One play session for one profile. Every change that matters is pushed back through
 * [onProfileChanged] so the profile is always an up-to-date save slot.
 */
class GameState(
    profile: Profile,
    val theme: WorldTheme,
    tier: Int,
    resume: SaveSlot? = null,
    val mode: GameMode = GameMode.FORWARD,
    private val sounds: SoundPlayer = SoundPlayer.Silent,
    /** Animation time scale, read on every tick so a settings change applies at once. */
    private val speed: () -> Float = { 1f },
    private val onProfileChanged: (Profile) -> Unit = {},
) {
    private var stepToggle = false

    private val generator = LevelGenerator()

    var profile by mutableStateOf(profile)
        private set

    var tier by mutableStateOf(tier)
        private set
    var level by mutableStateOf(generator.generate(tier, resume?.seed ?: Random.nextLong(), obstacles = mode == GameMode.FORWARD))
        private set

    // ---- predict mode ----
    var predict by mutableStateOf<PredictPuzzle?>(null)
        private set
    var guess by mutableStateOf<Pos?>(null)
        private set
    var predictAttempts by mutableStateOf(0)
        private set
    var predictResult by mutableStateOf<Boolean?>(null)
        private set

    // ---- hints ----
    private var solution: Solver.Solution? = null
    /** Hints taken on this level. Each one costs a star, the level never drops below one. */
    var hintsUsed by mutableStateOf(0)
        private set

    /** Best rating still reachable after the hints taken so far. */
    val maxStars: Int get() = (3 - hintsUsed).coerceAtLeast(1)
    /** Command the palette should pulse, if the hint says "add this next". */
    var hintCommand by mutableStateOf<Command?>(null)
        private set
    /** Hint says the last card is wrong. */
    var hintRemoveLast by mutableStateOf(false)
        private set
    /** Hint says the last card of block A is wrong. */
    var hintRemoveLastFunction by mutableStateOf(false)
        private set
    /** Hint says the program is already complete. */
    var hintReady by mutableStateOf(false)
        private set
    val program = mutableStateListOf<Command>()

    /** Block A: the reusable group of cards on the block tier. */
    val function = mutableStateListOf<Command>()

    /** Current selection: shows the delete badge and decides where a tapped tray card goes. */
    var selection by mutableStateOf<Selection?>(null)
        private set

    /** Snapshots of (program, block A) for Undo, newest last. */
    private val history = ArrayDeque<Pair<List<Command>, List<Command>>>()
    var canUndo by mutableStateOf(false)
        private set

    /** Slots in use: program, loop bodies and block A together. */
    val slotsUsed: Int get() = program.slotCount() + function.size

    var phase by mutableStateOf(Phase.EDITING)
        private set
    var hero by mutableStateOf(HeroVisual.of(HeroState(level.start, level.startDir)))
        private set
    var activeCommand by mutableStateOf<CommandRef?>(null)
        private set
    var failure by mutableStateOf<Failure?>(null)
        private set
    var stars by mutableStateOf(0)
        private set

    /** Stars actually added to the profile by the last win: rating x difficulty tier. */
    var payout by mutableStateOf(0)
        private set

    /** Character unlocked by the most recent win, shown once in the status line. */
    var justUnlocked by mutableStateOf<Character?>(null)
        private set

    /** Full-screen unlock splash is showing until the player dismisses it. */
    var unlockSplash by mutableStateOf(false)
        private set

    /** Bumps on every finished run so one-shot effects (confetti, splash, shake) can key on it. */
    var runId by mutableStateOf(0)
        private set

    var forwardCount by mutableStateOf(1)
    var repeatCount by mutableStateOf(2)

    val character: Character
        get() = Character.byId(profile.characterId)?.takeIf { isUnlocked(it) } ?: Character.All.first()

    /** A level is locked once it has been solved, in this session or earlier: no edits, no reruns, only New level. */
    val completed: Boolean
        get() = phase == Phase.WON || profile.progress.isRewarded(level.difficulty, rewardKey())

    val canEdit: Boolean get() = phase != Phase.RUNNING && mode == GameMode.FORWARD && !completed

    private fun rewardKey() = if (mode == GameMode.PREDICT) level.seed xor 0x5EED else level.seed

    init {
        if (mode == GameMode.PREDICT) {
            setupPredict()
        } else {
            resume?.function?.filter { level.commandSet.acceptsInFunction(it) }?.let { function.addAll(it) }
            resume?.program?.filter { level.commandSet.accepts(it) }?.let { program.addAll(it) }
            while (slotsUsed > level.maxSlots && program.isNotEmpty()) program.removeAt(program.lastIndex)
        }
        persist()
    }

    private fun setupPredict() {
        val puzzle = PredictPuzzles.make(level)
        predict = puzzle
        program.clear()
        program.addAll(puzzle.program)
        function.clear()
        function.addAll(puzzle.function)
        guess = null
        predictAttempts = 0
        predictResult = null
    }

    /** Predict mode: the player points at the cell where the hero will stop. */
    fun selectGuess(p: Pos) {
        if (mode != GameMode.PREDICT || phase == Phase.RUNNING || completed) return
        if (!level.grid.contains(p)) return
        if (phase == Phase.FAILED) { resetRun(); predictResult = null }
        guess = p
        sounds.play(Sfx.GUESS)
    }

    /** Hint: compare the program with the shortest solution and point at the next step. Caps the level at 2 stars. */
    fun hint() {
        if (mode != GameMode.FORWARD || !canEdit) return
        val full = solution ?: Solver.solve(level.grid, level.start, level.startDir, level.goal, level.commandSet)?.also { solution = it } ?: return
        hintsUsed++
        clearHint()
        sounds.play(Sfx.HINT)
        // Block A first: the program only makes sense once the block it calls is right.
        val fn = function.toList()
        if (fn != full.function) {
            if (fn.size < full.function.size && fn == full.function.take(fn.size)) {
                hintCommand = full.function[fn.size]
                selection = Selection.Function
            } else {
                hintRemoveLastFunction = true
            }
            return
        }
        val sol = full.program
        val current = program.toList()
        val last = current.lastOrNull()
        val expected = sol.getOrNull(current.lastIndex)
        when {
            current.size <= sol.size && current == sol.take(current.size) -> {
                if (current.size == sol.size) {
                    hintReady = true
                } else {
                    val next = sol[current.size]
                    // A loop is suggested empty: first place the loop card, then fill it.
                    hintCommand = if (next is Command.Repeat) Command.Repeat(next.times, emptyList()) else next
                    if (next is Command.Repeat) repeatCount = next.times
                    selection = null
                }
            }
            // Inside a half-built loop that matches the solution so far: point at the next body card.
            last is Command.Repeat && expected is Command.Repeat && last.times == expected.times &&
                current.dropLast(1) == sol.take(current.size - 1) &&
                last.body.size < expected.body.size && last.body == expected.body.take(last.body.size) -> {
                hintCommand = expected.body[last.body.size]
                selection = Selection.Loop(current.lastIndex)
            }
            else -> hintRemoveLast = true
        }
    }

    private fun clearHint() {
        hintCommand = null
        hintRemoveLast = false
        hintRemoveLastFunction = false
        hintReady = false
    }

    fun isUnlocked(c: Character) = profile.progress.totalStars >= c.unlockStars

    fun newLevel(newTier: Int = tier, seed: Long = Random.nextLong()) {
        tier = newTier
        level = generator.generate(newTier, seed, obstacles = mode == GameMode.FORWARD)
        solution = null
        hintsUsed = 0
        clearHint()
        program.clear()
        function.clear()
        selection = null
        history.clear()
        canUndo = false
        resetRun()
        if (mode == GameMode.PREDICT) setupPredict()
        justUnlocked = null
        persist()
    }

    /** Same level, empty program. */
    fun restartLevel() {
        if (completed) { newLevel(); return }
        if (mode == GameMode.PREDICT) { resetRun(); setupPredict(); persist(); return }
        remember()
        program.clear()
        function.clear()
        selection = null
        clearHint()
        resetRun()
        persist()
    }

    // ---- editing -------------------------------------------------------------------------

    /** Snapshot before a change so Undo can bring it back. */
    private fun remember() {
        history.addLast(program.toList() to function.toList())
        while (history.size > 50) history.removeFirst()
        canUndo = true
    }

    private fun afterEdit(sfx: Sfx?) {
        clearHint()
        sfx?.let { sounds.play(it) }
        persist()
    }

    fun undo() {
        if (!canEdit || history.isEmpty()) return
        val (p, f) = history.removeLast()
        canUndo = history.isNotEmpty()
        if (phase != Phase.EDITING) resetRun()
        program.clear(); program.addAll(p)
        function.clear(); function.addAll(f)
        selection = null
        afterEdit(Sfx.CARD_REMOVE)
    }

    /** The card at [slot], or null when the slot no longer exists. */
    fun commandAt(slot: Slot): Command? = when (slot) {
        is Slot.Top -> program.getOrNull(slot.index)
        is Slot.InLoop -> (program.getOrNull(slot.loop) as? Command.Repeat)?.body?.getOrNull(slot.index)
        is Slot.InFunction -> function.getOrNull(slot.index)
    }

    /** Whether [c] may live at [slot]'s container: loops only at the top, block A takes plain cards only. */
    fun canPlace(c: Command, slot: Slot): Boolean = when (slot) {
        is Slot.Top -> level.commandSet.accepts(c)
        is Slot.InLoop -> c !is Command.Repeat && level.commandSet.accepts(c) && program.getOrNull(slot.loop) is Command.Repeat
        is Slot.InFunction -> level.commandSet.acceptsInFunction(c)
    }

    /** Where a tapped tray card goes: right after the selected card, into the selected frame, or at the end. */
    private fun insertionPoint(c: Command): Slot = when (val sel = selection) {
        is Selection.Card -> {
            val next = when (val at = sel.slot) {
                is Slot.Top -> Slot.Top(at.index + 1)
                is Slot.InLoop -> Slot.InLoop(at.loop, at.index + 1)
                is Slot.InFunction -> Slot.InFunction(at.index + 1)
            }
            if (canPlace(c, next)) next else Slot.Top(program.size)
        }
        is Selection.Loop -> {
            val body = (program.getOrNull(sel.index) as? Command.Repeat)?.body
            if (body != null && canPlace(c, Slot.InLoop(sel.index, 0))) Slot.InLoop(sel.index, body.size) else Slot.Top(program.size)
        }
        Selection.Function -> if (canPlace(c, Slot.InFunction(0))) Slot.InFunction(function.size) else Slot.Top(program.size)
        null -> Slot.Top(program.size)
    }

    /** Tap on a tray card. */
    fun addCommand(c: Command) {
        if (!canEdit || slotsUsed >= level.maxSlots) return
        val at = insertionPoint(c)
        if (!canPlace(c, at)) return
        insert(c, at)
        selection = if (c is Command.Repeat) Selection.Loop(at.asTop()) else Selection.Card(at)
    }

    /** Drop of a tray card at [at]. Returns false when nothing was inserted. */
    fun insertCommand(c: Command, at: Slot): Boolean {
        if (!canEdit || slotsUsed >= level.maxSlots || !canPlace(c, at)) return false
        insert(c, at)
        selection = null
        return true
    }

    private fun Slot.asTop(): Int = (this as Slot.Top).index

    private fun insert(c: Command, at: Slot) {
        if (phase != Phase.EDITING) resetRun()
        remember()
        when (at) {
            is Slot.Top -> program.add(at.index.coerceIn(0, program.size), c)
            is Slot.InLoop -> {
                val loop = program[at.loop] as Command.Repeat
                program[at.loop] = loop.copy(body = loop.body.toMutableList().apply { add(at.index.coerceIn(0, size), c) })
            }
            is Slot.InFunction -> function.add(at.index.coerceIn(0, function.size), c)
        }
        afterEdit(Sfx.CARD_ADD)
    }

    /**
     * Moves the card at [from] so that it ends up at [to] (an insertion point computed while the
     * card is still in place). Returns false when the move is not allowed; nothing changes then.
     */
    fun moveCommand(from: Slot, to: Slot): Boolean {
        if (!canEdit) return false
        val c = commandAt(from) ?: return false
        if (!canPlace(c, to)) return false
        // Dropping a card back onto itself or right after itself is a no-op.
        if (from == to) return true
        if (sameContainer(from, to) && indexOf(to) == indexOf(from) + 1) return true
        // A loop cannot be dropped inside its own body.
        if (from is Slot.Top && to is Slot.InLoop && to.loop == from.index) return false
        if (phase != Phase.EDITING) resetRun()
        remember()
        removeAt(from)
        insertRaw(c, adjustAfterRemoval(to, from))
        selection = null
        afterEdit(Sfx.CARD_ADD)
        return true
    }

    private fun sameContainer(a: Slot, b: Slot) = when (a) {
        is Slot.Top -> b is Slot.Top
        is Slot.InLoop -> b is Slot.InLoop && b.loop == a.loop
        is Slot.InFunction -> b is Slot.InFunction
    }

    private fun indexOf(s: Slot) = when (s) { is Slot.Top -> s.index; is Slot.InLoop -> s.index; is Slot.InFunction -> s.index }

    /** Removing [from] shifts later indices in the same container, and loop indices when a top card goes. */
    private fun adjustAfterRemoval(to: Slot, from: Slot): Slot = when {
        sameContainer(to, from) && indexOf(to) > indexOf(from) -> when (to) {
            is Slot.Top -> Slot.Top(to.index - 1)
            is Slot.InLoop -> Slot.InLoop(to.loop, to.index - 1)
            is Slot.InFunction -> Slot.InFunction(to.index - 1)
        }
        from is Slot.Top && to is Slot.InLoop && to.loop > from.index -> Slot.InLoop(to.loop - 1, to.index)
        else -> to
    }

    private fun removeAt(slot: Slot) {
        when (slot) {
            is Slot.Top -> program.removeAt(slot.index)
            is Slot.InLoop -> {
                val loop = program[slot.loop] as Command.Repeat
                program[slot.loop] = loop.copy(body = loop.body.filterIndexed { i, _ -> i != slot.index })
            }
            is Slot.InFunction -> function.removeAt(slot.index)
        }
    }

    private fun insertRaw(c: Command, at: Slot) {
        when (at) {
            is Slot.Top -> program.add(at.index.coerceIn(0, program.size), c)
            is Slot.InLoop -> {
                val loop = program[at.loop] as Command.Repeat
                program[at.loop] = loop.copy(body = loop.body.toMutableList().apply { add(at.index.coerceIn(0, size), c) })
            }
            is Slot.InFunction -> function.add(at.index.coerceIn(0, function.size), c)
        }
    }

    /** Tap on a card in the program: select it, tap again to deselect. */
    fun select(sel: Selection?) {
        if (!canEdit) return
        selection = if (selection == sel) null else sel
        if (sel != null) sounds.play(Sfx.CLICK)
    }

    fun clearSelection() {
        selection = null
    }

    /** The delete badge on the selected card, or the Delete key. */
    fun removeSelected() {
        when (val sel = selection) {
            is Selection.Card -> removeCommand(sel.slot)
            is Selection.Loop -> removeCommand(Slot.Top(sel.index))
            else -> return
        }
    }

    fun removeCommand(slot: Slot) {
        if (!canEdit || commandAt(slot) == null) return
        if (phase != Phase.EDITING) resetRun()
        remember()
        removeAt(slot)
        selection = null
        afterEdit(Sfx.CARD_REMOVE)
    }

    /** The -/+ on a selected "forward N" or "repeat xN" card. */
    fun adjustSelected(delta: Int) {
        val slot = (selection as? Selection.Card)?.slot ?: (selection as? Selection.Loop)?.let { Slot.Top(it.index) } ?: return
        val c = commandAt(slot) ?: return
        val maxRun = minOf(maxOf(level.grid.width, level.grid.height) - 1, level.commandSet.maxForward)
        val replaced = when (c) {
            is Command.Forward -> Command.Forward((c.cells + delta).coerceIn(1, maxRun))
            is Command.Repeat -> Command.Repeat((c.times + delta).coerceIn(2, CommandSet.MAX_REPEAT), c.body)
            else -> return
        }
        if (replaced == c || !canEdit) return
        if (phase != Phase.EDITING) resetRun()
        remember()
        when (slot) {
            is Slot.Top -> program[slot.index] = replaced
            is Slot.InLoop -> {
                val loop = program[slot.loop] as Command.Repeat
                program[slot.loop] = loop.copy(body = loop.body.toMutableList().apply { set(slot.index, replaced) })
            }
            is Slot.InFunction -> function[slot.index] = replaced
        }
        afterEdit(Sfx.CLICK)
    }

    fun clearProgram() {
        if (!canEdit || (program.isEmpty() && function.isEmpty())) return
        remember()
        program.clear()
        function.clear()
        selection = null
        resetRun()
        afterEdit(Sfx.CARD_REMOVE)
    }

    fun resetRun() {
        payout = 0
        phase = Phase.EDITING
        hero = HeroVisual.of(HeroState(level.start, level.startDir))
        activeCommand = null
        failure = null
        stars = 0
    }

    /** Plays the trace step by step. Suspends until the run is over. */
    suspend fun run() {
        if (phase == Phase.RUNNING || program.isEmpty() || completed) return
        if (mode == GameMode.PREDICT && guess == null) return
        resetRun()
        selection = null
        phase = Phase.RUNNING
        val trace = Interpreter.run(level.copy(maxSlots = Int.MAX_VALUE), program.toList(), function.toList())

        for (step in trace.steps) {
            activeCommand = step.at
            animate(step)
            tick(STEP_PAUSE_MS)
        }

        if (mode == GameMode.PREDICT) {
            finishPredict()
            return
        }

        val f = trace.failure
        if (f == null) {
            runId++
            phase = Phase.WON
            stars = minOf(rate(slotsUsed, level.optimalLength), maxStars)
            activeCommand = null
            award(stars)
            sounds.play(if (stars == 3) Sfx.WIN_BIG else Sfx.WIN)
            victoryHop()
            if (justUnlocked != null) { unlockSplash = true; sounds.play(Sfx.UNLOCK) }
        } else {
            phase = Phase.FAILED
            failure = f
            activeCommand = trace.failedAt
            playFailure(f)
        }
    }

    /**
     * Every failure gets a readable little scene so the run never looks frozen:
     * fall in -> splash -> sink -> pop back at the start; bump -> recoil and wobble; and so on.
     * [runId] is bumped at the moment of impact so board effects (splash, shake) line up.
     */
    private suspend fun playFailure(f: Failure) {
        when (f) {
            is Failure.Fell -> {
                val (tx, ty) = fallTarget(f.at.x.toFloat(), f.at.y.toFloat())
                walkTo(tx, ty, frames = 12)
                runId++
                sounds.play(Sfx.SPLASH)
                sinkAndRespawn()
            }
            is Failure.BadLanding -> {
                val (tx, ty) = fallTarget(f.at.x.toFloat(), f.at.y.toFloat())
                hopTo(tx, ty, frames = 18)
                runId++
                sounds.play(Sfx.SPLASH)
                sinkAndRespawn()
            }
            is Failure.Bumped -> {
                val from = hero
                val dx = (f.into.x - from.x) * 0.3f
                val dy = (f.into.y - from.y) * 0.3f
                walkTo(from.x + dx, from.y + dy, frames = 5)
                runId++
                sounds.play(Sfx.BUMP)
                // recoil back with a dizzy wobble
                for (i in 1..22) {
                    val t = i / 22f
                    val back = 1f - easeOut(minOf(1f, t * 2f))
                    val wobble = (kotlin.math.sin(t * 5f * PI).toFloat()) * 14f * (1f - t)
                    hero = from.copy(x = from.x + dx * back, y = from.y + dy * back, tilt = wobble)
                    tick(FRAME_MS)
                }
                hero = from
            }
            is Failure.NotAtGoal -> {
                runId++
                sounds.play(Sfx.SAD)
                headShake()
            }
            else -> runId++
        }
    }

    /** Falling off the board: stop half a cell past the edge so the hero sinks in view. */
    private fun fallTarget(x: Float, y: Float): Pair<Float, Float> {
        val maxX = level.grid.width - 1f
        val maxY = level.grid.height - 1f
        return x.coerceIn(-0.45f, maxX + 0.45f) to y.coerceIn(-0.45f, maxY + 0.45f)
    }

    private suspend fun walkTo(x: Float, y: Float, frames: Int) {
        val from = hero
        for (i in 1..frames) {
            val e = easeInOut(i / frames.toFloat())
            hero = from.copy(x = lerp(from.x, x, e), y = lerp(from.y, y, e))
            tick(FRAME_MS)
        }
    }

    private suspend fun hopTo(x: Float, y: Float, frames: Int) {
        val from = hero
        for (i in 1..frames) {
            val t = i / frames.toFloat()
            val e = easeInOut(t)
            hero = from.copy(x = lerp(from.x, x, e), y = lerp(from.y, y, e), lift = (1f - (2 * t - 1f) * (2 * t - 1f)) * 0.6f)
            tick(FRAME_MS)
        }
        hero = from.copy(x = x, y = y, lift = 0f)
    }

    /** Shrink and fade into the water, wait a beat, then pop back in at the start tile. */
    private suspend fun sinkAndRespawn() {
        val from = hero
        for (i in 1..22) {
            val t = i / 22f
            hero = from.copy(scale = 1f - 0.75f * t, alpha = 1f - t, lift = -0.25f * t, tilt = 25f * t)
            tick(FRAME_MS)
        }
        tick(350)
        val home = HeroVisual.of(HeroState(level.start, level.startDir))
        for (i in 1..16) {
            val t = i / 16f
            val pop = overshoot(t)
            hero = home.copy(scale = pop, alpha = minOf(1f, t * 2f), lift = (1f - t) * 0.5f)
            tick(FRAME_MS)
        }
        hero = home
    }

    /** "No..." - rock left and right three times. */
    private suspend fun headShake() {
        val from = hero
        for (i in 1..30) {
            val t = i / 30f
            hero = from.copy(tilt = (kotlin.math.sin(t * 6f * PI).toFloat()) * 12f * (1f - t * 0.5f))
            tick(FRAME_MS)
        }
        hero = from
    }

    /** Predict mode outcome: the hero has walked the program, now compare with the guess. */
    private suspend fun finishPredict() {
        val answer = predict?.answer ?: return
        predictAttempts++
        runId++
        if (guess == answer) {
            predictResult = true
            phase = Phase.WON
            stars = when (predictAttempts) { 1 -> 3; 2 -> 2; else -> 1 }
            activeCommand = null
            award(stars)
            sounds.play(if (stars == 3) Sfx.WIN_BIG else Sfx.WIN)
            victoryHop()
            if (justUnlocked != null) { unlockSplash = true; sounds.play(Sfx.UNLOCK) }
        } else {
            predictResult = false
            phase = Phase.FAILED
            sounds.play(Sfx.SAD)
            headShake()
        }
    }

    fun dismissUnlock() { unlockSplash = false }

    /** Two happy bounces on the goal tile. */
    private suspend fun victoryHop() {
        val base = hero
        repeat(2) {
            for (i in 1..14) {
                val t = i / 14f
                hero = base.copy(lift = (1f - (2 * t - 1f) * (2 * t - 1f)) * 0.35f)
                tick(FRAME_MS)
            }
        }
        hero = base
    }

    /** Pays out stars once per level and reports a character that just crossed its threshold. */
    private fun award(stars: Int) {
        val progress = profile.progress
        // predict puzzles are rewarded separately from building the same level
        val seedKey = rewardKey()
        payout = 0
        if (!progress.isRewarded(level.difficulty, seedKey)) {
            // harder tiers pay more: the 1-3 rating is multiplied by the tier number
            payout = stars * level.difficulty
            val before = progress.totalStars
            val after = progress.reward(level.difficulty, seedKey, payout, theme.id)
            profile = profile.copy(progress = after)
            justUnlocked = Character.All.firstOrNull { it.unlockStars in (before + 1)..after.totalStars }
        }
        persist()
    }

    /** Writes the profile back: progress, choices, and the level in progress (none once it is won). */
    fun persist() {
        val forward = mode == GameMode.FORWARD
        val slot = if (phase == Phase.WON) null else SaveSlot(tier, level.seed, theme.id, if (forward) program.toList() else emptyList(), mode.id, if (forward) function.toList() else emptyList())
        profile = profile.copy(
            lastPlayedAt = Platform.currentTimeMillis(),
            worldId = theme.id,
            tier = tier,
            autosave = slot,
        )
        onProfileChanged(profile)
    }

    private suspend fun animate(step: Step) {
        when (step) {
            is Step.Walk -> { sounds.play(if (stepToggle) Sfx.STEP_A else Sfx.STEP_B); stepToggle = !stepToggle }
            is Step.Rotate -> sounds.play(Sfx.TURN)
            is Step.Hop -> sounds.play(Sfx.HOP)
        }
        val from = HeroVisual.of(step.before)
        val to = HeroVisual.of(step.after)
        val frames = when (step) {
            is Step.Rotate -> 10
            is Step.Walk -> 14
            is Step.Hop -> 18
        }
        val toDeg = shortestTurn(from.dirDegrees, to.dirDegrees)
        for (i in 1..frames) {
            val t = i / frames.toFloat()
            val e = easeInOut(t)
            val lift = if (step is Step.Hop) (1f - (2 * t - 1f) * (2 * t - 1f)) * 0.6f else 0f
            hero = HeroVisual(
                x = lerp(from.x, to.x, e),
                y = lerp(from.y, to.y, e),
                dirDegrees = lerp(from.dirDegrees, toDeg, e),
                lift = lift,
            )
            tick(FRAME_MS)
        }
        hero = to
    }

    /** A delay scaled by the animation speed setting. */
    private suspend fun tick(ms: Long) = delay((ms * speed()).toLong().coerceAtLeast(1L))

    private fun rate(used: Int, optimal: Int): Int = when {
        used <= optimal -> 3
        used <= optimal + 2 -> 2
        else -> 1
    }

    companion object {
        private const val FRAME_MS = 16L
        private const val STEP_PAUSE_MS = 80L

        private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        private fun easeInOut(t: Float) = if (t < 0.5f) 2 * t * t else 1 - (-2 * t + 2) * (-2 * t + 2) / 2
        private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t)
        private fun overshoot(t: Float): Float { val s = 1.6f; val x = t - 1f; return x * x * ((s + 1) * x + s) + 1f }

        /** Pick the equivalent target angle closest to [from] so 270 -> 0 rotates 90, not 270. */
        private fun shortestTurn(from: Float, to: Float): Float {
            var d = (to - from) % 360f
            if (d > 180f) d -= 360f
            if (d < -180f) d += 360f
            return from + d
        }
    }
}
