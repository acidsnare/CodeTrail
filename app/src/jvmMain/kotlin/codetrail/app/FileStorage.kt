package codetrail.app

import codetrail.core.progress.Profile
import codetrail.core.progress.ProfileRepository
import codetrail.core.progress.Settings
import codetrail.core.progress.SettingsStore
import java.io.File
import java.util.Properties

/**
 * Plain .properties files under ~/.codetrail:
 *   settings.properties          app-wide settings
 *   profiles/<id>.properties     one file per profile, deleting the file deletes the profile
 */
class FileStorage(
    private val root: File = File(System.getProperty("user.home"), ".codetrail"),
) : ProfileRepository, SettingsStore {

    private val profilesDir get() = File(root, "profiles")
    private val settingsFile get() = File(root, "settings.properties")

    override fun load(): Settings = ProfileCodec.decodeSettings(read(settingsFile) ?: emptyMap())

    override fun save(settings: Settings) = write(settingsFile, ProfileCodec.encode(settings), "CodeTrail settings")

    override fun list(): List<Profile> =
        profilesDir.listFiles { f -> f.extension == "properties" }
            ?.mapNotNull { f -> read(f)?.let { ProfileCodec.decodeProfile(f.nameWithoutExtension, it) } }
            ?.sortedByDescending { it.lastPlayedAt }
            ?: emptyList()

    override fun save(profile: Profile) = write(File(profilesDir, "${profile.id}.properties"), ProfileCodec.encode(profile), "CodeTrail profile")

    override fun delete(id: String) {
        File(profilesDir, "$id.properties").delete()
    }

    private fun read(f: File): Map<String, String>? {
        if (!f.exists()) return null
        return runCatching {
            val p = Properties().also { p -> f.inputStream().use { p.load(it) } }
            p.stringPropertyNames().associateWith { p.getProperty(it) }
        }.getOrNull()
    }

    private fun write(f: File, map: Map<String, String>, comment: String) {
        runCatching {
            f.parentFile.mkdirs()
            val p = Properties().also { p -> map.forEach { (k, v) -> p[k] = v } }
            f.outputStream().use { p.store(it, comment) }
        }
    }
}
