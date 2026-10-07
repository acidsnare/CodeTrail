package codetrail.app

actual object Platform {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual val saveLocation: String = "Android/data/dk.codetrail.app"

    actual val canQuit: Boolean = true
}
