package codetrail.app

/** The few things the shared app needs from the host: a clock and where saves live. */
expect object Platform {
    fun currentTimeMillis(): Long

    /** Human-readable location of the save files, shown in Settings. */
    val saveLocation: String
}
