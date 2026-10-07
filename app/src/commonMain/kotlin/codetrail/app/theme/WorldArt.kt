package codetrail.app.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * World-specific drawings. Terrain colours live in [WorldTheme]; everything that has a
 * recognisable shape (goal, obstacle, scattered props) lives here so each world feels different.
 *
 * All coordinates are in pixels, [cell] is the tile size. [rnd] is seeded per tile so props
 * stay put between frames.
 */
interface WorldArt {
    /** Goal object centred in its tile. */
    fun DrawScope.drawGoal(center: Offset, cell: Float)

    /** Thing standing on a walkable tile that must be jumped over. */
    fun DrawScope.drawObstacle(tl: Offset, cell: Float)

    /** Small props on a walkable tile. Called once per tile. */
    fun DrawScope.drawLandProps(tl: Offset, cell: Float, rnd: Random)

    /** Decoration on a BLOCKED tile (stars, canopy). Called once per tile. */
    fun DrawScope.drawSeaProps(tl: Offset, cell: Float, rnd: Random)

    /**
     * Drawn on every path tile after props, with which edges continue onto another path tile.
     * Lets a world draw road markings or rails that follow the corridor. Default: nothing.
     */
    fun DrawScope.drawConnections(tl: Offset, cell: Float, north: Boolean, east: Boolean, south: Boolean, west: Boolean) = Unit

    /** Drawn on every path tile right after the base fill, before props: bevels, inner faces, thick rims. Default: nothing. */
    fun DrawScope.drawTileFace(tl: Offset, cell: Float) = Unit
}

object IslandsArt : WorldArt {
    private val grass = Color(0xFF9ED47C)
    private val water = Color(0xFF5BB7E8)
    private val waterEdge = Color(0xFF2F7FB5)
    private val trunk = Color(0xFF7A4B23)
    private val frond = Color(0xFF3E9B4F)
    private val tuft = Color(0xFF5CB85C)
    private val shell = Color(0xFFF4B6C2)
    private val shellEdge = Color(0xFFD98AA0)
    private val wood = Color(0xFF8B5A2B)
    private val woodDark = Color(0xFF5A3A1C)
    private val cross = Color(0xFFE53935)

    override fun DrawScope.drawGoal(center: Offset, cell: Float) {
        val (cx, cy) = center
        drawOval(grass, Offset(cx - cell * 0.42f, cy - cell * 0.22f), Size(cell * 0.84f, cell * 0.58f))
        drawOval(water, Offset(cx - cell * 0.31f, cy - cell * 0.08f), Size(cell * 0.58f, cell * 0.34f))
        drawOval(waterEdge, Offset(cx - cell * 0.31f, cy - cell * 0.08f), Size(cell * 0.58f, cell * 0.34f), style = Stroke(cell * 0.022f))

        val base = Offset(cx + cell * 0.2f, cy + cell * 0.07f)
        val top = Offset(cx + cell * 0.22f, cy - cell * 0.38f)
        drawPath(
            Path().apply { moveTo(base.x, base.y); quadraticTo(base.x + cell * 0.06f, cy - cell * 0.2f, top.x, top.y) },
            trunk, style = Stroke(cell * 0.055f, cap = StrokeCap.Round),
        )
        for (deg in intArrayOf(-60, -20, 20, 60)) {
            val a = (deg - 90.0) * PI / 180
            val ex = top.x + cell * 0.31f * cos(a).toFloat()
            val ey = top.y + cell * 0.2f * sin(a).toFloat()
            drawPath(
                Path().apply { moveTo(top.x, top.y); quadraticTo((top.x + ex) / 2, ey - cell * 0.15f, ex, ey) },
                frond, style = Stroke(cell * 0.065f, cap = StrokeCap.Round),
            )
        }
    }

    override fun DrawScope.drawObstacle(tl: Offset, cell: Float) {
        val w = cell * 0.56f
        val h = cell * 0.4f
        val x = tl.x + (cell - w) / 2
        val y = tl.y + cell * 0.2f
        drawRect(wood, Offset(x + w * 0.2f, y + h), Size(cell * 0.065f, cell * 0.24f))
        drawRect(wood, Offset(x + w * 0.74f, y + h), Size(cell * 0.065f, cell * 0.24f))
        drawRoundRect(wood, Offset(x, y), Size(w, h), CornerRadius(cell * 0.06f))
        drawRoundRect(woodDark, Offset(x, y), Size(w, h), CornerRadius(cell * 0.06f), style = Stroke(cell * 0.03f))
        val inset = cell * 0.1f
        val width = cell * 0.065f
        drawLine(cross, Offset(x + inset, y + inset), Offset(x + w - inset, y + h - inset), width, StrokeCap.Round)
        drawLine(cross, Offset(x + w - inset, y + inset), Offset(x + inset, y + h - inset), width, StrokeCap.Round)
    }

