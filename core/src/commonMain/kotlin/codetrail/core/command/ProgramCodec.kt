package codetrail.core.command

import codetrail.core.model.Dir

/** Compact text form of a program, e.g. "TR F3 J F1 TL". Used for saves and sharing. */
object ProgramCodec {

    fun encode(program: Program): String = program.joinToString(" ") { encode(it) }

    fun encode(c: Command): String = when (c) {
        is Command.Move -> when (c.dir) {
            Dir.NORTH -> "U"
            Dir.EAST -> "R"
            Dir.SOUTH -> "D"
            Dir.WEST -> "L"
        }
        is Command.Forward -> "F${c.cells}"
        Command.TurnLeft -> "TL"
        Command.TurnRight -> "TR"
        is Command.Turn -> "T${c.degrees}"
        Command.Jump -> "J"
    }

    /** Lenient: unknown tokens are skipped so a corrupt save loses commands, not the whole slot. */
    fun decode(text: String): Program = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.mapNotNull { decodeToken(it) }

    private fun decodeToken(t: String): Command? = when {
        t == "U" -> Command.Move(Dir.NORTH)
        t == "R" -> Command.Move(Dir.EAST)
        t == "D" -> Command.Move(Dir.SOUTH)
        t == "L" -> Command.Move(Dir.WEST)
        t == "TL" -> Command.TurnLeft
        t == "TR" -> Command.TurnRight
        t == "J" -> Command.Jump
        t.startsWith("T") -> t.drop(1).toIntOrNull()?.takeIf { it in setOf(90, 180, 270) }?.let { Command.Turn(it) }
        t.startsWith("F") -> t.drop(1).toIntOrNull()?.takeIf { it >= 1 }?.let { Command.Forward(it) }
        else -> null
    }
}
