package codetrail.app

/** The few things the shared app needs from the host: a clock, where saves live, whether it can quit, file dialogs. */
expect object Platform {
    fun currentTimeMillis(): Long

    /** Human-readable location of the save files, shown in Settings. */
    val saveLocation: String

    /** Desktop apps have a Quit button; a browser tab does not. */
    val canQuit: Boolean

    /** Fingers, not a mouse: small controls grow to a comfortable tap size. */
    val touch: Boolean

    /** Whether [saveTextFile] and [openTextFile] do anything here. Phones only use the copy / paste code. */
    val canUseFiles: Boolean

    /** Lets the user pick where to save [text]; a no-op where files are not supported. */
    fun saveTextFile(suggestedName: String, text: String)

    /** Lets the user pick a text file and hands its content to [onLoaded]; a no-op where files are not supported. */
    fun openTextFile(onLoaded: (String) -> Unit)

    /**
     * Browsers may refuse clipboard access and Compose surfaces that as an unhandled rejection
     * that kills the app, so the web target copies through its own guarded JS.
     */
    val ownsClipboard: Boolean

    fun copyToClipboard(text: String)
}
