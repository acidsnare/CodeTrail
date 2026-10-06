package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.CommandSet
import codetrail.core.command.Compiled
import codetrail.core.command.Program

/**
 * Folds a flat program into the fewest slots the command set allows: loops, and on the
 * block tier also one reusable block. The block is chosen by trying every run of 2..4 cards
 * that occurs at least twice; what is left is compressed with loops. Small inputs (at most
 * ~25 cards), so brute force is fine.
 */
object ProgramCompressor {

    fun compress(flat: Program, set: CommandSet): Compiled {
        val loopsOnly = Compiled(if (set.loops) LoopCompressor.compress(flat) else flat)
        if (!set.functions) return loopsOnly

        var best = loopsOnly
        for (len in 2..4) {
            for (start in 0..flat.size - len) {
                val block = flat.subList(start, start + len)
                val replaced = replace(flat, block)
                val calls = replaced.count { it == Command.Call }
                if (calls < 2) continue
                val candidate = Compiled(if (set.loops) LoopCompressor.compress(replaced) else replaced, block.toList())
                if (candidate.slots < best.slots || (candidate.slots == best.slots && best.function.isEmpty())) best = candidate
            }
        }
        return best
    }

    /** Every non-overlapping occurrence of [block], left to right, becomes a call. */
    private fun replace(flat: Program, block: List<Command>): Program {
        val out = ArrayList<Command>()
        var i = 0
        while (i < flat.size) {
            if (i + block.size <= flat.size && flat.subList(i, i + block.size) == block) {
                out += Command.Call
                i += block.size
            } else {
                out += flat[i]
                i++
            }
        }
        return out
    }
}
