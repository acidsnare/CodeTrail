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

    /** Rotate clockwise by [degrees], a multiple of 90. */
    data class Turn(val degrees: Int) : Command {
        init {
            require(degrees % 90 == 0 && degrees in 90..270) { "Turn must be 90, 180 or 270" }
        }
    }

    /** Hop over the next cell in the current heading and land two cells ahead. */
    data object Jump : Command
}

typealias Program = List<Command>
