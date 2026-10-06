package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.Program
import codetrail.core.command.Compiled
import codetrail.core.model.Cell
import codetrail.core.model.Dir
import codetrail.core.model.Level
import codetrail.core.model.Pos

data class HeroState(val pos: Pos, val dir: Dir)

/** Why a step did not complete. */
sealed interface Failure {
    /** Stepped onto BLOCKED terrain or off the grid. */
    data class Fell(val at: Pos) : Failure

    /** Walked into an OBSTACLE. The hero stays put. */
    data class Bumped(val into: Pos) : Failure

    /** Jump landed on BLOCKED, OBSTACLE or outside the grid. */
    data class BadLanding(val at: Pos) : Failure

    /** Program ran out before the goal was reached. */
    data class NotAtGoal(val endedAt: Pos) : Failure

    data class NotAllowed(val command: Command) : Failure

    data object TooManyCommands : Failure
}

/**
 * Which card is executing: a top-level [index], and inside a loop also the [bodyIndex]
 * and the 1-based [iteration] so the UI can show "2 / 3" on the loop badge.
 * While block A runs, [index]/[bodyIndex] point at the call card and [fnIndex] at the card inside the block.
 */
data class CommandRef(val index: Int, val bodyIndex: Int? = null, val iteration: Int? = null, val fnIndex: Int? = null)

/** One atomic motion the UI can animate. Several steps may come from one command. */
sealed interface Step {
    val at: CommandRef
    val before: HeroState
    val after: HeroState

    data class Walk(override val at: CommandRef, override val before: HeroState, override val after: HeroState) : Step
    data class Rotate(override val at: CommandRef, override val before: HeroState, override val after: HeroState) : Step
    data class Hop(override val at: CommandRef, override val before: HeroState, override val after: HeroState) : Step
}

data class Trace(
    val steps: List<Step>,
    val finalState: HeroState,
    val failure: Failure?,
    /** The command that failed, if any. */
    val failedAt: CommandRef?,
) {
    val succeeded: Boolean get() = failure == null
}

/**
 * Runs a program against a level. Pure function: no side effects, deterministic.
 *
 * The program must end on the goal. Passing through the goal and walking away does not count,
 * so "fewest commands" scoring stays meaningful.
 */
object Interpreter {

    fun run(level: Level, program: Program, function: Program = emptyList()): Trace {
        if (Compiled(program, function).slots > level.maxSlots) {
            val s = HeroState(level.start, level.startDir)
            return Trace(emptyList(), s, Failure.TooManyCommands, null)
        }
        val steps = mutableListOf<Step>()
        var state = HeroState(level.start, level.startDir)
        function.firstOrNull { !level.commandSet.acceptsInFunction(it) }?.let {
            return Trace(steps, state, Failure.NotAllowed(it), CommandRef(0, fnIndex = function.indexOf(it)))
        }

        /** Runs one card, or every card of block A for a call. */
        fun runCard(cmd: Command, at: CommandRef): Trace? {
            if (cmd == Command.Call) {
                for ((k, inner) in function.withIndex()) {
                    val inside = at.copy(fnIndex = k)
                    when (val r = execute(level, state, inner, inside, steps)) {
                        is Outcome.Ok -> state = r.state
                        is Outcome.Failed -> return Trace(steps, state, r.failure, inside)
                    }
                }
                return null
            }
            return when (val r = execute(level, state, cmd, at, steps)) {
                is Outcome.Ok -> { state = r.state; null }
                is Outcome.Failed -> Trace(steps, state, r.failure, at)
            }
        }

        for ((i, cmd) in program.withIndex()) {
            if (!level.commandSet.accepts(cmd)) {
                return Trace(steps, state, Failure.NotAllowed(cmd), CommandRef(i))
            }
            if (cmd is Command.Repeat) {
                for (iteration in 1..cmd.times) {
                    for ((j, inner) in cmd.body.withIndex()) {
                        runCard(inner, CommandRef(i, j, iteration))?.let { return it }
                    }
                }
            } else {
                runCard(cmd, CommandRef(i))?.let { return it }
            }
        }

        val failure = if (state.pos == level.goal) null else Failure.NotAtGoal(state.pos)
        return Trace(steps, state, failure, null)
    }

    private sealed interface Outcome {
        data class Ok(val state: HeroState) : Outcome
        data class Failed(val failure: Failure) : Outcome
    }

    private fun execute(level: Level, start: HeroState, cmd: Command, at: CommandRef, out: MutableList<Step>): Outcome {
        var state = start
        when (cmd) {
            is Command.Move -> {
                // Absolute move also turns the hero so sprites face the travel direction.
                val next = state.pos + cmd.dir
                val f = walkCheck(level, next)
                if (f != null) return Outcome.Failed(f)
                val after = HeroState(next, cmd.dir)
                out += Step.Walk(at, state, after)
                state = after
            }

            is Command.Forward -> {
                repeat(cmd.cells) {
                    val next = state.pos + state.dir
                    val f = walkCheck(level, next)
                    if (f != null) return Outcome.Failed(f)
                    val after = state.copy(pos = next)
                    out += Step.Walk(at, state, after)
                    state = after
                }
            }

            Command.TurnLeft -> state = rotate(state, state.dir.left(), at, out)
            Command.TurnRight -> state = rotate(state, state.dir.right(), at, out)

            Command.Jump -> {
                val landing = state.pos.step(state.dir, 2)
                if (!level.grid.isWalkable(landing)) return Outcome.Failed(Failure.BadLanding(landing))
                val after = state.copy(pos = landing)
                out += Step.Hop(at, state, after)
                state = after
            }

            // Loops are unrolled and calls expanded by run(); neither reaches here.
            is Command.Repeat, Command.Call -> error("$cmd inside execute")
        }
        return Outcome.Ok(state)
    }

    private fun rotate(state: HeroState, to: Dir, at: CommandRef, out: MutableList<Step>): HeroState {
        val after = state.copy(dir = to)
        out += Step.Rotate(at, state, after)
        return after
    }

    private fun walkCheck(level: Level, next: Pos): Failure? = when (level.grid.cellOrNull(next)) {
        Cell.WALKABLE -> null
        Cell.OBSTACLE -> Failure.Bumped(next)
        Cell.BLOCKED, null -> Failure.Fell(next)
    }
}
