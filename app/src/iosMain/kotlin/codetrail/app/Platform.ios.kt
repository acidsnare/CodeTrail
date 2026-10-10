package codetrail.app

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object Platform {
    actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

    actual val saveLocation: String = "Documents/codetrail"

    // iOS apps are not quit from inside; the home gesture does that.
    actual val canQuit: Boolean = false

    actual val touch: Boolean = true

    // Compose's own clipboard works on UIKit, and profiles travel by copy / paste code on phones.
    actual val ownsClipboard: Boolean = false

    actual fun copyToClipboard(text: String) = Unit

    actual val canUseFiles: Boolean = false

    actual fun saveTextFile(suggestedName: String, text: String) = Unit

    actual fun openTextFile(onLoaded: (String) -> Unit) = Unit
}
