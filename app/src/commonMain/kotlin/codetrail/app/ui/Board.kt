package codetrail.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.offset
import codetrail.core.engine.Failure
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import codetrail.core.model.Cell
import codetrail.core.model.Level
import codetrail.core.model.Pos
import codetrail.app.HeroVisual
import codetrail.app.theme.Character
import codetrail.app.theme.WorldTheme
import kotlin.math.floor
import kotlin.random.Random

/**
 * @param effectKey changes once per finished run; drives one-shot confetti / splash / shake.
 * @param won whether the last run succeeded, [stars] how many it earned.
 * @param failure the last run's failure, used for the splash position and the shake.
 */
@Composable
fun Board(
    level: Level,
    hero: HeroVisual,
    theme: WorldTheme,
    character: Character,
    modifier: Modifier = Modifier,
    effectKey: Any? = null,
    won: Boolean = false,
    stars: Int = 0,
    failure: Failure? = null,
    guess: Pos? = null,
    answer: Pos? = null,
    onCellClick: ((Pos) -> Unit)? = null,
) {
    val sprite = characterPainter(character)
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    // Half a cell of water around the grid so tiles never touch the rounded frame.
    val cols = level.grid.width + BoardMargin * 2
    val rows = level.grid.height + BoardMargin * 2
    val cellPx = if (boardSize.width == 0) 0f else boardSize.width / cols
    val marginPx = cellPx * BoardMargin

    // Bump into an obstacle: short horizontal shake.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(effectKey) {
        if (effectKey != null && (failure is Failure.Bumped || failure is Failure.BadLanding)) {
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes {
                durationMillis = 380
                10f at 50
                -9f at 110
                7f at 170
                -5f at 230
                3f at 290
                0f at 380
            })
        }
    }

    Box(
        modifier
            .offset { IntOffset(shake.value.toInt(), 0) }
            .aspectRatio(cols / rows)
            .clip(RoundedCornerShape(18.dp))
            .onSizeChanged { boardSize = it }
            .then(
                if (onCellClick == null) Modifier else Modifier.pointerInput(level) {
                    detectTapGestures { tap ->
                        val c = size.width / cols
                        val x = floor((tap.x - c * BoardMargin) / c).toInt()
                        val y = floor((tap.y - c * BoardMargin) / c).toInt()
                        if (level.grid.contains(Pos(x, y))) onCellClick(Pos(x, y))
                    }
                },
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / cols
            val margin = cell * BoardMargin
            drawRect(Brush.verticalGradient(listOf(theme.seaTop, theme.seaBottom)), Offset.Zero, size)
            translate(margin, margin) {
                drawSea(level, theme, cell, margin)
                drawLand(level, theme, cell)
                with(theme.art) {
                    for (p in level.grid.positions()) {
                        if (level.grid[p] == Cell.OBSTACLE) drawObstacle(Offset(p.x * cell, p.y * cell), cell)
                    }
                    drawGoal(Offset((level.goal.x + 0.5f) * cell, (level.goal.y + 0.5f) * cell), cell)
                }
                if (guess != null) drawGuess(guess, cell, isAnswer = false)
                if (answer != null) drawGuess(answer, cell, isAnswer = true)
                drawHero(hero, sprite, character, cell)
            }
        }
        if (cellPx > 0f) {
            val boardCenter = Offset(boardSize.width / 2f, boardSize.height / 2f)
            CelebrationOverlay(trigger = if (won) effectKey else null, center = boardCenter, cell = cellPx, stars = stars)
            val fell = failure as? Failure.Fell
            SplashOverlay(
                trigger = if (fell != null) effectKey else null,
                center = fell?.let {
                    Offset(
                        (marginPx + (it.at.x + 0.5f) * cellPx).coerceIn(cellPx * 0.1f, boardSize.width - cellPx * 0.1f),
                        (marginPx + (it.at.y + 0.5f) * cellPx).coerceIn(cellPx * 0.1f, boardSize.height - cellPx * 0.1f),
                    )
                },
                cell = cellPx,
                color = theme.halo.copy(alpha = 1f),
            )
        }
    }
}

/** Water band around the grid, in cells. */
private const val BoardMargin = 0.5f

/** Stable per-cell randomness so decorations do not flicker between frames. */
private fun cellRandom(level: Level, p: Pos) = Random(level.seed xor (p.x * 73856093L) xor (p.y * 19349663L))

/** Grid lines and sea decorations. Called inside the margin translation; lines run out into the margin. */
private fun DrawScope.drawSea(level: Level, theme: WorldTheme, cell: Float, margin: Float) {
    val w = cell * 0.022f
    val right = size.width - margin
    val bottom = size.height - margin
    for (i in 0..level.grid.width) drawLine(theme.gridLine, Offset(i * cell, -margin), Offset(i * cell, bottom), w)
    for (j in 0..level.grid.height) drawLine(theme.gridLine, Offset(-margin, j * cell), Offset(right, j * cell), w)
    with(theme.art) {
        for (p in level.grid.positions()) {
            if (level.grid[p] == Cell.BLOCKED) drawSeaProps(Offset(p.x * cell, p.y * cell), cell, cellRandom(level, p))
        }
    }
}

