package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.CommandSet
import codetrail.core.command.Program
import codetrail.core.command.Compiled
import codetrail.core.model.Dir
import codetrail.core.model.Grid
import codetrail.core.model.Pos

/**
 * Breadth-first search over (position, heading) where every command costs 1.
 * Gives the optimal flat program for star rating and validates generated levels.
 * Loops are not searched: the flat optimum is compressed afterwards by [LoopCompressor],
 * which is exact here because the corridor is unique and so is its flat program.
 */
object Solver {

    data class Solution(val program: Program, val function: Program = emptyList()) {
        /** Slots the program occupies, loops and block A already folded in. */
        val length: Int get() = Compiled(program, function).slots
        val flat: Program get() = Compiled(program, function).flatten()
    }

    fun solve(grid: Grid, start: Pos, startDir: Dir, goal: Pos, set: CommandSet): Solution? {
        val origin = HeroState(start, startDir)
        val parent = HashMap<HeroState, Pair<HeroState, Command>>()
        val visited = HashSet<HeroState>()
        val queue = ArrayDeque<HeroState>()
        visited += origin
        queue += origin

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur.pos == goal) {
                val flat = reconstruct(origin, cur, parent)
                val folded = ProgramCompressor.compress(flat, set)
                return Solution(folded.program, folded.function)
            }
            for ((cmd, next) in edges(grid, cur, set)) {
                if (visited.add(next)) {
                    parent[next] = cur to cmd
                    queue += next
                }
            }
        }
        return null
    }

    private fun reconstruct(
        origin: HeroState,
        end: HeroState,
        parent: Map<HeroState, Pair<HeroState, Command>>,
    ): Program {
        val out = ArrayList<Command>()
        var cur = end
        while (cur != origin) {
            val (prev, cmd) = parent.getValue(cur)
            out += cmd
            cur = prev
        }
        out.reverse()
        return out
    }

    /** All (command, resulting state) pairs legal from [s] under [set]. */
    private fun edges(grid: Grid, s: HeroState, set: CommandSet): List<Pair<Command, HeroState>> {
        val out = ArrayList<Pair<Command, HeroState>>()
        if (!set.relative) {
            for (d in Dir.entries) {
                val p = s.pos + d
                if (grid.isWalkable(p)) out += Command.Move(d) to HeroState(p, d)
            }
            return out
        }

        // forward 1..n while terrain allows
        var p = s.pos
        for (n in 1..set.maxForward) {
            p += s.dir
            if (!grid.isWalkable(p)) break
            out += Command.Forward(n) to s.copy(pos = p)
        }

        out += Command.TurnLeft to s.copy(dir = s.dir.left())
        out += Command.TurnRight to s.copy(dir = s.dir.right())

        if (set.jump) {
            val landing = s.pos.step(s.dir, 2)
            if (grid.isWalkable(landing)) out += Command.Jump to s.copy(pos = landing)
        }
        return out
    }
}
