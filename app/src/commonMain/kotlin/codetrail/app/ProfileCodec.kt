package codetrail.app

import codetrail.core.command.ProgramCodec
import codetrail.core.progress.Profile
import codetrail.core.progress.Progress
import codetrail.core.progress.SaveSlot
import codetrail.core.progress.Settings

/**
 * Profiles and settings as flat key -> value maps. Every platform stores these maps its own
 * way (.properties files on desktop, localStorage in the browser) but the keys are shared, so a
 * save file can be moved between them.
 */
object ProfileCodec {

    fun encode(settings: Settings): Map<String, String> = buildMap {
        settings.language?.let { put("language", it) }
        settings.lastProfileId?.let { put("lastProfile", it) }
        put("sound", settings.sound.toString())
        put("speed", settings.speed)
    }

    fun decodeSettings(p: Map<String, String>): Settings = Settings(
        language = p["language"],
        lastProfileId = p["lastProfile"],
        sound = p["sound"]?.toBooleanStrictOrNull() ?: true,
        speed = p["speed"] ?: "normal",
    )

    fun encode(profile: Profile): Map<String, String> = buildMap {
        put("name", profile.name)
        put("createdAt", profile.createdAt.toString())
        put("lastPlayedAt", profile.lastPlayedAt.toString())
        profile.characterId?.let { put("character", it) }
        profile.worldId?.let { put("world", it) }
        put("tier", profile.tier.toString())
        with(profile.progress) {
            put("progress.totalStars", totalStars.toString())
            put("progress.levelsWon", levelsWon.toString())
            wonPerTier.forEach { (tier, n) -> put("progress.wonPerTier.$tier", n.toString()) }
            wonPerWorld.forEach { (w, n) -> put("progress.wonPerWorld.$w", n.toString()) }
            put("progress.rewarded", rewarded.joinToString(","))
        }
        profile.autosave?.let { a ->
            put("autosave.tier", a.tier.toString())
            put("autosave.seed", a.seed.toString())
            put("autosave.world", a.worldId)
            put("autosave.program", ProgramCodec.encode(a.program))
            put("autosave.mode", a.mode)
            put("autosave.function", ProgramCodec.encode(a.function))
        }
    }

    fun decodeProfile(id: String, p: Map<String, String>): Profile {
        val perTier = p.keys
            .filter { it.startsWith("progress.wonPerTier.") }
            .mapNotNull { k -> k.removePrefix("progress.wonPerTier.").toIntOrNull()?.let { it to (p[k]?.toIntOrNull() ?: 0) } }
            .toMap()
        val perWorld = p.keys
            .filter { it.startsWith("progress.wonPerWorld.") }
            .associate { k -> k.removePrefix("progress.wonPerWorld.") to (p[k]?.toIntOrNull() ?: 0) }
        val autosave = p["autosave.seed"]?.toLongOrNull()?.let { seed ->
            SaveSlot(
                tier = p["autosave.tier"]?.toIntOrNull() ?: 1,
                seed = seed,
                worldId = p["autosave.world"] ?: "islands",
                program = ProgramCodec.decode(p["autosave.program"] ?: ""),
                mode = p["autosave.mode"] ?: "forward",
                function = ProgramCodec.decode(p["autosave.function"] ?: ""),
            )
        }
        return Profile(
            id = id,
            name = p["name"] ?: id,
            createdAt = p["createdAt"]?.toLongOrNull() ?: 0L,
            lastPlayedAt = p["lastPlayedAt"]?.toLongOrNull() ?: 0L,
            progress = Progress(
                totalStars = p["progress.totalStars"]?.toIntOrNull() ?: 0,
                levelsWon = p["progress.levelsWon"]?.toIntOrNull() ?: 0,
                wonPerTier = perTier,
                wonPerWorld = perWorld,
                rewarded = (p["progress.rewarded"] ?: "").split(',').filter { it.isNotBlank() }.toSet(),
            ),
            characterId = p["character"],
            worldId = p["world"],
            tier = p["tier"]?.toIntOrNull()?.coerceIn(1, 6) ?: 1,
            autosave = autosave,
        )
    }

    /** "key=value" per line, the subset of .properties syntax both sides need. */
    fun toText(map: Map<String, String>): String = map.entries.joinToString("\n") { (k, v) -> "$k=${v.replace("\n", "\\n")}" }

    fun fromText(text: String): Map<String, String> = text.lineSequence()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapNotNull { line -> line.indexOf('=').takeIf { it > 0 }?.let { line.substring(0, it).trim() to line.substring(it + 1).replace("\\n", "\n") } }
        .toMap()
}
