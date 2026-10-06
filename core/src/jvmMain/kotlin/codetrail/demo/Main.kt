package codetrail.demo

import codetrail.core.engine.Ascii
import codetrail.core.engine.Interpreter
import codetrail.core.engine.ProgramDeriver
import codetrail.core.engine.Solver
import codetrail.core.gen.LevelGenerator
import kotlin.system.measureTimeMillis

/** Prints one generated level per tier with its optimal solution, then stress-tests the generator. */
fun main(args: Array<String>) {
    val seed = args.firstOrNull()?.toLongOrNull() ?: 42L
    val gen = LevelGenerator()

    for (tier in 1..5) {
        val level = gen.generate(tier, seed)
        val solution = Solver.solve(level.grid, level.start, level.startDir, level.goal, level.commandSet)!!
        val trace = Interpreter.run(level, solution.program)
        println("=== Tier $tier  ${level.grid.width}x${level.grid.height}  ${level.commandSet}  seed=$seed")
        print(Ascii.render(level))
        println("optimal ${level.optimalLength}, slots ${level.maxSlots}: ${Ascii.program(solution.program)}")
        println("replay: ${if (trace.succeeded) "OK" else "FAIL ${trace.failure}"}")
        println()
    }

    val n = 2000
    var worst = 0
    var shortcuts = 0
    val ms = measureTimeMillis {
        for (tier in 1..5) for (i in 0 until n) {
            val level = gen.generate(tier, seed * 31 + i)
            val sol = Solver.solve(level.grid, level.start, level.startDir, level.goal, level.commandSet)
            checkNotNull(sol) { "unsolvable level tier=$tier i=$i" }
            check(Interpreter.run(level, sol.program).succeeded) { "replay failed tier=$tier i=$i" }
            if (sol.length > worst) worst = sol.length
            // A solution shorter than the path-following program means the hero can cut corners.
            val derived = ProgramDeriver.fromPath(pathOf(level), level.startDir, level.commandSet)
            val obstacles = level.grid.positions().count { level.grid[it] == codetrail.core.model.Cell.OBSTACLE }
            if (sol.length < derived.size - 2 * obstacles) shortcuts++
        }
    }
    println("stress: ${5 * n} levels generated + solved + replayed in ${ms}ms, longest optimal program $worst, shortcuts $shortcuts")
}

/** Walk the unique corridor from start to goal. Obstacles count as corridor cells. */
private fun pathOf(level: codetrail.core.model.Level): List<codetrail.core.model.Pos> {
    val g = level.grid
    val out = mutableListOf(level.start)
    var prev: codetrail.core.model.Pos? = null
    var cur = level.start
    while (cur != level.goal) {
        val next = codetrail.core.model.Dir.entries.map { cur + it }
            .first { it != prev && g.contains(it) && g[it] != codetrail.core.model.Cell.BLOCKED }
        prev = cur
        cur = next
        out += cur
    }
    return out
}
