package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.Program
import codetrail.core.command.slotCount

/**
 * Rewrites a flat program with "repeat N times" loops so it takes the fewest slots.
 * Dynamic programming over prefixes: a prefix either ends with a plain card, or with a block
 * of length L repeated k >= 2 times, which costs 1 + L slots instead of k * L.
 */
object LoopCompressor {

    fun compress(flat: Program, maxRepeat: Int = 5): Program {
        require(flat.none { it is Command.Repeat }) { "compress expects a flat program" }
        val n = flat.size
        val best = IntArray(n + 1) { Int.MAX_VALUE }
        val choice = arrayOfNulls<Pair<Int, Int>>(n + 1) // (blockLength, times); times == 1 means a plain card
        best[0] = 0
        for (i in 1..n) {
            best[i] = best[i - 1] + 1
            choice[i] = 1 to 1
            for (len in 1..i / 2) {
                val block = flat.subList(i - len, i)
                var times = 1
                while (times < maxRepeat && i - (times + 1) * len >= 0 && flat.subList(i - (times + 1) * len, i - times * len) == block) {
                    times++
                    val cost = best[i - times * len] + 1 + len
                    if (cost < best[i]) {
                        best[i] = cost
                        choice[i] = len to times
                    }
                }
            }
        }
        val out = ArrayList<Command>()
        var i = n
        while (i > 0) {
            val (len, times) = choice[i]!!
            if (times == 1) out += flat[i - 1] else out += Command.Repeat(times, flat.subList(i - len, i).toList())
            i -= len * times
        }
        out.reverse()
        return out
    }

    /** Slots saved by looping, 0 when the program has no repetition worth a loop. */
    fun savings(flat: Program): Int = flat.size - compress(flat).slotCount()
}