    override fun DrawScope.drawLandProps(tl: Offset, cell: Float, rnd: Random) {
        if (rnd.nextFloat() < 0.6f) {
            val gx = tl.x + cell * (0.15f + rnd.nextFloat() * 0.62f)
            val gy = tl.y + cell * (0.66f + rnd.nextFloat() * 0.22f)
            val w = cell * 0.033f
            drawLine(tuft, Offset(gx, gy), Offset(gx - cell * 0.045f, gy - cell * 0.155f), w, StrokeCap.Round)
            drawLine(tuft, Offset(gx, gy), Offset(gx, gy - cell * 0.2f), w, StrokeCap.Round)
            drawLine(tuft, Offset(gx, gy), Offset(gx + cell * 0.055f, gy - cell * 0.145f), w, StrokeCap.Round)
        }
        if (rnd.nextFloat() < 0.3f) {
            val c = Offset(tl.x + cell * (0.15f + rnd.nextFloat() * 0.7f), tl.y + cell * (0.15f + rnd.nextFloat() * 0.3f))
            drawCircle(shell, cell * 0.045f, c)
            drawCircle(shellEdge, cell * 0.045f, c, style = Stroke(cell * 0.016f))
        }
    }

    override fun DrawScope.drawSeaProps(tl: Offset, cell: Float, rnd: Random) = Unit
}

object ForestArt : WorldArt {
    private val bush = Color(0xFF4CAF50)
    private val bushDark = Color(0xFF388E3C)
    private val berry = Color(0xFFE53935)
    private val berryShine = Color(0xFFFFCDD2)
    private val capRed = Color(0xFFD84315)
    private val stem = Color(0xFFF5E6CC)
    private val petal = Color(0xFFFFFFFF)
    private val pollen = Color(0xFFFFC107)
    private val leaf = Color(0xFFC0792F)
    private val canopy = Color(0x2E163F1A)
    private val canopyLight = Color(0x265FB95F)
    private val rock = Color(0xFF8D8D8D)
    private val rockDark = Color(0xFF616161)
    private val rockLight = Color(0xFFBDBDBD)

    override fun DrawScope.drawGoal(center: Offset, cell: Float) {
        val (cx, cy) = center
        drawCircle(bushDark, cell * 0.3f, Offset(cx - cell * 0.16f, cy + cell * 0.06f))
        drawCircle(bushDark, cell * 0.3f, Offset(cx + cell * 0.16f, cy + cell * 0.06f))
        drawCircle(bush, cell * 0.3f, Offset(cx, cy - cell * 0.08f))
        drawCircle(bush, cell * 0.24f, Offset(cx - cell * 0.18f, cy + cell * 0.08f))
        drawCircle(bush, cell * 0.24f, Offset(cx + cell * 0.18f, cy + cell * 0.08f))
        val berries = listOf(
            Offset(-0.12f, -0.14f), Offset(0.1f, -0.2f), Offset(0.22f, 0.04f),
            Offset(-0.24f, 0.08f), Offset(0.0f, 0.1f), Offset(-0.04f, -0.02f),
        )
        for (b in berries) {
            val p = Offset(cx + b.x * cell, cy + b.y * cell)
            drawCircle(berry, cell * 0.055f, p)
            drawCircle(berryShine, cell * 0.018f, p + Offset(-cell * 0.018f, -cell * 0.018f))
        }
    }

