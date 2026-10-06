package codetrail.desktop.ui

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import codetrail.core.model.Cell
import codetrail.core.model.Level
import codetrail.core.model.Pos
import codetrail.desktop.HeroVisual
import codetrail.desktop.theme.Character
import codetrail.desktop.theme.WorldTheme
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
) {
    val density = LocalDensity.current
    val sprite = remember(character.id) {
        useResource(character.svgPath) { loadSvgPainter(it, density) }
    }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val cellPx = if (boardSize.width == 0) 0f else boardSize.width / level.grid.width.toFloat()

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
            .aspectRatio(level.grid.width / level.grid.height.toFloat())
            .clip(RoundedCornerShape(18.dp))
            .onSizeChanged { boardSize = it },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / level.grid.width
            drawSea(level, theme, cell)
            drawLand(level, theme, cell)
            with(theme.art) {
                for (p in level.grid.positions()) {
                    if (level.grid[p] == Cell.OBSTACLE) drawObstacle(Offset(p.x * cell, p.y * cell), cell)
                }
                drawGoal(Offset((level.goal.x + 0.5f) * cell, (level.goal.y + 0.5f) * cell), cell)
            }
            drawHero(hero, sprite, cell)
        }
        if (cellPx > 0f) {
            val goalCenter = Offset((level.goal.x + 0.5f) * cellPx, (level.goal.y + 0.5f) * cellPx)
            CelebrationOverlay(trigger = if (won) effectKey else null, center = goalCenter, cell = cellPx, stars = stars)
            val fell = failure as? Failure.Fell
            SplashOverlay(
                trigger = if (fell != null) effectKey else null,
                center = fell?.let {
                    Offset(
                        ((it.at.x + 0.5f) * cellPx).coerceIn(cellPx * 0.1f, boardSize.width - cellPx * 0.1f),
                        ((it.at.y + 0.5f) * cellPx).coerceIn(cellPx * 0.1f, boardSize.height - cellPx * 0.1f),
                    )
                },
                cell = cellPx,
                color = theme.halo.copy(alpha = 1f),
            )
        }
    }
}

/** Stable per-cell randomness so decorations do not flicker between frames. */
private fun cellRandom(level: Level, p: Pos) = Random(level.seed xor (p.x * 73856093L) xor (p.y * 19349663L))

private fun DrawScope.drawSea(level: Level, theme: WorldTheme, cell: Float) {
    drawRect(Brush.verticalGradient(listOf(theme.seaTop, theme.seaBottom)), Offset.Zero, size)
    val w = cell * 0.022f
    for (i in 1 until level.grid.width) drawLine(theme.gridLine, Offset(i * cell, 0f), Offset(i * cell, size.height), w)
    for (j in 1 until level.grid.height) drawLine(theme.gridLine, Offset(0f, j * cell), Offset(size.width, j * cell), w)
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

        if (p != level.goal && level.grid[p] == Cell.WALKABLE) {
            with(theme.art) { drawLandProps(tl, cell, cellRandom(level, p)) }
        }
    }
}

private fun DrawScope.drawHero(hero: HeroVisual, sprite: Painter, cell: Float) {
    if (hero.alpha <= 0f) return
    val size = cell * 0.8f * hero.scale
    val cx = (hero.x + 0.5f) * cell
    val groundY = (hero.y + 0.5f) * cell
    val cy = groundY - hero.lift * cell
    // shadow stays on the ground and shrinks as the hero lifts or sinks
    val shadowScale = (1f - hero.lift.coerceIn(0f, 1f) * 0.4f) * hero.scale
    drawOval(
        Color.Black.copy(alpha = 0.18f * hero.alpha),
        Offset(cx - cell * 0.29f * shadowScale, groundY + cell * 0.22f),
        Size(cell * 0.58f * shadowScale, cell * 0.16f * shadowScale),
    )
    rotate(hero.tilt, Offset(cx, cy)) {
        translate(cx - size / 2, cy - size / 2) {
            with(sprite) { draw(Size(size, size), alpha = hero.alpha) }
        }
    }
    // heading badge on the sprite edge
    if (hero.scale > 0.6f) {
        rotate(hero.dirDegrees, Offset(cx, cy)) {
            val tip = Offset(cx, cy - cell * 0.5f * hero.scale)
            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - cell * 0.12f, tip.y + cell * 0.16f)
                lineTo(tip.x + cell * 0.12f, tip.y + cell * 0.16f)
                close()
            }
            drawPath(path, Color(0xFF2B1B14).copy(alpha = hero.alpha))
            drawPath(path, Color.White.copy(alpha = hero.alpha), style = Stroke(cell * 0.025f))
        }
    }
}
