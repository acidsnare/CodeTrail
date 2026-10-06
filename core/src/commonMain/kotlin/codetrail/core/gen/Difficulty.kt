package codetrail.core.gen

import codetrail.core.command.CommandSet

/**
 * Knobs for one difficulty tier. Tiers are 1..5 and map onto the worksheet progression:
 * arrows, longer arrows, relative turns, jumps, degree turns with tight slot budget.
 */
data class Difficulty(
    val tier: Int,
    val width: Int,
    val height: Int,
    val commandSet: CommandSet,
    /** Number of cells in the generated path, start and goal included. */
    val pathLength: IntRange,
    /** Probability of continuing straight at each step. Lower means more turns. */
    val straightBias: Double,
    /** How many OBSTACLE cells to drop onto straight segments. */
    val obstacles: IntRange,
    /** Extra commands allowed beyond the optimal solution. */
    val slotSlack: Int,
    /** Reject generated levels whose shortest program is longer than this. */
    val maxOptimal: Int = 14,
) {
    companion object {
        val TIERS: List<Difficulty> = listOf(
            Difficulty(1, 7, 5, CommandSet.ABSOLUTE, 5..7, 0.75, 0..0, slotSlack = 4, maxOptimal = 7),
            Difficulty(2, 7, 5, CommandSet.ABSOLUTE, 9..13, 0.55, 0..0, slotSlack = 3, maxOptimal = 13),
            Difficulty(3, 7, 5, CommandSet.RELATIVE, 9..13, 0.55, 0..0, slotSlack = 3, maxOptimal = 12),
            Difficulty(4, 7, 5, CommandSet.RELATIVE_JUMP, 10..13, 0.5, 1..2, slotSlack = 2, maxOptimal = 13),
            Difficulty(5, 9, 6, CommandSet.RELATIVE_DEGREES, 11..15, 0.5, 1..2, slotSlack = 1, maxOptimal = 13),
        )

        fun of(tier: Int): Difficulty = TIERS.getOrNull(tier - 1)
            ?: throw IllegalArgumentException("Difficulty tier must be in 1..${TIERS.size}, got $tier")
    }
}