    override fun DrawScope.drawObstacle(tl: Offset, cell: Float) {
        val cx = tl.x + cell / 2
        val cy = tl.y + cell * 0.56f
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(cx - cell * 0.3f, cy + cell * 0.14f), Size(cell * 0.6f, cell * 0.14f))
        drawCircle(rockDark, cell * 0.3f, Offset(cx, cy))
        drawCircle(rock, cell * 0.26f, Offset(cx - cell * 0.03f, cy - cell * 0.04f))
        drawCircle(rockLight, cell * 0.08f, Offset(cx - cell * 0.1f, cy - cell * 0.14f))
    }

    override fun DrawScope.drawLandProps(tl: Offset, cell: Float, rnd: Random) {
        when (rnd.nextInt(5)) {
            0, 1 -> mushroom(Offset(tl.x + cell * (0.2f + rnd.nextFloat() * 0.6f), tl.y + cell * (0.55f + rnd.nextFloat() * 0.25f)), cell)
            2 -> flower(Offset(tl.x + cell * (0.2f + rnd.nextFloat() * 0.6f), tl.y + cell * (0.2f + rnd.nextFloat() * 0.6f)), cell)
            3 -> {
                val c = Offset(tl.x + cell * (0.2f + rnd.nextFloat() * 0.6f), tl.y + cell * (0.2f + rnd.nextFloat() * 0.6f))
                drawOval(leaf, Offset(c.x - cell * 0.07f, c.y - cell * 0.04f), Size(cell * 0.14f, cell * 0.08f))
            }
            else -> Unit
        }
    }

    private fun DrawScope.mushroom(foot: Offset, cell: Float) {
        drawRoundRect(stem, Offset(foot.x - cell * 0.04f, foot.y - cell * 0.14f), Size(cell * 0.08f, cell * 0.14f), CornerRadius(cell * 0.02f))
        drawArc(capRed, 180f, 180f, true, Offset(foot.x - cell * 0.11f, foot.y - cell * 0.24f), Size(cell * 0.22f, cell * 0.2f))
        drawCircle(petal, cell * 0.02f, Offset(foot.x - cell * 0.04f, foot.y - cell * 0.18f))
        drawCircle(petal, cell * 0.016f, Offset(foot.x + cell * 0.04f, foot.y - cell * 0.2f))
    }

    private fun DrawScope.flower(c: Offset, cell: Float) {
        for (i in 0 until 5) {
            val a = (i * 72.0 - 90) * PI / 180
            drawCircle(petal, cell * 0.035f, Offset(c.x + cell * 0.05f * cos(a).toFloat(), c.y + cell * 0.05f * sin(a).toFloat()))
        }
        drawCircle(pollen, cell * 0.03f, c)
    }

    override fun DrawScope.drawSeaProps(tl: Offset, cell: Float, rnd: Random) {
        // Tree canopy: one soft round shadow on roughly half the tiles, so the grid stays readable.
        if (rnd.nextFloat() < 0.55f) {
            val c = Offset(tl.x + cell * (0.25f + rnd.nextFloat() * 0.5f), tl.y + cell * (0.25f + rnd.nextFloat() * 0.5f))
            val r = cell * (0.16f + rnd.nextFloat() * 0.1f)
            drawCircle(canopy, r, c)
            drawCircle(canopyLight, r * 0.5f, c + Offset(-r * 0.25f, -r * 0.25f))
        }
    }
}

object SpaceArt : WorldArt {
    private val hull = Color(0xFFECEFF1)
    private val hullShade = Color(0xFFB0BEC5)
    private val nose = Color(0xFFEF5350)
    private val fin = Color(0xFFEF5350)
    private val window = Color(0xFF4FC3F7)
    private val windowRim = Color(0xFF37474F)
    private val flame = Color(0xFFFFA726)
    private val flameCore = Color(0xFFFFEE58)
    private val crater = Color(0xFF6E7799)
    private val craterLight = Color(0xFFCAD1F0)
    private val crystal = Color(0xFF80DEEA)
    private val crystalDark = Color(0xFF26A69A)
    private val star = Color(0xFFFFFFFF)
    private val asteroid = Color(0xFF7B6A5A)
    private val asteroidDark = Color(0xFF55473B)
    private val asteroidLight = Color(0xFFA08C78)

    override fun DrawScope.drawGoal(center: Offset, cell: Float) {
        val (cx, cy) = center
        val w = cell * 0.3f
        val h = cell * 0.5f
        val top = cy - cell * 0.42f
        // flame
        drawPath(
            Path().apply {
                moveTo(cx - w * 0.35f, top + h)
                quadraticTo(cx, top + h + cell * 0.36f, cx + w * 0.35f, top + h)
                close()
            },
            flame,
        )
        drawPath(
            Path().apply {
                moveTo(cx - w * 0.18f, top + h)
                quadraticTo(cx, top + h + cell * 0.2f, cx + w * 0.18f, top + h)
                close()
            },
            flameCore,
        )
        // fins
        drawPath(Path().apply { moveTo(cx - w / 2, top + h * 0.55f); lineTo(cx - w / 2 - cell * 0.14f, top + h); lineTo(cx - w / 2, top + h); close() }, fin)
        drawPath(Path().apply { moveTo(cx + w / 2, top + h * 0.55f); lineTo(cx + w / 2 + cell * 0.14f, top + h); lineTo(cx + w / 2, top + h); close() }, fin)
        // body
        drawRoundRect(hull, Offset(cx - w / 2, top), Size(w, h), CornerRadius(cell * 0.08f))
        drawRect(hullShade, Offset(cx + w * 0.2f, top + cell * 0.08f), Size(w * 0.3f, h - cell * 0.1f))
        // nose
        drawPath(Path().apply { moveTo(cx - w / 2, top + cell * 0.06f); quadraticTo(cx, top - cell * 0.22f, cx + w / 2, top + cell * 0.06f); close() }, nose)
        // window
        drawCircle(windowRim, cell * 0.085f, Offset(cx, top + h * 0.38f))
        drawCircle(window, cell * 0.06f, Offset(cx, top + h * 0.38f))
    }

