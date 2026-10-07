package codetrail.app.sound

/**
 * Web Audio playback. Clips are synthesized once and uploaded as AudioBuffers through the small
 * helper defined in index.html; play() starts a new BufferSource, which the browser mixes for us.
 * The AudioContext is created lazily on the first play, which always follows a click, so the
 * autoplay policy is satisfied.
 */
class WebSoundPlayer(override var enabled: Boolean = true) : SoundPlayer {

    private var loaded = false

    override fun play(sfx: Sfx) {
        if (!enabled) return
        if (!loaded) {
            for (s in Sfx.entries) {
                val samples = Synth.build(s)
                val buffer = audioAlloc(samples.size)
                for (i in samples.indices) audioSet(buffer, i, samples[i].toFloat())
                audioLoad(s.ordinal, buffer, Synth.RATE)
            }
            loaded = true
        }
        audioPlay(sfx.ordinal)
    }
}

private fun audioAlloc(n: Int): JsAny = js("new Float32Array(n)")
private fun audioSet(buffer: JsAny, i: Int, v: Float): Unit = js("buffer[i] = v")
private fun audioLoad(id: Int, buffer: JsAny, rate: Int): Unit = js("codetrailAudio.load(id, buffer, rate)")
private fun audioPlay(id: Int): Unit = js("codetrailAudio.play(id)")
