package codetrail.core.command

import codetrail.core.model.Dir

/** A single instruction the player can place into a program slot. */
sealed interface Command {

    /** Move one cell in an absolute direction. Heading is ignored. Beginner levels. */
    data class Move(val dir: Dir) : Command

    /** Move [cells] cells in the current heading, one cell at a time. */
    data class Forward(val cells: Int) : Command {
        init {
            require(cells >= 1) { "Forward needs at least 1 cell" }
        }
    }

    data object TurnLeft : Command

    data object TurnRight : Command

    /** Hop over the next cell in the current heading and land two cells ahead. */
    data object Jump : Command

    /**
     * Run [body] [times] times in a row. One level of nesting only: the body never holds
     * another Repeat. Costs 1 slot plus one per body card, which is what makes loops pay off.
     */
    data class Repeat(val times: Int, val body: List<Command>) : Command {
        init {
            require(times >= 2) { "Repeat needs at least 2 iterations" }
            require(body.none { it is Repeat }) { "Repeat cannot be nested" }
        }
    }

    /**
     * Run the level's one reusable block ("block A"), defined once next to the program.
     * The block holds plain cards only (no loops, no calls). A call costs 1 slot, the
     * block's cards cost a slot each, once.
     */
    data object Call : Command
}

typealias Program = List<Command>

/** How many slots a program occupies: a loop costs one slot for itself plus its body. */
fun Program.slotCount(): Int = sumOf { if (it is Command.Repeat) 1 + it.body.size else 1 }

/** The program with every loop unrolled and every call replaced by the block's cards. */
fun Program.flatten(function: Program = emptyList()): Program = flatMap { c ->
    when (c) {
        is Command.Repeat -> List(c.times) { c.body.flatten(function) }.flatten()
        Command.Call -> function
        else -> listOf(c)
    }
}

/** A program together with its reusable block. Slots count both, the block once. */
data class Compiled(val program: Program, val function: Program = emptyList()) {
    val slots: Int get() = program.slotCount() + function.size
    fun flatten(): Program = program.flatten(function)
}