    override fun DrawScope.drawObstacle(tl: Offset, cell: Float) {
        val cx = tl.x + cell / 2
        val cy = tl.y + cell * 0.5f
        val path = Path().apply {
            moveTo(cx - cell * 0.3f, cy - cell * 0.05f)
            lineTo(cx - cell * 0.15f, cy - cell * 0.28f)
            lineTo(cx + cell * 0.12f, cy - cell * 0.3f)
            lineTo(cx + cell * 0.3f, cy - cell * 0.08f)
            lineTo(cx + cell * 0.24f, cy + cell * 0.2f)
            lineTo(cx - cell * 0.02f, cy + cell * 0.3f)
            lineTo(cx - cell * 0.26f, cy + cell * 0.16f)
            close()
        }
        drawPath(path, asteroid)
        drawPath(path, asteroidDark, style = Stroke(cell * 0.03f))
        drawCircle(asteroidDark, cell * 0.06f, Offset(cx - cell * 0.1f, cy))
        drawCircle(asteroidDark, cell * 0.04f, Offset(cx + cell * 0.12f, cy + cell * 0.1f))
        drawCircle(asteroidLight, cell * 0.03f, Offset(cx + cell * 0.08f, cy - cell * 0.14f))
    }

    override fun DrawScope.drawLandProps(tl: Offset, cell: Float, rnd: Random) {
        when (rnd.nextInt(4)) {
            0, 1 -> {
                // crater: light raised rim, dark bowl offset down-right, tiny highlight
                val c = Offset(tl.x + cell * (0.22f + rnd.nextFloat() * 0.56f), tl.y + cell * (0.22f + rnd.nextFloat() * 0.56f))
                val r = cell * (0.08f + rnd.nextFloat() * 0.05f)
                drawCircle(craterLight, r, c)
                drawCircle(crater, r * 0.78f, c + Offset(r * 0.1f, r * 0.12f))
                drawCircle(craterLight.copy(alpha = 0.6f), r * 0.2f, c + Offset(-r * 0.3f, -r * 0.3f))
            }
            2 -> {
                val c = Offset(tl.x + cell * (0.2f + rnd.nextFloat() * 0.6f), tl.y + cell * (0.35f + rnd.nextFloat() * 0.45f))
                val h = cell * 0.24f
                val w = cell * 0.1f
                drawPath(Path().apply { moveTo(c.x, c.y - h); lineTo(c.x + w, c.y); lineTo(c.x, c.y + h * 0.3f); lineTo(c.x - w, c.y); close() }, crystal)
                drawPath(Path().apply { moveTo(c.x, c.y - h); lineTo(c.x + w, c.y); lineTo(c.x, c.y + h * 0.3f); close() }, crystalDark)
            }
            else -> Unit
        }
    }

    override fun DrawScope.drawSeaProps(tl: Offset, cell: Float, rnd: Random) {
        repeat(3) {
            val c = Offset(tl.x + rnd.nextFloat() * cell, tl.y + rnd.nextFloat() * cell)
            drawCircle(star.copy(alpha = 0.35f + rnd.nextFloat() * 0.55f), cell * (0.012f + rnd.nextFloat() * 0.014f), c)
        }
        if (rnd.nextFloat() < 0.25f) {
            val c = Offset(tl.x + cell * (0.2f + rnd.nextFloat() * 0.6f), tl.y + cell * (0.2f + rnd.nextFloat() * 0.6f))
            val r = cell * 0.07f
            val w = cell * 0.014f
            drawLine(star, Offset(c.x - r, c.y), Offset(c.x + r, c.y), w, StrokeCap.Round)
            drawLine(star, Offset(c.x, c.y - r), Offset(c.x, c.y + r), w, StrokeCap.Round)
        }
    }
}


object IceArt : WorldArt {
    private val snow = Color(0xFFFFFFFF)
    private val floeTop = Color(0xFFF4FAFF)
    private val floeEdge = Color(0xFFBBDDF0)
    private val floeUnder = Color(0xFF9CCDE8)
    private val snowPatch = Color(0xFFDDEFF8)
    private val wave = Color(0x80FFFFFF)
    private val chunk = Color(0xD9FFFFFF)
    private val iglooShade = Color(0xFFD6E9F3)
    private val iglooLine = Color(0xFF9CC7DE)
    private val door = Color(0xFF2E5E7A)
    private val spike = Color(0xFFA9DDF3)
    private val spikeDark = Color(0xFF5FA8CF)
    private val outline = Color(0xFF2B4A5E)

