package codetrail.app.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The desktop mixer on top of AudioTrack: one streaming track fed in small chunks by a daemon
 * thread, voices summed on the fly. play() just adds a voice, so latency is one or two chunks.
 */
class AndroidSoundPlayer(override var enabled: Boolean = true) : SoundPlayer {

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
        val pcm = samples[sfx] ?: return
        voices += Voice(pcm)
    }

    private fun pump() {
        try {
            for (sfx in Sfx.entries) samples[sfx] = toShorts(Synth.toPcm16(Synth.build(sfx)))
            val minBuf = AudioTrack.getMinBufferSize(Synth.RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(Synth.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(minBuf, CHUNK * 2 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            track.play()
            val mix = IntArray(CHUNK)
            val out = ShortArray(CHUNK)
            while (true) {
                mix.fill(0)
                for (v in voices) {
                    val n = minOf(CHUNK, v.pcm.size - v.pos)
                    for (i in 0 until n) mix[i] += v.pcm[v.pos + i].toInt()
                    v.pos += n
                    if (v.pos >= v.pcm.size) voices.remove(v)
                }
                for (i in 0 until CHUNK) out[i] = mix[i].coerceIn(-32768, 32767).toShort()
                track.write(out, 0, CHUNK) // blocks until there is room, pacing the loop
            }
        } catch (e: Exception) {
            broken = true
        }
    }

    private fun toShorts(bytes: ByteArray): ShortArray =
        ShortArray(bytes.size / 2) { i -> ((bytes[2 * i].toInt() and 0xFF) or (bytes[2 * i + 1].toInt() shl 8)).toShort() }

    private companion object {
        const val CHUNK = 256
    }
}
