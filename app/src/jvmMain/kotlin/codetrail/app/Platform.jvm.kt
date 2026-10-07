package codetrail.app

import java.io.File

actual object Platform {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual val saveLocation: String = File(System.getProperty("user.home"), ".codetrail").path

    actual val canQuit: Boolean = true
}