    override fun DrawScope.drawGoal(center: Offset, cell: Float) {
        val (cx, cy) = center
        val r = cell * 0.36f
        drawArc(snow, 180f, 180f, true, Offset(cx - r, cy - r * 0.55f), Size(r * 2, r * 1.6f))
        drawArc(outline, 180f, 180f, false, Offset(cx - r, cy - r * 0.55f), Size(r * 2, r * 1.6f), style = Stroke(cell * 0.03f))
        drawRect(snow, Offset(cx - r, cy + r * 0.25f), Size(r * 2, cell * 0.06f))
        for (i in 1..2) {
            val y = cy - r * 0.55f + r * 0.8f * i / 3f + r * 0.1f
            drawLine(iglooLine, Offset(cx - r * (0.6f + 0.2f * i), y), Offset(cx + r * (0.6f + 0.2f * i), y), cell * 0.018f)
        }
        drawLine(iglooLine, Offset(cx - r * 0.3f, cy - r * 0.45f), Offset(cx - r * 0.25f, cy + r * 0.25f), cell * 0.018f)
        drawLine(iglooLine, Offset(cx + r * 0.3f, cy - r * 0.45f), Offset(cx + r * 0.25f, cy + r * 0.25f), cell * 0.018f)
        drawArc(iglooShade, 180f, 180f, true, Offset(cx - r * 0.34f, cy - r * 0.1f), Size(r * 0.68f, r * 0.8f))
        drawArc(door, 180f, 180f, true, Offset(cx - r * 0.2f, cy + r * 0.02f), Size(r * 0.4f, r * 0.5f))
        drawArc(outline, 180f, 180f, false, Offset(cx - r * 0.34f, cy - r * 0.1f), Size(r * 0.68f, r * 0.8f), style = Stroke(cell * 0.025f))
    }

    override fun DrawScope.drawObstacle(tl: Offset, cell: Float) {
        val bx = tl.x + cell / 2
        val by = tl.y + cell * 0.78f
        fun spikeAt(dx: Float, h: Float, w: Float) {
            val p = Path().apply { moveTo(bx + dx - w, by); lineTo(bx + dx, by - h); lineTo(bx + dx + w, by); close() }
            drawPath(p, spike)
            val shade = Path().apply { moveTo(bx + dx, by - h); lineTo(bx + dx + w, by); lineTo(bx + dx, by); close() }
            drawPath(shade, spikeDark)
            drawPath(p, outline, style = Stroke(cell * 0.03f))
        }
        spikeAt(-cell * 0.18f, cell * 0.34f, cell * 0.11f)
        spikeAt(cell * 0.16f, cell * 0.4f, cell * 0.12f)
        spikeAt(0f, cell * 0.56f, cell * 0.14f)
    }

