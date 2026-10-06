package codetrail.desktop.theme

import codetrail.desktop.res.Res
import codetrail.desktop.res.char_bear
import codetrail.desktop.res.char_turtle
import codetrail.desktop.res.char_fox
import codetrail.desktop.res.char_penguin
import codetrail.desktop.res.char_red_panda
import org.jetbrains.compose.resources.StringResource

import androidx.compose.ui.graphics.Color

/** How the tail is drawn on the top-down board sprite. */
enum class Tail { STRIPED, BUSHY, FLIPPERS, NONE }

/**
 * @property unlockStars total stars needed to play as this character. 0 means available from the start.
 * @property fur body colour on the board, @property belly lighter underside, @property tail tail style.
 */
data class Character(
    val id: String,
    val name: StringResource,
    val svgPath: String,
    val unlockStars: Int,
    val fur: Color,
    val belly: Color,
    val tail: Tail,
) {
    companion object {
        /** Ordered by unlock threshold. The red panda is the grand prize, so it comes last. */
        val All = listOf(
            Character("turtle", Res.string.char_turtle, "characters/turtle.svg", 0, Color(0xFF6DBE6A), Color(0xFFC9E8B0), Tail.NONE),
            Character("penguin", Res.string.char_penguin, "characters/penguin.svg", 12, Color(0xFF2F3A4A), Color(0xFFFFFFFF), Tail.FLIPPERS),
            Character("fox", Res.string.char_fox, "characters/fox.svg", 40, Color(0xFFE8862E), Color(0xFFFFF3E0), Tail.BUSHY),
            Character("bear", Res.string.char_bear, "characters/bear.svg", 90, Color(0xFFB07A4A), Color(0xFFD9B48C), Tail.NONE),
            Character("red_panda", Res.string.char_red_panda, "characters/red_panda.svg", 160, Color(0xFFD9602B), Color(0xFF4A2A1C), Tail.STRIPED),
        )

        /** Old saves may still say "capybara": that hero became the bear. */
        fun byId(id: String?) = All.firstOrNull { it.id == (if (id == "capybara") "bear" else id) }
    }
}
