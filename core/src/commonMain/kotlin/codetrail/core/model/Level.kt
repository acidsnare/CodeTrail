package codetrail.core.model

import codetrail.core.command.CommandSet

/**
 * A fully generated puzzle. Immutable from the player's point of view.
 *
 * @property maxSlots how many commands the program may contain.
 * @property optimalLength shortest known program length, used for star rating.
 * @property seed lets the same level be regenerated or shared.
 */
data class Level(
    val grid: Grid,
    val start: Pos,
    val startDir: Dir,
    val goal: Pos,
    val commandSet: CommandSet,
    val maxSlots: Int,
    val optimalLength: Int,
    val difficulty: Int,
    val seed: Long,
) {
    init {
        require(grid.isWalkable(start)) { "Start must be walkable" }
        require(grid.isWalkable(goal)) { "Goal must be walkable" }
        require(start != goal) { "Start and goal must differ" }
    }

    /**
     * The unique corridor from start to goal, obstacle cells included.
     * Valid because the generator never lets path cells touch except along the path.
     */
    fun corridor(): List<Pos> {
        val out = mutableListOf(start)
        var prev: Pos? = null
        var cur = start
        while (cur != goal) {
            val next = Dir.entries.map { cur + it }
                .firstOrNull { it != prev && grid.cellOrNull(it)?.let { c -> c != Cell.BLOCKED } == true }
                ?: break
            prev = cur
            cur = next
            out += cur
        }
        return out
    }
}
