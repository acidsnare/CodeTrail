package codetrail.desktop

/** How fast the hero moves. [scale] multiplies every animation delay. */
enum class AnimSpeed(val id: String, val scale: Float) {
    SLOW("slow", 1.7f),
    NORMAL("normal", 1f),
    FAST("fast", 0.5f);

    companion object {
        fun of(id: String?): AnimSpeed = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
