package codetrail.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.use
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Renders an SVG to a transparent PNG with Skia. Usage: iconrender <in.svg> <out.png> <size> */
fun main(args: Array<String>) {
    val input = File(args[0])
    val output = File(args[1])
    val size = args.getOrNull(2)?.toIntOrNull() ?: 1024
    ImageComposeScene(width = size, height = size).use { scene ->
        scene.setContent {
            val density = LocalDensity.current
            val painter = input.inputStream().use { loadSvgPainter(it, density) }
            Canvas(Modifier.fillMaxSize()) { with(painter) { draw(this@Canvas.size) } }
        }
        val image = scene.render()
        output.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
    println("wrote ${output.absolutePath}")
}
