package codetrail.core.model

/** Grid position. Origin is the top-left corner, y grows downward. */
data class Pos(val x: Int, val y: Int) {
    operator fun plus(d: Dir): Pos = Pos(x + d.dx, y + d.dy)
    fun step(d: Dir, n: Int): Pos = Pos(x + d.dx * n, y + d.dy * n)
}

/** Cardinal direction. Order is clockwise so turning is index arithmetic. */
enum class Dir(val dx: Int, val dy: Int) {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    fun right(): Dir = entries[(ordinal + 1) % 4]
    fun left(): Dir = entries[(ordinal + 3) % 4]
    fun opposite(): Dir = entries[(ordinal + 2) % 4]

    /** Rotate clockwise by a multiple of 90 degrees. */
    fun rotate(degrees: Int): Dir {
        require(degrees % 90 == 0) { "Rotation must be a multiple of 90, got $degrees" }
        val quarter = ((degrees / 90) % 4 + 4) % 4
        return entries[(ordinal + quarter) % 4]
    }
}
