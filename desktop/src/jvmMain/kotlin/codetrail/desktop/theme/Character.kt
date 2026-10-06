package codetrail.desktop.theme

import codetrail.desktop.res.Res
import codetrail.desktop.res.char_capybara
import codetrail.desktop.res.char_fox
import codetrail.desktop.res.char_penguin
import codetrail.desktop.res.char_red_panda
import org.jetbrains.compose.resources.StringResource

/** @property unlockStars total stars needed to play as this character. 0 means available from the start. */
data class Character(val id: String, val name: StringResource, val svgPath: String, val unlockStars: Int) {
    companion object {
        /** Ordered by unlock threshold. The red panda is the grand prize, so it comes last. */
        val All = listOf(
            Character("capybara", Res.string.char_capybara, "characters/capybara.svg", unlockStars = 0),
            Character("fox", Res.string.char_fox, "characters/fox.svg", unlockStars = 10),
            Character("penguin", Res.string.char_penguin, "characters/penguin.svg", unlockStars = 25),
            Character("red_panda", Res.string.char_red_panda, "characters/red_panda.svg", unlockStars = 50),
        )

        fun byId(id: String?) = All.firstOrNull { it.id == id }
    }
}
