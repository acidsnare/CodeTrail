package codetrail.app.sound

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create

/**
 * Every effect is synthesized once into an in-memory WAV; play() starts a fresh AVAudioPlayer on
 * it so overlapping sounds mix. The ambient category keeps the player's music going.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSoundPlayer(override var enabled: Boolean = true) : SoundPlayer {

    private val clips: Map<Sfx, NSData> = Sfx.entries.associateWith { wav(Synth.toPcm16(Synth.build(it))) }
    private val playing = ArrayList<AVAudioPlayer>()

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryAmbient, null)
        session.setActive(true, null)
    }

    override fun play(sfx: Sfx) {
        if (!enabled) return
        val data = clips[sfx] ?: return
        val player = AVAudioPlayer(data, null)
        playing.removeAll { !it.playing }
        playing += player
        player.play()
    }

    /** 16-bit mono PCM wrapped in a RIFF/WAVE header. */
    private fun wav(pcm: ByteArray): NSData {
        val out = ByteArray(44 + pcm.size)
        fun ascii(at: Int, s: String) { for (i in s.indices) out[at + i] = s[i].code.toByte() }
        fun le32(at: Int, v: Int) { for (i in 0 until 4) out[at + i] = (v ushr (8 * i)).toByte() }
        fun le16(at: Int, v: Int) { for (i in 0 until 2) out[at + i] = (v ushr (8 * i)).toByte() }
        ascii(0, "RIFF"); le32(4, 36 + pcm.size); ascii(8, "WAVE")
        ascii(12, "fmt "); le32(16, 16); le16(20, 1); le16(22, 1)
        le32(24, Synth.RATE); le32(28, Synth.RATE * 2); le16(32, 2); le16(34, 16)
        ascii(36, "data"); le32(40, pcm.size)
        pcm.copyInto(out, 44)
        return out.usePinned { NSData.create(bytes = it.addressOf(0), length = out.size.toULong()) }
    }
}
