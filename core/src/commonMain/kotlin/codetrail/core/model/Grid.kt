package codetrail.core.model

/**
 * Semantic cell types. Rendering (water, trees, void) is decided by the world theme,
 * the engine only cares about these.
 */
enum class Cell {
    /** The hero can stand here. */
    WALKABLE,

    /** Impassable terrain: water, wall, void. Can be jumped over. */
    BLOCKED,

    /** An object standing on the path. Cannot be walked through, can be jumped over. */
    OBSTACLE,
}

class Grid(val width: Int, val height: Int, private val cells: Array<Cell>) {

    init {
        require(cells.size == width * height) { "Grid size mismatch" }
    }

    constructor(width: Int, height: Int, fill: Cell = Cell.BLOCKED) :
        this(width, height, Array(width * height) { fill })

    operator fun get(p: Pos): Cell {
        require(contains(p)) { "Position $p is outside ${width}x$height grid" }
        return cells[p.y * width + p.x]
    }

    operator fun set(p: Pos, c: Cell) {
        require(contains(p)) { "Position $p is outside ${width}x$height grid" }
        cells[p.y * width + p.x] = c
    }

    fun contains(p: Pos): Boolean = p.x in 0 until width && p.y in 0 until height

    fun cellOrNull(p: Pos): Cell? = if (contains(p)) this[p] else null

    fun isWalkable(p: Pos): Boolean = cellOrNull(p) == Cell.WALKABLE

    fun positions(): Sequence<Pos> = sequence {
        for (y in 0 until height) for (x in 0 until width) yield(Pos(x, y))
    }

    fun copy(): Grid = Grid(width, height, cells.copyOf())
}