    /** Jagged floe: blue underside offset down, white top. Shape alternates per tile so the path is not a stamp. */
    override fun DrawScope.drawTileFace(tl: Offset, cell: Float) {
        val variant = ((tl.x / cell).toInt() + (tl.y / cell).toInt()) % 2
        val pts = if (variant == 0) listOf(
            0.07f to 0.16f, 0.24f to 0.05f, 0.64f to 0.07f, 0.93f to 0.18f, 0.95f to 0.55f, 0.86f to 0.91f, 0.49f to 0.95f, 0.11f to 0.87f, 0.05f to 0.49f,
        ) else listOf(
            0.05f to 0.3f, 0.16f to 0.07f, 0.52f to 0.04f, 0.88f to 0.1f, 0.96f to 0.46f, 0.9f to 0.86f, 0.6f to 0.96f, 0.2f to 0.92f, 0.06f to 0.7f,
        )
        val floe = Path().apply {
            pts.forEachIndexed { i, (fx, fy) -> if (i == 0) moveTo(tl.x + fx * cell, tl.y + fy * cell) else lineTo(tl.x + fx * cell, tl.y + fy * cell) }
            close()
        }
        translate(0f, cell * 0.065f) { drawPath(floe, floeUnder) }
        drawPath(floe, floeTop)
        drawPath(floe, floeEdge, style = Stroke(cell * 0.022f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }

    override fun DrawScope.drawLandProps(tl: Offset, cell: Float, rnd: Random) {
        if (rnd.nextFloat() < 0.6f) {
            val c = Offset(tl.x + cell * (0.3f + rnd.nextFloat() * 0.4f), tl.y + cell * (0.3f + rnd.nextFloat() * 0.4f))
            drawOval(snowPatch, Offset(c.x - cell * 0.13f, c.y - cell * 0.075f), Size(cell * 0.26f, cell * 0.15f))
        }
    }

    override fun DrawScope.drawSeaProps(tl: Offset, cell: Float, rnd: Random) {
        repeat(2) {
            val x = tl.x + cell * (0.1f + rnd.nextFloat() * 0.6f)
            val y = tl.y + cell * (0.15f + rnd.nextFloat() * 0.7f)
            val w = cell * 0.13f
            val p = Path().apply {
                moveTo(x, y)
                quadraticTo(x + w / 2, y - cell * 0.055f, x + w, y)
                quadraticTo(x + w * 1.5f, y + cell * 0.055f, x + w * 2, y)
            }
            drawPath(p, wave, style = Stroke(cell * 0.028f, cap = StrokeCap.Round))
        }
        if (rnd.nextFloat() < 0.28f) {
            val c = Offset(tl.x + cell * (0.2f + rnd.nextFloat() * 0.6f), tl.y + cell * (0.2f + rnd.nextFloat() * 0.6f))
            val s = cell * 0.13f
            val p = Path().apply {
                moveTo(c.x - s, c.y + s * 0.35f); lineTo(c.x - s * 0.5f, c.y - s * 0.65f); lineTo(c.x + s * 0.65f, c.y - s * 0.5f); lineTo(c.x + s, c.y + s * 0.35f); close()
            }
            drawPath(p, chunk)
        }
    }
}

object CityArt : WorldArt {
    private val wall = Color(0xFFF2C14E)
    private val roof = Color(0xFFD9534F)
    private val doorC = Color(0xFF6D4C41)
    private val window = Color(0xFF81D4FA)
    private val cone = Color(0xFFFF7043)
    private val coneStripe = Color(0xFFFFFFFF)
    private val dash = Color(0xFFFFF8E1)
    private val blockRoof = Color(0xFF8D6E63)
    private val blockDoor = Color(0xFF5D4037)
    private val blockWalls = listOf(Color(0xFFEF9A9A), Color(0xFF80CBC4), Color(0xFFFFE082), Color(0xFFB39DDB), Color(0xFF90CAF9))
    private val treeTop = Color(0xFF4E9A51)
    private val trunk = Color(0xFF6D4C41)
    private val outline = Color(0xFF2B2B2B)

    /** The goal house: same toy-block shape as the neighbourhood, but with a red roof so it stands out. */
    override fun DrawScope.drawGoal(center: Offset, cell: Float) {
        val (cx, cy) = center
        house(cx, cy, cell, wall, roof, cell * 0.03f, withWindows = true)
    }

    private fun DrawScope.house(cx: Float, cy: Float, cell: Float, wallColor: Color, roofColor: Color, lw: Float, withWindows: Boolean) {
        val w = cell * 0.52f
        val h = cell * 0.36f
        val top = cy - cell * 0.06f
        drawRect(wallColor, Offset(cx - w / 2, top), Size(w, h))
        drawRect(outline, Offset(cx - w / 2, top), Size(w, h), style = Stroke(lw))
        val roofPath = Path().apply { moveTo(cx - w * 0.58f, top); lineTo(cx, top - cell * 0.26f); lineTo(cx + w * 0.58f, top); close() }
        drawPath(roofPath, roofColor)
        drawPath(roofPath, outline, style = Stroke(lw))
        drawRect(doorC, Offset(cx - w * 0.12f, top + h * 0.45f), Size(w * 0.24f, h * 0.55f))
        if (withWindows) {
            drawRect(window, Offset(cx - w * 0.42f, top + h * 0.18f), Size(w * 0.2f, h * 0.3f))
            drawRect(window, Offset(cx + w * 0.22f, top + h * 0.18f), Size(w * 0.2f, h * 0.3f))
        }
    }

    override fun DrawScope.drawObstacle(tl: Offset, cell: Float) {
        val cx = tl.x + cell / 2
        val by = tl.y + cell * 0.78f
        drawRoundRect(cone, Offset(cx - cell * 0.26f, by - cell * 0.06f), Size(cell * 0.52f, cell * 0.08f), CornerRadius(cell * 0.02f))
        val p = Path().apply { moveTo(cx - cell * 0.16f, by - cell * 0.06f); lineTo(cx - cell * 0.05f, by - cell * 0.56f); lineTo(cx + cell * 0.05f, by - cell * 0.56f); lineTo(cx + cell * 0.16f, by - cell * 0.06f); close() }
        drawPath(p, cone)
        drawRect(coneStripe, Offset(cx - cell * 0.11f, by - cell * 0.3f), Size(cell * 0.22f, cell * 0.07f))
        drawPath(p, outline, style = Stroke(cell * 0.03f))
    }

    /** Wooden road tiles carry no props: the dashed centre line is drawn by [drawConnections]. */
    override fun DrawScope.drawLandProps(tl: Offset, cell: Float, rnd: Random) = Unit

    override fun DrawScope.drawConnections(tl: Offset, cell: Float, north: Boolean, east: Boolean, south: Boolean, west: Boolean) {
        val cx = tl.x + cell / 2
        val cy = tl.y + cell / 2
        val w = cell * 0.05f
        val dashLen = cell * 0.13f
        val gap = cell * 0.13f
        val effect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dashLen, gap), 0f)
        fun seg(to: Offset) = drawLine(dash, Offset(cx, cy), to, w, StrokeCap.Round, effect)
        if (north) seg(Offset(cx, tl.y))
        if (south) seg(Offset(cx, tl.y + cell))
        if (west) seg(Offset(tl.x, cy))
        if (east) seg(Offset(tl.x + cell, cy))
        if (!north && !south && !west && !east) drawCircle(dash, w, Offset(cx, cy))
    }

