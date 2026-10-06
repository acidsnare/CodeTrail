package codetrail.core.command

/**
 * Which commands a difficulty level offers. Drives the palette in the UI
 * and the edge set in the solver.
 */
enum class CommandSet(
    val relative: Boolean,
    val jump: Boolean,
    val loops: Boolean,
    /** "Block A": one reusable group of cards that can be called from anywhere. */
    val functions: Boolean,
    /** Largest X the "forward X" card may carry. Irrelevant for absolute sets. */
    val maxForward: Int,
) {
    /** Arrow cards: up, down, left, right. */
    ABSOLUTE(relative = false, jump = false, loops = false, functions = false, maxForward = 1),

    /** forward X, turn left, turn right. */
    RELATIVE(relative = true, jump = false, loops = false, functions = false, maxForward = 9),

    /** RELATIVE plus "hop over 1 cell". */
    RELATIVE_JUMP(relative = true, jump = true, loops = false, functions = false, maxForward = 9),

    /** RELATIVE_JUMP plus "repeat N times". */
    RELATIVE_LOOP(relative = true, jump = true, loops = true, functions = false, maxForward = 9),

    /** RELATIVE_LOOP plus "block A". */
    RELATIVE_FUNC(relative = true, jump = true, loops = true, functions = true, maxForward = 9);

    fun accepts(c: Command): Boolean = when (c) {
        is Command.Move -> !relative
        is Command.Forward -> relative && c.cells <= maxForward
        Command.TurnLeft, Command.TurnRight -> relative
        Command.Jump -> jump
        is Command.Repeat -> loops && c.times <= MAX_REPEAT && c.body.all { accepts(it) }
        Command.Call -> functions
    }

    /** Cards allowed inside block A: plain moves only. */
    fun acceptsInFunction(c: Command): Boolean = functions && c !is Command.Repeat && c !is Command.Call && accepts(c)

    companion object {
        const val MAX_REPEAT = 5
    }
}
