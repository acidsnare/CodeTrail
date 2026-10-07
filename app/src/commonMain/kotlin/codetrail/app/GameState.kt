package codetrail.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import codetrail.core.command.Command
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

/** Where a card from the tray goes: the program, inside a loop, or into block A. */
sealed interface EditTarget {
    data object Main : EditTarget
    data class Loop(val index: Int) : EditTarget
    data object Function : EditTarget
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

    /** Where new cards go. */
    var target by mutableStateOf<EditTarget>(EditTarget.Main)
        private set

    /** Top-level index of the loop that receives new cards, or null. */
    val openLoop: Int? get() = (target as? EditTarget.Loop)?.index

    val editingFunction: Boolean get() = target == EditTarget.Function

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
                target = EditTarget.Function
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
                    target = EditTarget.Main
                }
            }
            // Inside a half-built loop that matches the solution so far: point at the next body card.
            last is Command.Repeat && expected is Command.Repeat && last.times == expected.times &&
                current.dropLast(1) == sol.take(current.size - 1) &&
                last.body.size < expected.body.size && last.body == expected.body.take(last.body.size) -> {
                hintCommand = expected.body[last.body.size]
                target = EditTarget.Loop(current.lastIndex)
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
        target = EditTarget.Main
        resetRun()
        if (mode == GameMode.PREDICT) setupPredict()
        justUnlocked = null
        persist()
    }

    /** Same level, empty program. */
    fun restartLevel() {
        if (completed) { newLevel(); return }
        if (mode == GameMode.PREDICT) { resetRun(); setupPredict(); persist(); return }
        program.clear()
        function.clear()
        target = EditTarget.Main
        clearHint()
        resetRun()
        persist()
    }

    /** Adds a card where the focus is: the program, the open loop, or block A. */
    fun addCommand(c: Command) {
        if (!canEdit || slotsUsed >= level.maxSlots) return
        if (phase != Phase.EDITING) resetRun()
        when (val t = target) {
            EditTarget.Function -> {
                if (!level.commandSet.acceptsInFunction(c)) return
                function += c
            }
            is EditTarget.Loop -> {
                val loop = program.getOrNull(t.index) as? Command.Repeat
                when {
                    c is Command.Repeat -> { program += c; target = EditTarget.Loop(program.lastIndex) }
                    loop != null -> program[t.index] = loop.copy(body = loop.body + c)
                    else -> { program += c; target = EditTarget.Main }
                }
            }
            EditTarget.Main -> {
                program += c
                if (c is Command.Repeat) target = EditTarget.Loop(program.lastIndex)
            }
        }
        clearHint()
        sounds.play(Sfx.CARD_ADD)
        persist()
    }

    /** Tapping anywhere on a loop focuses it: new cards go inside until focus is lost. */
    fun focusLoop(index: Int) {
        if (!canEdit || program.getOrNull(index) !is Command.Repeat || openLoop == index) return
        target = EditTarget.Loop(index)
        sounds.play(Sfx.CLICK)
    }

    /** Tapping block A focuses it: new cards go into the block. */
    fun focusFunction() {
        if (!canEdit || !level.commandSet.functions || editingFunction) return
        target = EditTarget.Function
        sounds.play(Sfx.CLICK)
    }

    /** Tapping outside drops the focus; new cards go to the program again. */
    fun closeLoop() {
        target = EditTarget.Main
    }

    /** Removes a top-level card (a loop goes with its body) or one card inside a loop. */
    fun removeCommand(index: Int, bodyIndex: Int? = null) {
        if (!canEdit || index !in program.indices) return
        if (phase != Phase.EDITING) resetRun()
        val loop = program[index] as? Command.Repeat
        if (bodyIndex != null && loop != null && bodyIndex in loop.body.indices) {
            program[index] = loop.copy(body = loop.body.filterIndexed { i, _ -> i != bodyIndex })
        } else {
            program.removeAt(index)
            val open = openLoop
            target = when {
                open == null -> target
                open == index -> EditTarget.Main
                open > index -> EditTarget.Loop(open - 1)
                else -> target
            }
        }
        clearHint()
        sounds.play(Sfx.CARD_REMOVE)
        persist()
    }

    /** Removes one card of block A. */
    fun removeFunctionCommand(index: Int) {
        if (!canEdit || index !in function.indices) return
        if (phase != Phase.EDITING) resetRun()
        function.removeAt(index)
        clearHint()
        sounds.play(Sfx.CARD_REMOVE)
        persist()
    }

    fun clearProgram() {
        if (!canEdit) return
        program.clear()
        function.clear()
        target = EditTarget.Main
        resetRun()
        persist()
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
        target = EditTarget.Main
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
