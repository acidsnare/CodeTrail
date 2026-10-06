package codetrail.core.engine

import codetrail.core.command.Program
import codetrail.core.model.Level
import codetrail.core.model.Pos
import kotlin.random.Random

/**
 * "Where does the hero end up?" - the reverse of the usual puzzle, like worksheets 5 and 6.
 * The program follows a prefix of the level's corridor, so it is always valid, and the player
 * has to read it and point at the final cell.
 */
data class PredictPuzzle(val program: Program, val answer: Pos)

object PredictPuzzles {

    /** Deterministic for a given level: the same seed always yields the same question. */
    fun make(level: Level): PredictPuzzle {
        val rng = Random(level.seed xor 0x5EED)
        val corridor = level.corridor()
        // at least 3 moves, at most the whole corridor; stop short of the goal most of the time
        val minLen = minOf(4, corridor.size)
        val len = if (corridor.size <= minLen) corridor.size else rng.nextInt(minLen, corridor.size + 1)
        val prefix = corridor.take(len)
        val program = ProgramDeriver.fromPath(prefix, level.startDir, level.commandSet)
        val end = Interpreter.run(level.copy(maxSlots = Int.MAX_VALUE), program).finalState.pos
        return PredictPuzzle(program, end)
    }
}
