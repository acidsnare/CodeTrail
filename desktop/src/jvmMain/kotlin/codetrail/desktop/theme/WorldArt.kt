package codetrail.desktop.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
            val a = Math.toRadians(deg - 90.0)
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
            val a = Math.toRadians(i * 72.0 - 90)
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
                val c = Offset(tl.x + cell * (0.2f + rnd.nextFloat() * 0.6f), tl.y + cell * (0.2f + rnd.nextFloat() * 0.6f))
                val r = cell * (0.06f + rnd.nextFloat() * 0.05f)
                drawOval(crater, Offset(c.x - r, c.y - r * 0.7f), Size(r * 2, r * 1.4f))
                drawOval(craterLight, Offset(c.x - r * 0.8f, c.y - r * 0.9f), Size(r * 1.6f, r * 0.6f))
            }
            2 -> {
                val c = Offset(tl.x + cell * (0.2f + rnd.nextFloat() * 0.6f), tl.y + cell * (0.3f + rnd.nextFloat() * 0.5f))
                val h = cell * 0.16f
                val w = cell * 0.07f
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
