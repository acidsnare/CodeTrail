package codetrail.core.progress

import codetrail.core.command.Program

/**
 * One player. A profile is also a save slot: it owns its progress, its last chosen
 * character / world / difficulty and an [autosave] of the level in progress so the
 * game resumes exactly where it was left.
 */
data class Profile(
    val id: String,
    val name: String,
    val createdAt: Long,
    val lastPlayedAt: Long,
    val progress: Progress = Progress(),
    val characterId: String? = null,
    val worldId: String? = null,
    val tier: Int = 1,
    val autosave: SaveSlot? = null,
)

/** Enough to rebuild a level mid-solve: the generator is deterministic given tier and seed. */
data class SaveSlot(
    val tier: Int,
    val seed: Long,
    val worldId: String,
    val program: Program,
    /** "forward" (build the program) or "predict" (guess where a given program ends). */
    val mode: String = "forward",
)

/** App-wide settings that are not tied to a profile. */
data class Settings(
    val language: String? = null,
    val lastProfileId: String? = null,
)

interface ProfileRepository {
    fun list(): List<Profile>
    fun save(profile: Profile)
    fun delete(id: String)
}

interface SettingsStore {
    fun load(): Settings
    fun save(settings: Settings)
}
