package codetrail.core.gen

import codetrail.core.model.Dir
import codetrail.core.model.Pos
import kotlin.random.Random

/**
 * Builds a self-avoiding path whose cells never touch each other except along the path.
 * That "thin" property means there is exactly one corridor from start to goal:
 * no shortcuts, no ambiguity about which way the kid should go.
 */
internal class PathCarver(
    private val width: Int,
    private val height: Int,
    private val rng: Random,
    private val straightBias: Double,
    /**
     * When jumps are available a path cell two steps away in a straight line is a shortcut
     * (hop over the water between them). Keep such cells apart too.
     */
    private val jumpSafe: Boolean,
) {

    fun carve(targetLength: Int, maxAttempts: Int = 200): List<Pos>? {
        repeat(maxAttempts) {
            val start = Pos(rng.nextInt(width), rng.nextInt(height))
            val path = ArrayList<Pos>(targetLength)
            val used = HashSet<Pos>()
            path += start
            used += start
            if (extend(path, used, targetLength, null, depth = 0)) return path
        }
        return null
    }

    private fun extend(path: ArrayList<Pos>, used: HashSet<Pos>, target: Int, heading: Dir?, depth: Int): Boolean {
        if (path.size == target) return true
        if (depth > target * 4) return false

        val cur = path.last()
        val candidates = Dir.entries
            .filter { d -> isFree(cur + d, cur, path, used) }
            .toMutableList()
        if (candidates.isEmpty()) return false

        // Prefer going straight with probability straightBias, otherwise shuffle.
        val ordered = ArrayList<Dir>(4)
        if (heading != null && heading in candidates && rng.nextDouble() < straightBias) {
            ordered += heading
            candidates -= heading
        }
        candidates.shuffle(rng)
        ordered += candidates

        for (d in ordered) {
            val next = cur + d
            path += next
            used += next
            if (extend(path, used, target, d, depth + 1)) return true
            path.removeAt(path.lastIndex)
            used -= next
        }
        return false
    }

    /**
     * A cell may join the path if it is on the grid, unused, and touches no path cell except [from].
     * With [jumpSafe] it also must not be two cells in a straight line from any path cell,
     * except the one two steps back along the path itself.
     */
    private fun isFree(p: Pos, from: Pos, path: List<Pos>, used: Set<Pos>): Boolean {
        if (p.x !in 0 until width || p.y !in 0 until height) return false
        if (p in used) return false
        for (d in Dir.entries) {
            val n = p + d
            if (n != from && n in used) return false
        }
        if (jumpSafe) {
            val twoBack = path.getOrNull(path.size - 2)
            for (d in Dir.entries) {
                val far = p.step(d, 2)
                if (far != twoBack && far in used) return false
            }
        }
        return true
    }
}
