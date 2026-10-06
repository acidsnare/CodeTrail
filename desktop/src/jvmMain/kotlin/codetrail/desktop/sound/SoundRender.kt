package codetrail.desktop.sound

import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/** Writes every effect to a WAV file so the sound set can be auditioned outside the game. */
fun main(args: Array<String>) {
    val dir = File(args.getOrElse(0) { "build/sounds" }).apply { mkdirs() }
    val format = AudioFormat(Synth.RATE.toFloat(), 16, 1, true, false)
    for (sfx in Sfx.entries) {
        val pcm = Synth.toPcm16(Synth.build(sfx))
        val stream = AudioInputStream(ByteArrayInputStream(pcm), format, (pcm.size / 2).toLong())
        val out = File(dir, "${sfx.name.lowercase()}.wav")
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out)
        println("%-12s %4d ms  %s".format(sfx.name, pcm.size / 2 * 1000 / Synth.RATE, out.name))
    }
}
