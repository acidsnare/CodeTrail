package codetrail.app.sound

/** Every sound the game can make. The UI and GameState ask for these by name, never by file. */
enum class Sfx {
    STEP_A, STEP_B, TURN, HOP, SPLASH, BUMP, SAD,
    WIN, WIN_BIG, UNLOCK,
    CLICK, CARD_ADD, CARD_REMOVE, HINT, GUESS,
}

interface SoundPlayer {
    var enabled: Boolean
    fun play(sfx: Sfx)

    object Silent : SoundPlayer {
        override var enabled: Boolean = false
        override fun play(sfx: Sfx) = Unit
    }
}
