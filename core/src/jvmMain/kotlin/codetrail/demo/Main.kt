package codetrail.demo

import codetrail.core.command.flatten
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

    for (tier in 1..6) {
        val level = gen.generate(tier, seed)
        val solution = Solver.solve(level.grid, level.start, level.startDir, level.goal, level.commandSet)!!
        val trace = Interpreter.run(level, solution.program, solution.function)
        println("=== Tier $tier  ${level.grid.width}x${level.grid.height}  ${level.commandSet}  seed=$seed")
        print(Ascii.render(level))
        val block = if (solution.function.isEmpty()) "" else "   A = [${Ascii.program(solution.function)}]"
        println("optimal ${level.optimalLength}, slots ${level.maxSlots}: ${Ascii.program(solution.program)}$block")
        println("replay: ${if (trace.succeeded) "OK" else "FAIL ${trace.failure}"}")
        println()
    }

    val n = 2000
    var worst = 0
    var shortcuts = 0
    val ms = measureTimeMillis {
        for (tier in 1..6) for (i in 0 until n) {
            val level = gen.generate(tier, seed * 31 + i)
            val sol = Solver.solve(level.grid, level.start, level.startDir, level.goal, level.commandSet)
            checkNotNull(sol) { "unsolvable level tier=$tier i=$i" }
            check(Interpreter.run(level, sol.program, sol.function).succeeded) { "replay failed tier=$tier i=$i" }
            if (sol.length > worst) worst = sol.length
            // A solution shorter than the path-following program means the hero can cut corners.
            val derived = ProgramDeriver.fromPath(pathOf(level), level.startDir, level.commandSet)
            val obstacles = level.grid.positions().count { level.grid[it] == codetrail.core.model.Cell.OBSTACLE }
            if (sol.flat.size < derived.flatten().size - 2 * obstacles) shortcuts++
        }
    }
    println("stress: ${6 * n} levels generated + solved + replayed in ${ms}ms, longest optimal program $worst, shortcuts $shortcuts")

    // Loop tier: how much do loops actually buy, and how big are they?
    val savings = IntArray(8)
    val times = IntArray(6)
    var loops = 0
    for (i in 0 until n) {
        val level = gen.generate(5, seed * 17 + i)
        val sol = Solver.solve(level.grid, level.start, level.startDir, level.goal, level.commandSet)!!
        savings[minOf(7, sol.flat.size - sol.length)]++
        for (c in sol.program) if (c is codetrail.core.command.Command.Repeat) { loops++; times[c.times]++ }
    }
    println("tier 5 over $n levels: slots saved by loops " + savings.withIndex().filter { it.value > 0 }.joinToString { "${it.index}:${it.value}" } +
        "; loops per level %.2f; repeat counts ".format(loops / n.toDouble()) + (2..5).joinToString { "x$it:${times[it]}" })

    // Block tier: block sizes and how often it is called.
    val blockLen = IntArray(5)
    val callCount = IntArray(6)
    var fnSavings = 0
    for (i in 0 until n) {
        val level = gen.generate(6, seed * 19 + i)
        val sol = Solver.solve(level.grid, level.start, level.startDir, level.goal, level.commandSet)!!
        blockLen[sol.function.size]++
        val calls = sol.program.sumOf { c -> if (c is codetrail.core.command.Command.Repeat) c.times * c.body.count { it == codetrail.core.command.Command.Call } else if (c == codetrail.core.command.Command.Call) 1 else 0 }
        callCount[minOf(5, calls)]++
        fnSavings += sol.flat.size - sol.length
    }
    println("tier 6 over $n levels: block size " + (2..4).joinToString { "$it:${blockLen[it]}" } +
        "; calls " + (2..5).joinToString { "$it:${callCount[it]}" } + "; avg slots saved %.1f".format(fnSavings / n.toDouble()))
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
