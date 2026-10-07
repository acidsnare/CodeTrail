package codetrail.app

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import codetrail.core.command.Command
import codetrail.core.engine.Solver
import codetrail.core.progress.Profile
import codetrail.core.progress.ProfileRepository
import codetrail.core.progress.Progress
import codetrail.core.progress.Settings
import codetrail.core.progress.SettingsStore
import codetrail.app.theme.WorldTheme
import codetrail.app.ui.Root
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Renders a screen to a PNG without opening a window. Handy for checking layout from a terminal.
 * Usage: snapshot <out.png> [tier] [seed] [solve|edit] [world] [lang] [stars] [screen]
 * screen: menu | profiles | play | settings | game (default game) | pause
 */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "snapshot.png" })
    val tier = args.getOrNull(1)?.toIntOrNull() ?: 3
    val seed = args.getOrNull(2)?.toLongOrNull() ?: 7L
    val solve = args.getOrNull(3) == "solve"
    val world = WorldTheme.byId(args.getOrNull(4)) ?: WorldTheme.Islands
    val lang = AppLanguage.entries.firstOrNull { it.tag == args.getOrNull(5) } ?: AppLanguage.EN
    val stars = args.getOrNull(6)?.toIntOrNull() ?: 0
    val screen = args.getOrNull(7) ?: "game"
    val frameMs = args.getOrNull(8)?.toLongOrNull() ?: 16L
    val heroId = args.getOrNull(9)?.takeIf { it.isNotBlank() } ?: "turtle"
    val mode = GameMode.of(args.getOrNull(10))

    val demo = Profile("demo", "Mila", 0L, 0L, Progress(totalStars = stars, levelsWon = stars / 2, wonPerTier = mapOf(1 to 7, 2 to 5, 3 to 3, 4 to 1), wonPerWorld = mapOf("islands" to 6, "forest" to 4, "space" to 2, "lava" to 3, "city" to 1)), characterId = heroId, tier = tier)
    val repo = object : ProfileRepository {
        val items = mutableListOf(demo, Profile("p2", "Oskar", 0L, 0L, Progress(totalStars = 31, levelsWon = 14), characterId = "penguin"))
        override fun list() = items.toList()
        override fun save(profile: Profile) { items.removeAll { it.id == profile.id }; items.add(0, profile) }
        override fun delete(id: String) { items.removeAll { it.id == id } }
    }
    val settings = object : SettingsStore {
        override fun load() = Settings(language = lang.tag, lastProfileId = "demo")
        override fun save(settings: Settings) = Unit
    }

    val app = AppState(repo, settings)
    when (screen) {
        "menu" -> app.goMenu()
        "profiles" -> app.goProfiles()
        "play" -> app.goPlay()
        "settings" -> app.goSettings()
        "stats" -> app.goStats()
        "quit" -> { app.goMenu(); app.requestQuit() }
        else -> {
            // pass the seed through a save slot so the level is reproducible
            app.startGame(world, tier, mode, resume = codetrail.core.progress.SaveSlot(tier, seed, world.id, emptyList(), mode.id))
            val state = app.game!!
            if (mode == GameMode.PREDICT) {
                // guess the true answer so the win state renders
                state.selectGuess(state.predict!!.answer)
                if (solve) runBlocking { state.run() }
                println("predict program=${state.program.size} answer=${state.predict!!.answer} phase=${state.phase}")
            } else if (args.getOrNull(3) == "hint") {
                state.addCommand(Command.TurnRight)
                state.hint()
                println("hint=${state.hintCommand} removeLast=${state.hintRemoveLast}")
            } else if (solve) {
                val l = state.level
                Solver.solve(l.grid, l.start, l.startDir, l.goal, l.commandSet)!!.let { state.program.addAll(it.program); state.function.addAll(it.function) }
                runBlocking { state.run() }
                println("phase=${state.phase} stars=${state.stars} payout=${state.payout} profileStars=${state.profile.progress.totalStars}")
                // replay the same level: must not pay again
                runBlocking { state.run() }
                println("replay: payout=${state.payout} profileStars=${state.profile.progress.totalStars}")
                // a solved level is locked: restart hands out a fresh level, solve that one too
                state.restartLevel()
                val n = state.level
                Solver.solve(n.grid, n.start, n.startDir, n.goal, n.commandSet)!!.let { state.program.addAll(it.program); state.function.addAll(it.function) }
                runBlocking { state.run() }
                println("restart+solve: newLevel=${n.seed != l.seed} payout=${state.payout} profileStars=${state.profile.progress.totalStars} rewarded=${state.profile.progress.rewarded}")
            } else if (args.getOrNull(3) == "fail") {
                // Walk straight off the path so the failure scene plays; sample the pose while it runs.
                val l = state.level
                if (l.commandSet.relative) state.addCommand(Command.Forward(l.grid.width)) else repeat(l.grid.width) { state.addCommand(Command.Move(l.startDir)) }
                runBlocking {
                    val job = launch { state.run() }
                    var t = 0
                    while (job.isActive) { delay(100); t += 100; val h = state.hero; println("t=${t}ms pos=(%.2f,%.2f) lift=%.2f scale=%.2f alpha=%.2f tilt=%.0f phase=${state.phase}".format(h.x, h.y, h.lift, h.scale, h.alpha, h.tilt)) }
                }
                println("failure=${state.failure}")
            } else {
                state.addCommand(Command.TurnRight)
                state.addCommand(Command.Forward(3))
            }
            if (screen == "pause") app.pause()
        }
    }

    ImageComposeScene(width = 1280, height = 800).use { scene ->
        scene.setContent { Root(app, onQuit = {}) }
        scene.render(nanoTime = 0L)
        // Step through intermediate frames so animations driven by frame clocks advance.
        var t = 0L
        while (t < frameMs) { t = minOf(t + 16, frameMs); scene.render(nanoTime = t * 1_000_000L) }
        val image = scene.render(nanoTime = frameMs * 1_000_000L)
        out.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
    println("wrote ${out.absolutePath}")
}
