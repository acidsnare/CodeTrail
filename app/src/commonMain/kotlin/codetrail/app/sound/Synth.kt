package codetrail.app.sound

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tiny procedural synthesizer. Every effect is a few hundred milliseconds of 16-bit mono PCM
 * built from sine / square / triangle tones, pitch sweeps and filtered noise, so the game
 * ships no audio files and a web port can rebuild the same sounds with Web Audio.
 */
object Synth {
    const val RATE = 22050

    enum class Wave { SINE, SQUARE, TRIANGLE }

    private fun osc(wave: Wave, phase: Double): Double = when (wave) {
        Wave.SINE -> sin(phase)
        Wave.SQUARE -> if (sin(phase) >= 0) 0.6 else -0.6
        Wave.TRIANGLE -> 2.0 / PI * kotlin.math.asin(sin(phase))
    }

    /** A tone whose pitch glides from [f0] to [f1] with an exponential decay envelope. */
    fun sweep(f0: Double, f1: Double, ms: Int, wave: Wave = Wave.SINE, volume: Double = 0.5, decay: Double = 6.0, attackMs: Int = 4): DoubleArray {
        val n = RATE * ms / 1000
        val out = DoubleArray(n)
        var phase = 0.0
        val attack = RATE * attackMs / 1000
        for (i in 0 until n) {
            val t = i / n.toDouble()
            val f = f0 + (f1 - f0) * t
            phase += 2 * PI * f / RATE
            val env = exp(-decay * t) * (if (i < attack) i / attack.toDouble() else 1.0)
            out[i] = osc(wave, phase) * env * volume
        }
        return out
    }

    fun tone(freq: Double, ms: Int, wave: Wave = Wave.SINE, volume: Double = 0.5, decay: Double = 6.0) =
        sweep(freq, freq, ms, wave, volume, decay)

    /** White noise through a one-pole low-pass, decaying. Water and thuds. */
    fun noise(ms: Int, volume: Double = 0.5, decay: Double = 5.0, lowpass: Double = 0.2, seed: Int = 1): DoubleArray {
        val rnd = Random(seed)
        val n = RATE * ms / 1000
        val out = DoubleArray(n)
        var last = 0.0
        for (i in 0 until n) {
            val t = i / n.toDouble()
            last += (rnd.nextDouble(-1.0, 1.0) - last) * lowpass
            out[i] = last * exp(-decay * t) * volume
        }
        return out
    }

    fun silence(ms: Int) = DoubleArray(RATE * ms / 1000)

    /** Play one after another. */
    fun seq(vararg parts: DoubleArray): DoubleArray {
        val out = DoubleArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) { p.copyInto(out, pos); pos += p.size }
        return out
    }

    /** Play at the same time. */
    fun mix(vararg parts: DoubleArray): DoubleArray {
        val out = DoubleArray(parts.maxOf { it.size })
        for (p in parts) for (i in p.indices) out[i] += p[i]
        return out
    }

    fun toPcm16(samples: DoubleArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = (samples[i].coerceIn(-1.0, 1.0) * 32767).toInt()
            bytes[2 * i] = (v and 0xFF).toByte()
            bytes[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    // ---- the actual effects ----

    private const val C5 = 523.25; private const val E5 = 659.25; private const val G5 = 783.99; private const val C6 = 1046.5
    private const val G4 = 392.0; private const val A4 = 440.0; private const val EB4 = 311.13

    fun build(sfx: Sfx): DoubleArray = when (sfx) {
        Sfx.STEP_A -> tone(G4, 70, Wave.SQUARE, 0.25, 9.0)
        Sfx.STEP_B -> tone(A4, 70, Wave.SQUARE, 0.25, 9.0)
        Sfx.TURN -> sweep(320.0, 520.0, 90, Wave.TRIANGLE, 0.35, 5.0)
        Sfx.HOP -> sweep(300.0, 950.0, 200, Wave.SINE, 0.4, 3.0)
        Sfx.SPLASH -> mix(noise(420, 0.55, 4.0, 0.12, 7), sweep(650.0, 140.0, 300, Wave.SINE, 0.3, 4.0))
        Sfx.BUMP -> mix(tone(110.0, 150, Wave.SQUARE, 0.5, 10.0), noise(80, 0.4, 12.0, 0.5, 3))
        Sfx.SAD -> seq(tone(G4, 220, Wave.TRIANGLE, 0.4, 3.0), tone(EB4, 320, Wave.TRIANGLE, 0.4, 3.0))
        Sfx.WIN -> seq(
            tone(C5, 110, Wave.SQUARE, 0.35, 4.0), tone(E5, 110, Wave.SQUARE, 0.35, 4.0),
            tone(G5, 110, Wave.SQUARE, 0.35, 4.0), tone(C6, 300, Wave.SQUARE, 0.35, 3.0),
        )
        Sfx.WIN_BIG -> seq(
            tone(C5, 100, Wave.SQUARE, 0.35, 4.0), tone(E5, 100, Wave.SQUARE, 0.35, 4.0),
            tone(G5, 100, Wave.SQUARE, 0.35, 4.0), tone(C6, 100, Wave.SQUARE, 0.35, 4.0),
            mix(tone(C6, 450, Wave.SQUARE, 0.25, 2.5), tone(E5, 450, Wave.TRIANGLE, 0.25, 2.5), tone(G5, 450, Wave.TRIANGLE, 0.25, 2.5)),
        )
        Sfx.UNLOCK -> seq(
            tone(G4, 140, Wave.TRIANGLE, 0.4, 3.0), tone(C5, 140, Wave.TRIANGLE, 0.4, 3.0),
            tone(E5, 140, Wave.TRIANGLE, 0.4, 3.0), tone(G5, 140, Wave.TRIANGLE, 0.4, 3.0),
            mix(tone(C6, 600, Wave.SINE, 0.35, 2.0), tone(G5, 600, Wave.SINE, 0.25, 2.0), tone(E5, 600, Wave.SINE, 0.2, 2.0)),
        )
        Sfx.CLICK -> tone(1200.0, 30, Wave.SINE, 0.3, 12.0)
        Sfx.CARD_ADD -> sweep(600.0, 900.0, 70, Wave.SINE, 0.35, 8.0)
        Sfx.CARD_REMOVE -> sweep(800.0, 400.0, 80, Wave.SINE, 0.3, 8.0)
        Sfx.HINT -> mix(tone(C6, 260, Wave.SINE, 0.3, 4.0), seq(silence(60), tone(E5 * 2, 200, Wave.SINE, 0.2, 4.0)))
        Sfx.GUESS -> sweep(500.0, 700.0, 60, Wave.TRIANGLE, 0.3, 8.0)
    }
}
