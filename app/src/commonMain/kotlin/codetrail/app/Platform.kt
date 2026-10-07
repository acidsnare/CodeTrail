package codetrail.app

/** The few things the shared app needs from the host: a clock, where saves live, whether it can quit. */
expect object Platform {
    fun currentTimeMillis(): Long

    /** Human-readable location of the save files, shown in Settings. */
    val saveLocation: String

    /** Desktop apps have a Quit button; a browser tab does not. */
    val canQuit: Boolean
}
