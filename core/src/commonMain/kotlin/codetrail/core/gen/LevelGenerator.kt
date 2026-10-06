package codetrail.core.gen

import codetrail.core.command.CommandSet
import codetrail.core.engine.Solver
import codetrail.core.model.Cell
import codetrail.core.model.Dir
import codetrail.core.model.Grid
import codetrail.core.model.Level
import codetrail.core.model.Pos
import kotlin.random.Random

/**
 * Produces levels that are solvable by construction:
 * carve a path first, fill everything else with BLOCKED, then verify with the solver.
 */
class LevelGenerator {

    fun generate(tier: Int, seed: Long = Random.nextLong()): Level =
        generate(Difficulty.of(tier), seed)

    fun generate(difficulty: Difficulty, seed: Long): Level {
        var attempt = 0L
        while (true) {
            val rng = Random(seed + attempt * 7919)
            val level = tryGenerate(difficulty, seed, rng)
            if (level != null) return level
            attempt++
            check(attempt < 1000) { "Could not generate a level for tier ${difficulty.tier} with seed $seed" }
        }
    }

    private fun tryGenerate(d: Difficulty, seed: Long, rng: Random): Level? {
        val length = rng.nextInt(d.pathLength.first, d.pathLength.last + 1)
        val path = PathCarver(d.width, d.height, rng, d.straightBias, jumpSafe = d.commandSet.jump).carve(length) ?: return null

        val grid = Grid(d.width, d.height, Cell.BLOCKED)
        path.forEach { grid[it] = Cell.WALKABLE }

        val start = path.first()
        val goal = path.last()
        val startDir = initialHeading(path, d.commandSet, rng)

        placeObstacles(grid, path, rng.nextInt(d.obstacles.first, d.obstacles.last + 1))

        val solution = Solver.solve(grid, start, startDir, goal, d.commandSet) ?: return null

        return Level(
            grid = grid,
            start = start,
            startDir = startDir,
            goal = goal,
            commandSet = d.commandSet,
            maxSlots = solution.length + d.slotSlack,
            optimalLength = solution.length,
            difficulty = d.tier,
            seed = seed,
        )
    }

    /**
     * Absolute levels: face along the first step so the sprite looks sensible.
     * Relative levels: along the path or perpendicular to it, so the first command is often
     * a single turn. Never facing away: a U-turn costs two cards and only frustrates.
     */
    private fun initialHeading(path: List<Pos>, set: CommandSet, rng: Random): Dir {
        val first = path[0]
        val second = path[1]
        val along = Dir.entries.first { first + it == second }
        return if (set.relative) Dir.entries.filter { it != along.opposite() }.random(rng) else along
    }

    /**
     * Drops obstacles in the middle of straight runs of at least 3 cells, so the hero can
     * stand before it, jump, and land on the path. Never on start or goal.
     */
    private fun placeObstacles(grid: Grid, path: List<Pos>, count: Int) {
        if (count == 0) return
        val candidates = (1 until path.size - 1).filter { i ->
            val a = path[i - 1]
            val b = path[i]
            val c = path[i + 1]
            val straight = (a.x == b.x && b.x == c.x) || (a.y == b.y && b.y == c.y)
            straight
        }.toMutableList()

        var placed = 0
        while (placed < count && candidates.isNotEmpty()) {
            val i = candidates.removeAt(0)
            // Keep obstacles apart: neighbours on the path must stay walkable.
            if (grid[path[i - 1]] == Cell.WALKABLE && grid[path[i + 1]] == Cell.WALKABLE) {
                grid[path[i]] = Cell.OBSTACLE
                placed++
                candidates.removeAll { it in (i - 2)..(i + 2) }
            }
        }
    }
}
