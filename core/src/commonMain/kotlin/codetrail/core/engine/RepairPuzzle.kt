package codetrail.core.engine

import codetrail.core.command.Command
import codetrail.core.command.CommandSet
import codetrail.core.command.Compiled
import codetrail.core.model.Dir
import codetrail.core.model.Level
import kotlin.random.Random

/** A card's definition, independent of how often a loop or block executes it. */
sealed interface CardAddress {
    data class Main(val index: Int) : CardAddress
    data class Loop(val index: Int, val bodyIndex: Int) : CardAddress
    data class Block(val index: Int) : CardAddress
}

/** Exactly one legal card has been changed in a working program. */
data class RepairPuzzle(val broken: Compiled, val solution: Compiled, val changedCard: CardAddress)

object RepairPuzzles {
    fun make(level: Level): RepairPuzzle {
        val solved = checkNotNull(Solver.solve(level.grid, level.start, level.startDir, level.goal, level.commandSet))
        val solution = Compiled(solved.program, solved.function)
        check(Interpreter.run(level, solution.program, solution.function).succeeded)
        val candidates = cards(solution).flatMap { (at, card) ->
            alternatives(card, level).mapNotNull { replacement ->
                val broken = replace(solution, at, replacement)
                val trace = Interpreter.run(level, broken.program, broken.function)
                if (trace.succeeded) null else Candidate(
                    RepairPuzzle(broken, solution, at),
                    trace.steps.count { it is Step.Walk || it is Step.Hop },
                )
            }
        }
        check(candidates.isNotEmpty()) { "No repair puzzle for tier ${level.difficulty}, seed ${level.seed}" }
        // Usually exercise the tier's new concept, but keep other bug locations in play.
        val (focused, other) = candidates.partition {
            when (level.difficulty) {
                6 -> it.puzzle.changedCard is CardAddress.Block
                5 -> cardAt(solution, it.puzzle.changedCard) is Command.Repeat
                else -> true
            }
        }
        val rng = Random(level.seed xor 0x524550414952L)
        val group = when {
            focused.isEmpty() -> other
            other.isEmpty() -> focused
            rng.nextDouble() < 0.6 -> focused
            else -> other
        }
        // Let the player see some correct movement before the bug when the map permits it.
        val readable = group.filter { it.movesBeforeFailure >= 2 }.ifEmpty { group }
        return readable.random(rng).puzzle
    }

    private data class Candidate(val puzzle: RepairPuzzle, val movesBeforeFailure: Int)

    private fun alternatives(card: Command, level: Level): List<Command> = when (card) {
        is Command.Move -> Dir.entries.filter { it != card.dir }.map { Command.Move(it) }
        is Command.Forward -> listOf(card.cells - 1, card.cells + 1)
            .filter { it in 1..minOf(level.commandSet.maxForward, maxOf(level.grid.width, level.grid.height) - 1) }
            .map { Command.Forward(it) }
        Command.TurnLeft -> listOf(Command.TurnRight)
        Command.TurnRight -> listOf(Command.TurnLeft)
        Command.Jump -> listOf(Command.Forward(1), Command.Forward(2))
        Command.Call -> listOf(Command.Forward(1))
        is Command.Repeat -> listOf(card.times - 1, card.times + 1)
            .filter { it in 2..CommandSet.MAX_REPEAT }.map { card.copy(times = it) }
    }

    fun cards(compiled: Compiled): List<Pair<CardAddress, Command>> = buildList {
        compiled.program.forEachIndexed { i, card ->
            add(CardAddress.Main(i) to card)
            if (card is Command.Repeat) card.body.forEachIndexed { j, inner -> add(CardAddress.Loop(i, j) to inner) }
        }
        compiled.function.forEachIndexed { i, card -> add(CardAddress.Block(i) to card) }
    }

    fun cardAt(compiled: Compiled, at: CardAddress): Command = when (at) {
        is CardAddress.Main -> compiled.program[at.index]
        is CardAddress.Loop -> (compiled.program[at.index] as Command.Repeat).body[at.bodyIndex]
        is CardAddress.Block -> compiled.function[at.index]
    }

    /** A replacement keeps the program's layout and all loop bodies intact. */
    fun replace(compiled: Compiled, at: CardAddress, replacement: Command): Compiled = when (at) {
        is CardAddress.Main -> compiled.copy(program = compiled.program.toMutableList().apply { set(at.index, replacement) })
        is CardAddress.Loop -> {
            val loop = compiled.program[at.index] as Command.Repeat
            compiled.copy(program = compiled.program.toMutableList().apply {
                set(at.index, loop.copy(body = loop.body.toMutableList().apply { set(at.bodyIndex, replacement) }))
            })
        }
        is CardAddress.Block -> compiled.copy(function = compiled.function.toMutableList().apply { set(at.index, replacement) })
    }

    fun sameShape(a: Compiled, b: Compiled): Boolean =
        a.function.size == b.function.size && a.program.size == b.program.size &&
            a.program.zip(b.program).all { (x, y) ->
                if (x is Command.Repeat) y is Command.Repeat && x.body.size == y.body.size else y !is Command.Repeat
            }

    /** Changing a loop's count and one body card is two edits; changing a block card is one. */
    fun differences(a: Compiled, b: Compiled): List<CardAddress> {
        require(sameShape(a, b))
        return cards(a).mapNotNull { (at, card) ->
            val other = cardAt(b, at)
            val same = if (card is Command.Repeat && other is Command.Repeat) card.times == other.times else card == other
            at.takeUnless { same }
        }
    }
}
