package codetrail.app

import codetrail.core.progress.Profile
import codetrail.core.progress.ProfileRepository
import codetrail.core.progress.Settings
import codetrail.core.progress.SettingsStore
import java.io.File

/**
 * Same layout as the desktop under the app's private files directory:
 *   codetrail/settings.properties
 *   codetrail/profiles/<id>.properties
 */
class AndroidStorage(filesDir: File) : ProfileRepository, SettingsStore {

    private val root = File(filesDir, "codetrail")
    private val profilesDir get() = File(root, "profiles")
    private val settingsFile get() = File(root, "settings.properties")

    override fun load(): Settings = ProfileCodec.decodeSettings(read(settingsFile) ?: emptyMap())

    override fun save(settings: Settings) = write(settingsFile, ProfileCodec.encode(settings))

    override fun list(): List<Profile> =
        profilesDir.listFiles { f -> f.extension == "properties" }
            ?.mapNotNull { f -> read(f)?.let { ProfileCodec.decodeProfile(f.nameWithoutExtension, it) } }
            ?.sortedByDescending { it.lastPlayedAt }
            ?: emptyList()

    override fun save(profile: Profile) = write(File(profilesDir, "${profile.id}.properties"), ProfileCodec.encode(profile))

    override fun delete(id: String) {
        File(profilesDir, "$id.properties").delete()
    }

    private fun read(f: File): Map<String, String>? =
        if (f.exists()) runCatching { ProfileCodec.fromText(f.readText()) }.getOrNull() else null

    private fun write(f: File, map: Map<String, String>) {
        runCatching {
            f.parentFile?.mkdirs()
            f.writeText(ProfileCodec.toText(map))
        }
    }
}
