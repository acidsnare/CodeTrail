package codetrail.app

import codetrail.core.progress.Profile
import codetrail.core.progress.ProfileRepository
import codetrail.core.progress.Settings
import codetrail.core.progress.SettingsStore
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/**
 * Same layout as the desktop under the app's Documents directory:
 *   codetrail/settings.properties
 *   codetrail/profiles/<id>.properties
 */
@OptIn(ExperimentalForeignApi::class)
class IosStorage : ProfileRepository, SettingsStore {

    private val fm = NSFileManager.defaultManager
    private val root: String = (fm.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as platform.Foundation.NSURL).path!! + "/codetrail"
    private val profilesDir get() = "$root/profiles"
    private val settingsFile get() = "$root/settings.properties"

    override fun load(): Settings = ProfileCodec.decodeSettings(read(settingsFile) ?: emptyMap())

    override fun save(settings: Settings) = write(settingsFile, ProfileCodec.encode(settings))

    override fun list(): List<Profile> {
        val names = fm.contentsOfDirectoryAtPath(profilesDir, null) ?: return emptyList()
        return names.filterIsInstance<String>()
            .filter { it.endsWith(".properties") }
            .mapNotNull { name -> read("$profilesDir/$name")?.let { ProfileCodec.decodeProfile(name.removeSuffix(".properties"), it) } }
            .sortedByDescending { it.lastPlayedAt }
    }

    override fun save(profile: Profile) = write("$profilesDir/${profile.id}.properties", ProfileCodec.encode(profile))

    override fun delete(id: String) {
        fm.removeItemAtPath("$profilesDir/$id.properties", null)
    }

    private fun read(path: String): Map<String, String>? {
        if (!fm.fileExistsAtPath(path)) return null
        val text = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: return null
        return runCatching { ProfileCodec.fromText(text) }.getOrNull()
    }

    private fun write(path: String, map: Map<String, String>) {
        val dir = path.substringBeforeLast('/')
        fm.createDirectoryAtPath(dir, true, null, null)
        @Suppress("CAST_NEVER_SUCCEEDS")
        (ProfileCodec.toText(map) as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
    }
}
