package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.Compiled
import codetrail.core.command.Program
import codetrail.core.model.Level
import codetrail.core.model.Pos
import kotlin.random.Random

/**
 * "Where does the hero end up?" - the reverse of the usual puzzle, like worksheets 5 and 6.
 * The program follows a prefix of the level's corridor, so it is always valid, and the player
 * has to read it and point at the final cell.
 */
data class PredictPuzzle(val program: Program, val answer: Pos, val function: Program = emptyList())

object PredictPuzzles {

    /** Deterministic for a given level: the same seed always yields the same question. */
    fun make(level: Level): PredictPuzzle {
        val rng = Random(level.seed xor 0x5EED)
        val corridor = level.corridor()
        // at least 3 moves, at most the whole corridor; stop short of the goal most of the time
        val minLen = minOf(4, corridor.size)
        val lengths = if (corridor.size <= minLen) listOf(corridor.size) else (minLen..corridor.size).toList()
        val candidates = if (level.commandSet.loops) {
            // Loop tier: prefer prefixes whose program folds into a loop, that is the reading skill being taught.
            // Score: loops in the program plus calls of block A, so the reading puzzle exercises what the tier teaches.
            fun score(c: Compiled) = c.program.count { it is Command.Repeat } + c.program.flatMap { if (it is Command.Repeat) it.body else listOf(it) }.count { it == Command.Call }
            val scored = lengths.map { len -> len to score(ProgramDeriver.compiledFromPath(corridor.take(len), level.startDir, level.commandSet)) }
            val best = scored.maxOf { it.second }
            scored.filter { it.second == best }.map { it.first }
        } else {
            lengths
        }
        val len = candidates[rng.nextInt(candidates.size)]
        val prefix = corridor.take(len)
        val compiled = ProgramDeriver.compiledFromPath(prefix, level.startDir, level.commandSet)
        val end = Interpreter.run(level.copy(maxSlots = Int.MAX_VALUE), compiled.program, compiled.function).finalState.pos
        return PredictPuzzle(compiled.program, end, compiled.function)
    }
}