    /** Lawn tiles: every third one gets a toy-block house, the rest a lollipop tree. */
    override fun DrawScope.drawSeaProps(tl: Offset, cell: Float, rnd: Random) {
        val roll = rnd.nextInt(10)
        when {
            roll < 4 -> {
                val cx = tl.x + cell / 2
                val cy = tl.y + cell * 0.58f
                house(cx, cy, cell * 0.78f, blockWalls[rnd.nextInt(blockWalls.size)], blockRoof, cell * 0.025f, withWindows = false)
            }
            roll < 8 -> {
                val c = Offset(tl.x + cell * (0.3f + rnd.nextFloat() * 0.4f), tl.y + cell * (0.3f + rnd.nextFloat() * 0.4f))
                val r = cell * 0.17f
                drawRect(trunk, Offset(c.x - cell * 0.03f, c.y + r * 0.7f), Size(cell * 0.06f, cell * 0.12f))
                drawCircle(treeTop, r, c)
                drawCircle(outline, r, c, style = Stroke(cell * 0.025f))
            }
            else -> Unit
        }
    }
}

object LavaArt : WorldArt {
    private val chest = Color(0xFF8D5524)
    private val chestDark = Color(0xFF5D3714)
    private val gold = Color(0xFFFFD54F)
    private val goldDark = Color(0xFFF9A825)
    private val gem = Color(0xFF4FC3F7)
    private val glow = Color(0xFFFF9E40)
    private val slab = Color(0xFF56505E)
    private val slabShine = Color(0xFF6E6878)
    private val rock = Color(0xFF6B6472)
    private val rockDark = Color(0xFF3F3944)
    private val rockCrack = Color(0xFFFF6D2E)
    private val streakDark = Color(0x73D9381E)
    private val streakLight = Color(0x80FFE08A)
    private val crust = Color(0xD93A2420)
    private val outline = Color(0xFF1E1418)

    override fun DrawScope.drawGoal(center: Offset, cell: Float) {
        val (cx, cy) = center
        val w = cell * 0.56f
        val h = cell * 0.34f
        val top = cy - cell * 0.1f
        drawCircle(glow.copy(alpha = 0.35f), cell * 0.4f, Offset(cx, cy + cell * 0.05f))
        drawRoundRect(chestDark, Offset(cx - w / 2, top - h * 0.5f), Size(w, h * 0.6f), CornerRadius(cell * 0.06f))
        drawRoundRect(chest, Offset(cx - w / 2, top), Size(w, h), CornerRadius(cell * 0.04f))
        drawRect(chestDark, Offset(cx - w / 2, top + h * 0.45f), Size(w, h * 0.1f))
        for (i in -2..2) drawCircle(if (i % 2 == 0) gold else goldDark, cell * 0.065f, Offset(cx + i * cell * 0.09f, top - cell * 0.02f))
        for (i in -1..1) drawCircle(gold, cell * 0.06f, Offset(cx + i * cell * 0.09f, top - cell * 0.09f))
        val gemPath = Path().apply { moveTo(cx, top - cell * 0.24f); lineTo(cx + cell * 0.07f, top - cell * 0.14f); lineTo(cx, top - cell * 0.06f); lineTo(cx - cell * 0.07f, top - cell * 0.14f); close() }
        drawPath(gemPath, gem)
        drawRoundRect(outline, Offset(cx - w / 2, top), Size(w, h), CornerRadius(cell * 0.04f), style = Stroke(cell * 0.03f))
        drawRoundRect(outline, Offset(cx - w / 2, top - h * 0.5f), Size(w, h * 0.6f), CornerRadius(cell * 0.06f), style = Stroke(cell * 0.03f))
        drawRect(gold, Offset(cx - cell * 0.04f, top + h * 0.4f), Size(cell * 0.08f, h * 0.22f))
    }

