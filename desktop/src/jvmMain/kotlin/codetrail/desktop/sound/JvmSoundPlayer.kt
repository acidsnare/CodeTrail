package codetrail.desktop.sound

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

/**
 * Plays synthesized effects through one always-open output line with a tiny mixer.
 *
 * Java Sound's Clip.start() carries 100-200 ms of latency on macOS and serialises badly when
 * effects come fast, so the hero's footsteps lagged behind the animation. Here a daemon thread
 * keeps a SourceDataLine fed in ~12 ms chunks, summing whatever voices are active. play() only
 * drops a voice into a list, so the delay between the call and the speaker is about one chunk.
 * Any audio failure (headless CI, no output device) silently disables sound.
 */
class JvmSoundPlayer(override var enabled: Boolean = true) : SoundPlayer {

    private val format = AudioFormat(Synth.RATE.toFloat(), 16, 1, true, false)
    private val samples = ConcurrentHashMap<Sfx, ShortArray>()
    private val voices = CopyOnWriteArrayList<Voice>()
    @Volatile private var broken = false

    private class Voice(val pcm: ShortArray) {
        var pos = 0
    }

    init {
        Thread({ pump() }, "codetrail-audio").apply { isDaemon = true; priority = Thread.MAX_PRIORITY }.start()
    }

    override fun play(sfx: Sfx) {
        if (!enabled || broken) return
        val pcm = samples[sfx] ?: return // still synthesizing at startup: skip rather than stall
        voices += Voice(pcm)
    }

    /** Audio thread: synthesize all effects, then mix forever. */
    private fun pump() {
        try {
            for (sfx in Sfx.entries) samples[sfx] = toShorts(Synth.toPcm16(Synth.build(sfx)))
            val line = AudioSystem.getSourceDataLine(format)
            line.open(format, CHUNK * 2 * 4) // four chunks of headroom in the device buffer
            line.start()
            val mix = IntArray(CHUNK)
            val out = ByteArray(CHUNK * 2)
            while (true) {
                mix.fill(0)
                for (v in voices) {
                    val n = minOf(CHUNK, v.pcm.size - v.pos)
                    for (i in 0 until n) mix[i] += v.pcm[v.pos + i].toInt()
                    v.pos += n
                    if (v.pos >= v.pcm.size) voices.remove(v)
                }
                for (i in 0 until CHUNK) {
                    val s = mix[i].coerceIn(-32768, 32767)
                    out[2 * i] = (s and 0xFF).toByte()
                    out[2 * i + 1] = (s shr 8).toByte()
                }
                line.write(out, 0, out.size) // blocks until there is room: this paces the loop
            }
        } catch (e: Exception) {
            broken = true
        }
    }

    private fun toShorts(bytes: ByteArray): ShortArray =
        ShortArray(bytes.size / 2) { i -> ((bytes[2 * i].toInt() and 0xFF) or (bytes[2 * i + 1].toInt() shl 8)).toShort() }

    companion object {
        /** Frames per mix chunk: 256 frames at 22050 Hz is about 12 ms of latency. */
        private const val CHUNK = 256
    }
}