private fun DrawScope.drawLand(level: Level, theme: WorldTheme, cell: Float) {
    val land = level.grid.positions().filter { level.grid[it] != Cell.BLOCKED }.toList()

    // Shallow water: inflated translucent squares overlap into one ring around every island.
    val haloPad = cell * 0.09f
    for (p in land) {
        drawRoundRect(
            theme.halo,
            Offset(p.x * cell - haloPad, p.y * cell - haloPad),
            Size(cell + 2 * haloPad, cell + 2 * haloPad),
            CornerRadius(cell * 0.22f),
        )
    }
    val radius = CornerRadius(cell * 0.155f)
    val rim = Stroke(cell * 0.022f)
    for (p in land) {
        val tl = Offset(p.x * cell, p.y * cell)
        drawRoundRect(theme.land, tl, Size(cell, cell), radius)
        drawRoundRect(theme.landEdge, tl, Size(cell, cell), radius, style = rim)
        with(theme.art) { drawTileFace(tl, cell) }

        if (p != level.goal && level.grid[p] == Cell.WALKABLE) {
            with(theme.art) { drawLandProps(tl, cell, cellRandom(level, p)) }
        }
        fun open(q: Pos) = level.grid.cellOrNull(q)?.let { it != Cell.BLOCKED } ?: false
        with(theme.art) {
            drawConnections(
                tl, cell,
                north = open(Pos(p.x, p.y - 1)), east = open(Pos(p.x + 1, p.y)),
                south = open(Pos(p.x, p.y + 1)), west = open(Pos(p.x - 1, p.y)),
            )
        }
    }
}

/** Predict mode markers: the player's guess as a question-mark ring, the true answer as a green ring. */
private fun DrawScope.drawGuess(p: Pos, cell: Float, isAnswer: Boolean) {
    val c = Offset((p.x + 0.5f) * cell, (p.y + 0.5f) * cell)
    val color = if (isAnswer) Color(0xFF43A047) else Color(0xFFFFD54F)
    drawRoundRect(color.copy(alpha = 0.25f), Offset(p.x * cell + cell * 0.06f, p.y * cell + cell * 0.06f), Size(cell * 0.88f, cell * 0.88f), CornerRadius(cell * 0.14f))
    drawRoundRect(color, Offset(p.x * cell + cell * 0.06f, p.y * cell + cell * 0.06f), Size(cell * 0.88f, cell * 0.88f), CornerRadius(cell * 0.14f), style = Stroke(cell * 0.06f))
    if (!isAnswer) {
        drawCircle(color, cell * 0.2f, c)
        drawCircle(Color(0xFF2B1B14), cell * 0.2f, c, style = Stroke(cell * 0.03f))
        // question mark
        val q = Path().apply {
            moveTo(c.x - cell * 0.07f, c.y - cell * 0.05f)
            quadraticTo(c.x - cell * 0.07f, c.y - cell * 0.14f, c.x, c.y - cell * 0.14f)
            quadraticTo(c.x + cell * 0.08f, c.y - cell * 0.14f, c.x + cell * 0.08f, c.y - cell * 0.05f)
            quadraticTo(c.x + cell * 0.08f, c.y + cell * 0.01f, c.x, c.y + cell * 0.03f)
            lineTo(c.x, c.y + cell * 0.07f)
        }
        drawPath(q, Color(0xFF2B1B14), style = Stroke(cell * 0.035f, cap = StrokeCap.Round))
        drawCircle(Color(0xFF2B1B14), cell * 0.022f, Offset(c.x, c.y + cell * 0.13f))
    }
}

/** Portrait sprite with shadow and a chunky heading chevron on its leading edge. */
private fun DrawScope.drawHero(hero: HeroVisual, sprite: Painter, c: Character, cell: Float) {
    if (hero.alpha <= 0f) return
    val k = hero.scale
    val a = hero.alpha
    val size = cell * 0.8f * k
    val cx = (hero.x + 0.5f) * cell
    val groundY = (hero.y + 0.5f) * cell
    val cy = groundY - hero.lift * cell

    // shadow stays on the ground and shrinks as the hero lifts or sinks
    val shadowScale = (1f - hero.lift.coerceIn(0f, 1f) * 0.4f) * k
    drawOval(
        Color.Black.copy(alpha = 0.18f * a),
        Offset(cx - cell * 0.29f * shadowScale, groundY + cell * 0.22f),
        Size(cell * 0.58f * shadowScale, cell * 0.16f * shadowScale),
    )

    rotate(hero.tilt, Offset(cx, cy)) {
        translate(cx - size / 2, cy - size / 2) {
            with(sprite) { draw(Size(size, size), alpha = a) }
        }
    }

    // heading chevron: white with dark outline, sitting on the sprite's edge
    if (k > 0.6f) {
        rotate(hero.dirDegrees, Offset(cx, cy)) {
            val tip = Offset(cx, cy - cell * 0.56f * k)
            val w = cell * 0.17f
            val h = cell * 0.2f
            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - w, tip.y + h)
                lineTo(tip.x, tip.y + h * 0.7f)
                lineTo(tip.x + w, tip.y + h)
                close()
            }
            drawPath(path, Color.White.copy(alpha = a))
            drawPath(path, Color(0xFF2B1B14).copy(alpha = a), style = Stroke(cell * 0.035f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}
