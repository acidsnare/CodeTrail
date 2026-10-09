package codetrail.app

actual object Platform {
    actual fun currentTimeMillis(): Long = Browser.now()

    actual val saveLocation: String = "localStorage"

    actual val canQuit: Boolean = false

    actual val touch: Boolean = Browser.hasTouch()

    actual val ownsClipboard: Boolean = true

    actual fun copyToClipboard(text: String) = Browser.copy(text)

    actual val canUseFiles: Boolean = true

    actual fun saveTextFile(suggestedName: String, text: String) = Browser.download(suggestedName, text)

    actual fun openTextFile(onLoaded: (String) -> Unit) = Browser.pickTextFile(onLoaded)
}
