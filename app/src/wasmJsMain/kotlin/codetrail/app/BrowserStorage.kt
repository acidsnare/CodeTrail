package codetrail.app

import codetrail.core.progress.Profile
import codetrail.core.progress.ProfileRepository
import codetrail.core.progress.Settings
import codetrail.core.progress.SettingsStore

/**
 * localStorage, one entry per profile plus one for settings, in the same key=value text the
 * desktop writes to .properties files.
 *   codetrail.settings
 *   codetrail.profile.<id>
 */
class BrowserStorage : ProfileRepository, SettingsStore {

    override fun load(): Settings = ProfileCodec.decodeSettings(Browser.get(SETTINGS)?.let(ProfileCodec::fromText) ?: emptyMap())

    override fun save(settings: Settings) = Browser.set(SETTINGS, ProfileCodec.toText(ProfileCodec.encode(settings)))

    override fun list(): List<Profile> = Browser.keys()
        .filter { it.startsWith(PROFILE) }
        .mapNotNull { key -> Browser.get(key)?.let { ProfileCodec.decodeProfile(key.removePrefix(PROFILE), ProfileCodec.fromText(it)) } }
        .sortedByDescending { it.lastPlayedAt }

    override fun save(profile: Profile) = Browser.set(PROFILE + profile.id, ProfileCodec.toText(ProfileCodec.encode(profile)))

    override fun delete(id: String) = Browser.remove(PROFILE + id)

    private companion object {
        const val SETTINGS = "codetrail.settings"
        const val PROFILE = "codetrail.profile."
    }
}
