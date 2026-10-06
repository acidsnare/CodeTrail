package codetrail.desktop.sound

import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * Plays synthesized effects through javax.sound.sampled.
 *
 * All clips are synthesized and opened once, on a background thread at startup, and every
 * play() call is handed to a single audio thread, so the UI thread never touches the audio
 * device. Any audio failure (headless CI, no output device) silently disables sound.
 */
class JvmSoundPlayer(override var enabled: Boolean = true) : SoundPlayer {

    private val format = AudioFormat(Synth.RATE.toFloat(), 16, 1, true, false)
    private val clips = ConcurrentHashMap<Sfx, Clip>()
    @Volatile private var broken = false
    private val audio = Executors.newSingleThreadExecutor { r -> Thread(r, "codetrail-audio").apply { isDaemon = true } }

    init {
        audio.execute {
            try {
                for (sfx in Sfx.entries) clips[sfx] = load(sfx)
            } catch (e: Exception) {
                broken = true
            }
        }
    }

    override fun play(sfx: Sfx) {
        if (!enabled || broken) return
        audio.execute {
            try {
                val clip = clips[sfx] ?: load(sfx).also { clips[sfx] = it }
                if (clip.isRunning) clip.stop()
                clip.framePosition = 0
                clip.start()
            } catch (e: Exception) {
                broken = true
            }
        }
    }

    private fun load(sfx: Sfx): Clip {
        val pcm = Synth.toPcm16(Synth.build(sfx))
        val stream = AudioInputStream(ByteArrayInputStream(pcm), format, (pcm.size / 2).toLong())
        val clip = AudioSystem.getClip()
        clip.open(stream)
        return clip
    }
}
