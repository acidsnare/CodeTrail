package codetrail.app

actual object Platform {
    actual fun currentTimeMillis(): Long = Browser.now()

    actual val saveLocation: String = "localStorage"

    actual val canQuit: Boolean = false
}
