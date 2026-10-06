package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.Program
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

/** One atomic motion the UI can animate. Several steps may come from one command. */
sealed interface Step {
    val commandIndex: Int
    val before: HeroState
    val after: HeroState

    data class Walk(override val commandIndex: Int, override val before: HeroState, override val after: HeroState) : Step
    data class Rotate(override val commandIndex: Int, override val before: HeroState, override val after: HeroState) : Step
    data class Hop(override val commandIndex: Int, override val before: HeroState, override val after: HeroState) : Step
}

data class Trace(
    val steps: List<Step>,
    val finalState: HeroState,
    val failure: Failure?,
    /** Index of the command that failed, if any. */
    val failedCommandIndex: Int?,
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

    fun run(level: Level, program: Program): Trace {
        if (program.size > level.maxSlots) {
            val s = HeroState(level.start, level.startDir)
            return Trace(emptyList(), s, Failure.TooManyCommands, null)
        }
        val steps = mutableListOf<Step>()
        var state = HeroState(level.start, level.startDir)

        for ((i, cmd) in program.withIndex()) {
            if (!level.commandSet.accepts(cmd)) {
                return Trace(steps, state, Failure.NotAllowed(cmd), i)
            }
            val result = execute(level, state, cmd, i, steps)
            when (result) {
                is Outcome.Ok -> state = result.state
                is Outcome.Failed -> return Trace(steps, state, result.failure, i)
            }
        }

        val failure = if (state.pos == level.goal) null else Failure.NotAtGoal(state.pos)
        return Trace(steps, state, failure, null)
    }

    private sealed interface Outcome {
        data class Ok(val state: HeroState) : Outcome
        data class Failed(val failure: Failure) : Outcome
    }

    private fun execute(level: Level, start: HeroState, cmd: Command, index: Int, out: MutableList<Step>): Outcome {
        var state = start
        when (cmd) {
            is Command.Move -> {
                // Absolute move also turns the hero so sprites face the travel direction.
                val faced = state.copy(dir = cmd.dir)
                val next = faced.pos + cmd.dir
                val f = walkCheck(level, next)
                if (f != null) return Outcome.Failed(f)
                val after = HeroState(next, cmd.dir)
                out += Step.Walk(index, state, after)
                state = after
            }

            is Command.Forward -> {
                repeat(cmd.cells) {
                    val next = state.pos + state.dir
                    val f = walkCheck(level, next)
                    if (f != null) return Outcome.Failed(f)
                    val after = state.copy(pos = next)
                    out += Step.Walk(index, state, after)
                    state = after
                }
            }

            Command.TurnLeft -> state = rotate(state, state.dir.left(), index, out)
            Command.TurnRight -> state = rotate(state, state.dir.right(), index, out)
            is Command.Turn -> state = rotate(state, state.dir.rotate(cmd.degrees), index, out)

            Command.Jump -> {
                val landing = state.pos.step(state.dir, 2)
                if (!level.grid.isWalkable(landing)) return Outcome.Failed(Failure.BadLanding(landing))
                val after = state.copy(pos = landing)
                out += Step.Hop(index, state, after)
                state = after
            }
        }
        return Outcome.Ok(state)
    }

    private fun rotate(state: HeroState, to: Dir, index: Int, out: MutableList<Step>): HeroState {
        val after = state.copy(dir = to)
        out += Step.Rotate(index, state, after)
        return after
    }

    private fun walkCheck(level: Level, next: Pos): Failure? = when (level.grid.cellOrNull(next)) {
        Cell.WALKABLE -> null
        Cell.OBSTACLE -> Failure.Bumped(next)
        Cell.BLOCKED, null -> Failure.Fell(next)
    }
}
