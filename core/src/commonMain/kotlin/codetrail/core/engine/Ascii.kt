package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.Program
import codetrail.core.model.Cell
import codetrail.core.model.Dir
import codetrail.core.model.Level
import codetrail.core.model.Pos

/** Text rendering for debugging and the console demo. */
object Ascii {

    fun render(level: Level, hero: HeroState? = null): String {
        val sb = StringBuilder()
        for (y in 0 until level.grid.height) {
            for (x in 0 until level.grid.width) {
                val p = Pos(x, y)
                val ch = when {
                    hero != null && hero.pos == p -> arrow(hero.dir)
                    p == level.start -> arrow(level.startDir)
                    p == level.goal -> '◎'
                    else -> when (level.grid[p]) {
                        Cell.WALKABLE -> '·'
                        Cell.BLOCKED -> '≈'
                        Cell.OBSTACLE -> '✕'
                    }
                }
                sb.append(ch).append(' ')
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    fun arrow(d: Dir): Char = when (d) {
        Dir.NORTH -> '↑'
        Dir.EAST -> '→'
        Dir.SOUTH -> '↓'
        Dir.WEST -> '←'
    }

    fun program(p: Program): String = p.joinToString(" ") { command(it) }

    fun command(c: Command): String = when (c) {
        is Command.Move -> arrow(c.dir).toString()
        is Command.Forward -> "F${c.cells}"
        Command.TurnLeft -> "L"
        Command.TurnRight -> "R"
        Command.Jump -> "J"
        Command.Call -> "A"
        is Command.Repeat -> "${c.times}x[${program(c.body)}]"
    }
}