    /** Volcanic boulder with glowing cracks. */
    override fun DrawScope.drawObstacle(tl: Offset, cell: Float) {
        val cx = tl.x + cell / 2
        val cy = tl.y + cell * 0.52f
        val path = Path().apply {
            moveTo(cx - cell * 0.3f, cy + cell * 0.1f); lineTo(cx - cell * 0.22f, cy - cell * 0.22f); lineTo(cx + cell * 0.05f, cy - cell * 0.3f)
            lineTo(cx + cell * 0.3f, cy - cell * 0.08f); lineTo(cx + cell * 0.22f, cy + cell * 0.24f); lineTo(cx - cell * 0.1f, cy + cell * 0.28f); close()
        }
        drawPath(path, rock)
        drawPath(path, rockDark, style = Stroke(cell * 0.03f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        val cracks = Path().apply { moveTo(cx - cell * 0.12f, cy + cell * 0.1f); lineTo(cx - cell * 0.02f, cy); lineTo(cx + cell * 0.04f, cy + cell * 0.12f); moveTo(cx - cell * 0.02f, cy); lineTo(cx + cell * 0.12f, cy - cell * 0.1f) }
        drawPath(cracks, rockCrack, style = Stroke(cell * 0.03f, cap = StrokeCap.Round))
    }

    override fun DrawScope.drawLandProps(tl: Offset, cell: Float, rnd: Random) = Unit

    /** Lighter inner face with a soft wavy highlight, like a smooth basalt stepping stone. */
    override fun DrawScope.drawTileFace(tl: Offset, cell: Float) {
        val inset = cell * 0.07f
        drawRoundRect(slab, Offset(tl.x + inset, tl.y + inset), Size(cell - 2 * inset, cell - 2 * inset), CornerRadius(cell * 0.13f))
        val y = tl.y + cell * 0.3f
        val p = Path().apply {
            moveTo(tl.x + cell * 0.22f, y)
            quadraticTo(tl.x + cell * 0.36f, y + cell * 0.07f, tl.x + cell * 0.5f, y - cell * 0.02f)
            quadraticTo(tl.x + cell * 0.64f, y - cell * 0.1f, tl.x + cell * 0.78f, y)
        }
        drawPath(p, slabShine, style = Stroke(cell * 0.03f, cap = StrokeCap.Round))
    }

    /**
     * Flowing streaks: wave phase is tied to the tile's row, so lines run continuously across the board,
     * with a wave period of half a tile so edges meet.
     */
    override fun DrawScope.drawSeaProps(tl: Offset, cell: Float, rnd: Random) {
        val row = (tl.y / cell).toInt()
        val rowRnd = Random(row * 7919L + 17)
        repeat(3) { i ->
            val y0 = tl.y + cell * (0.12f + i * 0.3f + rowRnd.nextFloat() * 0.12f)
            val amp = cell * (0.05f + rowRnd.nextFloat() * 0.06f)
            val width = cell * (0.045f + rowRnd.nextFloat() * 0.05f)
            drawWave(tl.x, y0, cell, amp, streakDark, width)
            drawWave(tl.x, y0 + amp + cell * 0.07f, cell, amp, streakLight, cell * 0.03f)
        }
        if (rnd.nextFloat() < 0.22f) {
            val c = Offset(tl.x + cell * (0.25f + rnd.nextFloat() * 0.5f), tl.y + cell * (0.25f + rnd.nextFloat() * 0.5f))
            val rx = cell * (0.14f + rnd.nextFloat() * 0.12f)
            val ry = cell * (0.09f + rnd.nextFloat() * 0.05f)
            drawOval(crust, Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2))
        }
    }

    private fun DrawScope.drawWave(x0: Float, y: Float, cell: Float, amp: Float, color: Color, width: Float) {
        val half = cell / 2
        val p = Path().apply {
            moveTo(x0, y)
            quadraticTo(x0 + half / 2, y - amp, x0 + half, y)
            quadraticTo(x0 + half * 1.5f, y - amp, x0 + cell, y)
        }
        drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round))
    }
}
