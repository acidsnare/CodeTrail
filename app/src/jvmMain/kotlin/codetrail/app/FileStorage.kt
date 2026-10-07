package codetrail.app

import codetrail.core.command.ProgramCodec
import codetrail.core.progress.Profile
import codetrail.core.progress.ProfileRepository
import codetrail.core.progress.Progress
import codetrail.core.progress.SaveSlot
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

    // ---- settings ----

    override fun load(): Settings {
        val p = read(settingsFile) ?: return Settings()
        return Settings(
            language = p.getProperty("language"),
            lastProfileId = p.getProperty("lastProfile"),
            sound = p.getProperty("sound")?.toBooleanStrictOrNull() ?: true,
            speed = p.getProperty("speed") ?: "normal",
        )
    }

    override fun save(settings: Settings) {
        val p = Properties()
        settings.language?.let { p["language"] = it }
        settings.lastProfileId?.let { p["lastProfile"] = it }
        p["sound"] = settings.sound.toString()
        p["speed"] = settings.speed
        write(settingsFile, p, "CodeTrail settings")
    }

    // ---- profiles ----

    override fun list(): List<Profile> =
        profilesDir.listFiles { f -> f.extension == "properties" }
            ?.mapNotNull { f -> read(f)?.let { toProfile(f.nameWithoutExtension, it) } }
            ?.sortedByDescending { it.lastPlayedAt }
            ?: emptyList()

    override fun save(profile: Profile) {
        val p = Properties()
        p["name"] = profile.name
        p["createdAt"] = profile.createdAt.toString()
        p["lastPlayedAt"] = profile.lastPlayedAt.toString()
        profile.characterId?.let { p["character"] = it }
        profile.worldId?.let { p["world"] = it }
        p["tier"] = profile.tier.toString()
        with(profile.progress) {
            p["progress.totalStars"] = totalStars.toString()
            p["progress.levelsWon"] = levelsWon.toString()
            wonPerTier.forEach { (tier, n) -> p["progress.wonPerTier.$tier"] = n.toString() }
            wonPerWorld.forEach { (w, n) -> p["progress.wonPerWorld.$w"] = n.toString() }
            p["progress.rewarded"] = rewarded.joinToString(",")
        }
        profile.autosave?.let { a ->
            p["autosave.tier"] = a.tier.toString()
            p["autosave.seed"] = a.seed.toString()
            p["autosave.world"] = a.worldId
            p["autosave.program"] = ProgramCodec.encode(a.program)
            p["autosave.mode"] = a.mode
            p["autosave.function"] = ProgramCodec.encode(a.function)
        }
        write(File(profilesDir, "${profile.id}.properties"), p, "CodeTrail profile")
    }

    override fun delete(id: String) {
        File(profilesDir, "$id.properties").delete()
    }

    private fun toProfile(id: String, p: Properties): Profile {
        val perTier = p.stringPropertyNames()
            .filter { it.startsWith("progress.wonPerTier.") }
            .mapNotNull { k -> k.removePrefix("progress.wonPerTier.").toIntOrNull()?.let { it to (p.getProperty(k).toIntOrNull() ?: 0) } }
            .toMap()
        val perWorld = p.stringPropertyNames()
            .filter { it.startsWith("progress.wonPerWorld.") }
            .associate { k -> k.removePrefix("progress.wonPerWorld.") to (p.getProperty(k).toIntOrNull() ?: 0) }
        val autosave = p.getProperty("autosave.seed")?.toLongOrNull()?.let { seed ->
            SaveSlot(
                tier = p.getProperty("autosave.tier")?.toIntOrNull() ?: 1,
                seed = seed,
                worldId = p.getProperty("autosave.world") ?: "islands",
                program = ProgramCodec.decode(p.getProperty("autosave.program") ?: ""),
                mode = p.getProperty("autosave.mode") ?: "forward",
                function = ProgramCodec.decode(p.getProperty("autosave.function") ?: ""),
            )
        }
        return Profile(
            id = id,
            name = p.getProperty("name") ?: id,
            createdAt = p.getProperty("createdAt")?.toLongOrNull() ?: 0L,
            lastPlayedAt = p.getProperty("lastPlayedAt")?.toLongOrNull() ?: 0L,
            progress = Progress(
                totalStars = p.getProperty("progress.totalStars")?.toIntOrNull() ?: 0,
                levelsWon = p.getProperty("progress.levelsWon")?.toIntOrNull() ?: 0,
                wonPerTier = perTier,
                wonPerWorld = perWorld,
                rewarded = p.getProperty("progress.rewarded", "").split(',').filter { it.isNotBlank() }.toSet(),
            ),
            characterId = p.getProperty("character"),
            worldId = p.getProperty("world"),
            tier = p.getProperty("tier")?.toIntOrNull()?.coerceIn(1, 6) ?: 1,
            autosave = autosave,
        )
    }

    private fun read(f: File): Properties? {
        if (!f.exists()) return null
        return runCatching { Properties().also { p -> f.inputStream().use { p.load(it) } } }.getOrNull()
    }

    private fun write(f: File, p: Properties, comment: String) {
        runCatching {
            f.parentFile.mkdirs()
            f.outputStream().use { p.store(it, comment) }
        }
    }
}
