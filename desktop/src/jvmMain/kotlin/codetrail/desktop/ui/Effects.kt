package codetrail.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * One-shot particle effects drawn over the board. Particles are described by their launch
 * parameters and positioned analytically from elapsed time, so there is no per-frame mutation.
 */
private class Particle(
    val origin: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val spin: Float,
    val delay: Float,
    val shape: Int,
)

/** Drives [progress] from 0 to 1 over [durationMs] whenever [trigger] changes to a non-null value. */
@Composable
private fun rememberEffectClock(trigger: Any?, durationMs: Int): Float {
    var progress by remember(trigger) { mutableStateOf(if (trigger == null) 1f else 0f) }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            val t = withFrameNanos { it }
            val p = (t - start) / 1_000_000f / durationMs
            if (p >= 1f) { progress = 1f; break }
            progress = p
        }
    }
    return progress
}

private val confettiColors = listOf(
    Color(0xFFFFD54F), Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFFFF7043), Color(0xFFFFFFFF),
)

/** Confetti burst from [center] plus three stars popping up above it. */
@Composable
fun CelebrationOverlay(trigger: Any?, center: Offset?, cell: Float, stars: Int, modifier: Modifier = Modifier) {
    val durationMs = 2600
    val progress = rememberEffectClock(trigger, durationMs)
    val particles = remember(trigger) {
        val rnd = Random(trigger.hashCode())
        List(90) {
            val a = rnd.nextDouble(-Math.PI * 0.95, -Math.PI * 0.05)
            val speed = cell * (3.5f + rnd.nextFloat() * 5f)
            Particle(
                origin = Offset.Zero,
                velocity = Offset((cos(a) * speed).toFloat(), (sin(a) * speed).toFloat()),
                color = confettiColors[rnd.nextInt(confettiColors.size)],
                size = cell * (0.06f + rnd.nextFloat() * 0.08f),
                spin = (rnd.nextFloat() - 0.5f) * 1200f,
                delay = rnd.nextFloat() * 0.08f,
                shape = rnd.nextInt(3),
            )
        }
    }
    if (trigger == null || center == null || progress >= 1f) return

    Canvas(modifier.fillMaxSize()) {
        val tSec = progress * durationMs / 1000f
        val gravity = cell * 9f
        val fade = ((1f - progress) / 0.25f).coerceIn(0f, 1f)
        for (p in particles) {
            val t = (tSec - p.delay).coerceAtLeast(0f)
            val x = center.x + p.velocity.x * t
            val y = center.y + p.velocity.y * t + 0.5f * gravity * t * t
            rotate(p.spin * t, Offset(x, y)) {
                val c = p.color.copy(alpha = fade)
                when (p.shape) {
                    0 -> drawRect(c, Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
                    1 -> drawCircle(c, p.size / 2, Offset(x, y))
                    else -> drawRect(c, Offset(x - p.size / 2, y - p.size / 2), Size(p.size, p.size))
                }
            }
        }
        // stars pop in one after another above the goal
        for (i in 0 until 3) {
            val appear = 0.15f + i * 0.12f
            val local = ((progress - appear) / 0.12f).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val pop = overshoot(local)
            val lit = i < stars
            // Keep the trio inside the board: below the goal when it sits in the top row.
            val above = center.y > cell * 1.6f
            val baseX = center.x.coerceIn(cell * 1.0f, size.width - cell * 1.0f)
            val x = baseX + (i - 1) * cell * 0.75f
            val y = if (above) center.y - cell * 1.1f - (i % 2) * cell * 0.15f else center.y + cell * 1.1f + (i % 2) * cell * 0.15f
            drawStar(Offset(x, y), cell * 0.36f * pop, if (lit) Color(0xFFFFD54F) else Color(0x66FFFFFF), fade)
        }
    }
}

/** Water droplets flying out of the tile the hero fell into. */
@Composable
fun SplashOverlay(trigger: Any?, center: Offset?, cell: Float, color: Color, modifier: Modifier = Modifier) {
    val durationMs = 900
    val progress = rememberEffectClock(trigger, durationMs)
    val drops = remember(trigger) {
        val rnd = Random(trigger.hashCode() * 31)
        List(26) {
            val a = rnd.nextDouble(-Math.PI * 0.9, -Math.PI * 0.1)
            val speed = cell * (2f + rnd.nextFloat() * 3.5f)
            Particle(Offset.Zero, Offset((cos(a) * speed).toFloat(), (sin(a) * speed).toFloat()), color, cell * (0.05f + rnd.nextFloat() * 0.06f), 0f, rnd.nextFloat() * 0.05f, 1)
        }
    }
    if (trigger == null || center == null || progress >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val tSec = progress * durationMs / 1000f
        val gravity = cell * 12f
        val fade = ((1f - progress) / 0.3f).coerceIn(0f, 1f)
        // expanding ring on the water surface
        drawCircle(color.copy(alpha = 0.5f * (1f - progress)), cell * 0.2f + cell * 0.6f * progress, center, style = androidx.compose.ui.graphics.drawscope.Stroke(cell * 0.04f))
        for (d in drops) {
            val t = (tSec - d.delay).coerceAtLeast(0f)
            val x = center.x + d.velocity.x * t
            val y = center.y + d.velocity.y * t + 0.5f * gravity * t * t
            drawCircle(color.copy(alpha = fade), d.size / 2, Offset(x, y))
        }
    }
}

private fun overshoot(t: Float): Float {
    val s = 1.7f
    val x = t - 1f
    return x * x * ((s + 1) * x + s) + 1f
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (radius <= 0f) return
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val a = Math.toRadians(-90.0 + i * 36.0)
        val p = Offset(center.x + (cos(a) * r).toFloat(), center.y + (sin(a) * r).toFloat())
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, color.copy(alpha = color.alpha * alpha))
    drawPath(path, Color(0xFF2B1B14).copy(alpha = alpha), style = androidx.compose.ui.graphics.drawscope.Stroke(radius * 0.08f))
}
