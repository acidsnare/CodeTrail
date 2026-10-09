package codetrail.app

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual object Platform {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual val saveLocation: String = File(System.getProperty("user.home"), ".codetrail").path

    actual val canQuit: Boolean = true

    actual val touch: Boolean = false

    actual val ownsClipboard: Boolean = false

    actual fun copyToClipboard(text: String) = Unit

    actual val canUseFiles: Boolean = true

    actual fun saveTextFile(suggestedName: String, text: String) {
        val dialog = FileDialog(null as Frame?, "CodeTrail", FileDialog.SAVE).apply { file = suggestedName; isVisible = true }
        val dir = dialog.directory ?: return
        val name = dialog.file ?: return
        runCatching { File(dir, name).writeText(text) }
    }

    actual fun openTextFile(onLoaded: (String) -> Unit) {
        val dialog = FileDialog(null as Frame?, "CodeTrail", FileDialog.LOAD).apply { isVisible = true }
        val dir = dialog.directory ?: return
        val name = dialog.file ?: return
        runCatching { File(dir, name).readText() }.getOrNull()?.let(onLoaded)
    }
}
