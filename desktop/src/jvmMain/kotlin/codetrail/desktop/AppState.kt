package codetrail.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import codetrail.core.progress.Profile
import codetrail.core.progress.ProfileRepository
import codetrail.core.progress.Settings
import codetrail.core.progress.SettingsStore
import codetrail.desktop.sound.Sfx
import codetrail.desktop.sound.SoundPlayer
import codetrail.desktop.theme.Character
import codetrail.desktop.theme.WorldTheme
import java.util.UUID

enum class Screen { MENU, PROFILES, PLAY, GAME, SETTINGS, STATS }

/** Top-level navigation, profiles and settings. Owns at most one running [GameState]. */
class AppState(
    private val profiles: ProfileRepository,
    private val settingsStore: SettingsStore,
    val sounds: SoundPlayer = SoundPlayer.Silent,
) {
    var screen by mutableStateOf(Screen.MENU)
        private set

    var settings by mutableStateOf(settingsStore.load())
        private set

    val allProfiles = mutableStateListOf<Profile>().apply { addAll(profiles.list()) }

    var current by mutableStateOf<Profile?>(null)
        private set

    var game by mutableStateOf<GameState?>(null)
        private set

    var paused by mutableStateOf(false)
        private set

    val language: AppLanguage
        get() = AppLanguage.entries.firstOrNull { it.tag == settings.language } ?: AppLanguage.SYSTEM

    init {
        sounds.enabled = settings.sound
        current = allProfiles.firstOrNull { it.id == settings.lastProfileId } ?: allProfiles.firstOrNull()
        // First launch: go straight to profile creation.
        if (current == null) screen = Screen.PROFILES
    }

    // ---- navigation ----

    fun goMenu() { screen = Screen.MENU }
    fun goProfiles() { screen = Screen.PROFILES }
    fun goPlay() { if (current != null) screen = Screen.PLAY }
    fun goSettings() { screen = Screen.SETTINGS }
    fun goStats() { if (current != null) screen = Screen.STATS }

    /** Quit confirmation dialog. */
    var quitRequested by mutableStateOf(false)
        private set

    fun requestQuit() { quitRequested = true }
    fun cancelQuit() { quitRequested = false }

    /** Wipes stars and counters of the current profile; name, hero choice and saved level stay. */
    fun resetProgress() {
        val p = current ?: return
        game = null
        updateProfile(p.copy(progress = codetrail.core.progress.Progress(), characterId = null, autosave = null))
    }

    // ---- profiles ----

    fun createProfile(name: String) {
        val trimmed = name.trim().ifEmpty { return }
        val now = System.currentTimeMillis()
        val p = Profile(id = UUID.randomUUID().toString().take(8), name = trimmed, createdAt = now, lastPlayedAt = now)
        profiles.save(p)
        allProfiles.add(0, p)
        selectProfile(p)
        screen = Screen.MENU
    }

    fun selectProfile(p: Profile) {
        current = p
        game = null
        saveSettings(settings.copy(lastProfileId = p.id))
    }

    fun deleteProfile(p: Profile) {
        profiles.delete(p.id)
        allProfiles.remove(p)
        if (current?.id == p.id) {
            current = allProfiles.firstOrNull()
            game = null
            saveSettings(settings.copy(lastProfileId = current?.id))
        }
    }

    fun chooseCharacter(c: Character) {
        val p = current ?: return
        if (p.progress.totalStars < c.unlockStars) return
        updateProfile(p.copy(characterId = c.id))
    }

    private fun updateProfile(p: Profile) {
        profiles.save(p)
        val i = allProfiles.indexOfFirst { it.id == p.id }
        if (i >= 0) allProfiles[i] = p else allProfiles.add(0, p)
        if (current?.id == p.id) current = p
    }

    // ---- game ----

    val canContinue: Boolean get() = current?.autosave != null

    /** Resume the saved level if there is one, otherwise start a fresh level with the profile's last choices. */
    fun continueGame() {
        val p = current ?: return
        val slot = p.autosave
        val theme = WorldTheme.byId(slot?.worldId ?: p.worldId) ?: WorldTheme.Islands
        startGame(theme, slot?.tier ?: p.tier, GameMode.of(slot?.mode), resume = slot)
    }

    fun startGame(theme: WorldTheme, tier: Int, mode: GameMode = GameMode.FORWARD, resume: codetrail.core.progress.SaveSlot? = null) {
        val p = current ?: return
        game = GameState(p, theme, tier, resume, mode, sounds = sounds, onProfileChanged = ::updateProfile)
        paused = false
        screen = Screen.GAME
    }

    fun pause() { if (screen == Screen.GAME) paused = true }
    fun resume() { paused = false }

    fun exitToMenu() {
        game?.persist()
        paused = false
        screen = Screen.MENU
    }

    // ---- settings ----

    fun setLanguage(l: AppLanguage) = saveSettings(settings.copy(language = l.tag))

    fun setSound(on: Boolean) {
        sounds.enabled = on
        saveSettings(settings.copy(sound = on))
        if (on) sounds.play(Sfx.CLICK)
    }

    private fun saveSettings(s: Settings) {
        settings = s
        settingsStore.save(s)
    }
}
