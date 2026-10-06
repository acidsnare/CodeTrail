package codetrail.core.gen

import codetrail.core.model.Dir
import codetrail.core.model.Pos
import kotlin.random.Random

/**
 * Paths for the loop and block tiers: a random head, a repeated block, a random tail.
 * Random corridors rarely repeat themselves, so levels built this way are what makes
 * "repeat 3 times" and "block A" worth teaching.
 *
 * Loop tier: the block repeats back to back. Block tier: the same block appears twice or
 * three times with random walking in between, so a loop does not fit but a reusable block does.
 * Blocks are relative (forward, turn, jump), laid out from whatever heading the path has.
 */
internal class PatternCarver(
    private val width: Int,
    private val height: Int,
    private val rng: Random,
    private val carver: PathCarver,
    /** Predict puzzles run without obstacles, so hop blocks are not offered there. */
    private val allowObstacles: Boolean,
    /** Block tier: separate the repetitions with random segments instead of chaining them. */
    private val separated: Boolean,
) {

    class Result(val path: List<Pos>, val obstacles: Set<Pos>, val protected: Set<Int>)

    /** One relative move of a block. */
    private sealed interface Rel {
        data class F(val n: Int) : Rel
        data object L : Rel
        data object R : Rel
        data object J : Rel
    }

    private class Block(val moves: List<Rel>) {
        val cells: Int get() = moves.sumOf { when (it) { is Rel.F -> it.n; Rel.J -> 2; else -> 0 } }
    }

    fun carve(targetLength: Int, maxAttempts: Int = 40): Result? {
        repeat(maxAttempts) {
            val headLen = rng.nextInt(0, 5)
            val tailLen = rng.nextInt(2, 8)
            val shape = rng.nextInt(10)
            val room = targetLength - 1 - headLen - tailLen
            // Shapes that bend fail placement far more often than a straight hop run, so each gets
            // several placement tries before the shape is given up: otherwise hop runs would dominate.
            repeat(12) {
                val block = when (shape) {
                    in 0..6 -> staircase()
                    in 7..8 -> if (separated) staircase() else meander()
                    else -> if (allowObstacles) hopRun() else staircase()
                }
                val gaps = if (separated) 2 + rng.nextInt(3) else 0
                val maxTimes = (room + gaps) / (block.cells + gaps)
                if (maxTimes < 2) return@repeat
                val times = when {
                    maxTimes >= 4 && !separated && rng.nextInt(4) == 0 -> 4
                    maxTimes >= 3 && (!separated || rng.nextInt(3) == 0) -> 3
                    else -> 2
                }
                val head = if (headLen == 0) listOf(Pos(rng.nextInt(width), rng.nextInt(height))) else carver.carve(headLen + 1, maxAttempts = 20) ?: return@repeat
                val laid = lay(head, block, times, gaps) ?: return@repeat
                val full = carver.extendPath(laid.path, targetLength, headingOf(laid.path)) ?: return@repeat
                return Result(full, laid.obstacles, laid.protected)
            }
        }
        return null
    }

    /** forward a, turn, forward b, turn back: a diagonal staircase. A leg of 3 may carry an obstacle. */
    private fun staircase(): Block {
        val a = leg()
        val b = leg()
        val right = rng.nextBoolean()
        val moves = ArrayList<Rel>()
        moves += legMoves(a)
        moves += if (right) Rel.R else Rel.L
        moves += legMoves(b)
        moves += if (right) Rel.L else Rel.R
        return Block(moves)
    }

    /** Leg length 1..3, short legs favoured so three iterations usually fit the board. */
    private fun leg(): Int = when (rng.nextInt(5)) { 0, 1 -> 1; 2, 3 -> 2; else -> 3 }

    /** A leg of 3 becomes "forward 1, jump" half the time when obstacles are allowed. */
    private fun legMoves(n: Int): List<Rel> =
        if (n == 3 && allowObstacles && rng.nextBoolean()) listOf(Rel.F(1), Rel.J) else listOf(Rel.F(n))

    /** forward a, turn, forward 3, turn, forward a, turn back, forward 3, turn back: a narrow snake. */
    private fun meander(): Block {
        val a = 1 + rng.nextInt(2)
        val right = rng.nextBoolean()
        val (t1, t2) = if (right) Rel.R to Rel.L else Rel.L to Rel.R
        return Block(listOf(Rel.F(a), t1, Rel.F(3), t1, Rel.F(a), t2, Rel.F(3), t2))
    }

    /** forward a, jump: a straight run with an obstacle every a + 2 cells. */
    private fun hopRun(): Block = Block(listOf(Rel.F(2 + rng.nextInt(2)), Rel.J))

    /**
     * Appends [times] copies of the block to [head], with [gaps] random cells between copies,
     * keeping the corridor thin. Returns the path, obstacle cells and the path indices that the
     * pattern occupies (random obstacles must stay out of them).
     */
    private fun lay(head: List<Pos>, block: Block, times: Int, gaps: Int): Result? {
        var path: List<Pos> = ArrayList(head)
        val obstacles = HashSet<Pos>()
        val protected = HashSet<Int>()
        var heading = headingOf(path) ?: Dir.entries.random(rng)
        repeat(times) { t ->
            if (t > 0 && gaps > 0) {
                path = carver.extendPath(path, path.size + gaps, heading) ?: return null
                heading = headingOf(path)!!
            }
            val out = ArrayList(path)
            val from = out.lastIndex
            for (m in block.moves) {
                when (m) {
                    Rel.L -> heading = heading.left()
                    Rel.R -> heading = heading.right()
                    is Rel.F -> repeat(m.n) { if (!step(out, heading)) return null }
                    Rel.J -> {
                        if (!step(out, heading)) return null
                        obstacles += out.last()
                        if (!step(out, heading)) return null
                    }
                }
            }
            for (i in from..out.lastIndex) protected += i
            path = out
        }
        return Result(path, obstacles, protected)
    }

    private fun step(path: ArrayList<Pos>, d: Dir): Boolean {
        val next = path.last() + d
        if (next.x !in 0 until width || next.y !in 0 until height || !carver.canAppend(path, next)) return false
        path += next
        return true
    }

    private fun headingOf(path: List<Pos>): Dir? {
        if (path.size < 2) return null
        val a = path[path.size - 2]
        val b = path.last()
        return Dir.entries.first { a + it == b }
    }
}
