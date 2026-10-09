package codetrail.app

actual object Platform {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual val saveLocation: String = "Android/data/dk.codetrail.app"

    actual val canQuit: Boolean = true

    actual val touch: Boolean = true

    // Profiles travel by copy / paste code on phones; no file pickers wired up here.
    actual val ownsClipboard: Boolean = false

    actual fun copyToClipboard(text: String) = Unit

    actual val canUseFiles: Boolean = false

    actual fun saveTextFile(suggestedName: String, text: String) = Unit

    actual fun openTextFile(onLoaded: (String) -> Unit) = Unit
}
