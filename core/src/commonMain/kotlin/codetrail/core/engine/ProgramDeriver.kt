package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.CommandSet
import codetrail.core.command.Program
import codetrail.core.model.Dir
import codetrail.core.model.Pos

/**
 * Turns a cell path into a program in a given command set.
 * Used for hints and for "where does the hero end up?" reverse puzzles,
 * where we need a plausible program rather than the shortest one.
 */
object ProgramDeriver {

    fun fromPath(path: List<Pos>, startDir: Dir, set: CommandSet): Program {
        require(path.size >= 2) { "Path needs at least two cells" }
        val moves = path.zipWithNext().map { (a, b) -> Dir.entries.first { a + it == b } }
        if (!set.relative) return moves.map { Command.Move(it) }

        val out = ArrayList<Command>()
        var heading = startDir
        var run = 0
        for (d in moves) {
            if (d != heading) {
                if (run > 0) out += Command.Forward(run)
                run = 0
                out.addAll(turn(heading, d, set))
                heading = d
            }
            run++
        }
        if (run > 0) out += Command.Forward(run)
        return out
    }

    private fun turn(from: Dir, to: Dir, set: CommandSet): List<Command> {
        val quarters = ((to.ordinal - from.ordinal) % 4 + 4) % 4
        if (set.degreeTurns) return listOf(Command.Turn(quarters * 90))
        return when (quarters) {
            1 -> listOf(Command.TurnRight)
            3 -> listOf(Command.TurnLeft)
            // A U-turn has no single card in this set, so spend two.
            else -> listOf(Command.TurnRight, Command.TurnRight)
        }
    }
}
