package codetrail.core.command

/**
 * Which commands a difficulty level offers. Drives the palette in the UI
 * and the edge set in the solver.
 */
enum class CommandSet(
    val relative: Boolean,
    val jump: Boolean,
    val degreeTurns: Boolean,
    /** Largest X the "forward X" card may carry. Irrelevant for absolute sets. */
    val maxForward: Int,
) {
    /** Arrow cards: up, down, left, right. */
    ABSOLUTE(relative = false, jump = false, degreeTurns = false, maxForward = 1),

    /** forward X, turn left, turn right. */
    RELATIVE(relative = true, jump = false, degreeTurns = false, maxForward = 9),

    /** RELATIVE plus "hop over 1 cell". */
    RELATIVE_JUMP(relative = true, jump = true, degreeTurns = false, maxForward = 9),

    /** forward X, turn X degrees, jump. */
    RELATIVE_DEGREES(relative = true, jump = true, degreeTurns = true, maxForward = 9);

    fun accepts(c: Command): Boolean = when (c) {
        is Command.Move -> !relative
        is Command.Forward -> relative && c.cells <= maxForward
        Command.TurnLeft, Command.TurnRight -> relative && !degreeTurns
        is Command.Turn -> relative && degreeTurns
        Command.Jump -> jump
    }
}
