package codetrail.core.command

import codetrail.core.model.Dir

/** Compact text form of a program, e.g. "TR F3 J R3[ F1 TL ] F2". Used for saves and sharing. */
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
        Command.Jump -> "J"
        Command.Call -> "A"
        is Command.Repeat -> if (c.body.isEmpty()) "R${c.times}[ ]" else "R${c.times}[ ${encode(c.body)} ]"
    }

    /** Lenient: unknown tokens are skipped so a corrupt save loses commands, not the whole slot. */
    fun decode(text: String): Program {
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val out = ArrayList<Command>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            i++
            if (t.startsWith("R") && t.endsWith("[")) {
                val times = t.drop(1).dropLast(1).toIntOrNull()
                val body = ArrayList<Command>()
                while (i < tokens.size && tokens[i] != "]") {
                    decodeToken(tokens[i])?.takeIf { it !is Command.Repeat }?.let { body += it }
                    i++
                }
                i++ // the closing bracket
                if (times != null && times >= 2) out += Command.Repeat(times, body)
            } else {
                decodeToken(t)?.let { out += it }
            }
        }
        return out
    }

    private fun decodeToken(t: String): Command? = when {
        t == "U" -> Command.Move(Dir.NORTH)
        t == "R" -> Command.Move(Dir.EAST)
        t == "D" -> Command.Move(Dir.SOUTH)
        t == "L" -> Command.Move(Dir.WEST)
        t == "TL" -> Command.TurnLeft
        t == "TR" -> Command.TurnRight
        t == "J" -> Command.Jump
        t == "A" -> Command.Call
        // Degree turns from older saves map onto the plain turn cards; a U-turn is dropped.
        t == "T90" -> Command.TurnRight
        t == "T270" -> Command.TurnLeft
        t.startsWith("F") -> t.drop(1).toIntOrNull()?.takeIf { it >= 1 }?.let { Command.Forward(it) }
        else -> null
    }
}
