package codetrail.app.theme

import codetrail.app.res.Res
import codetrail.app.res.char_bear
import codetrail.app.res.char_turtle
import codetrail.app.res.char_fox
import codetrail.app.res.char_penguin
import codetrail.app.res.char_red_panda
import codetrail.app.res.bear
import codetrail.app.res.fox
import codetrail.app.res.penguin
import codetrail.app.res.red_panda
import codetrail.app.res.turtle
import org.jetbrains.compose.resources.DrawableResource
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
    val art: DrawableResource,
    val unlockStars: Int,
    val fur: Color,
    val belly: Color,
    val tail: Tail,
) {
    companion object {
        /** Ordered by unlock threshold. The red panda is the grand prize, so it comes last. */
        val All = listOf(
            Character("turtle", Res.string.char_turtle, Res.drawable.turtle, 0, Color(0xFF6DBE6A), Color(0xFFC9E8B0), Tail.NONE),
            Character("penguin", Res.string.char_penguin, Res.drawable.penguin, 12, Color(0xFF2F3A4A), Color(0xFFFFFFFF), Tail.FLIPPERS),
            Character("fox", Res.string.char_fox, Res.drawable.fox, 40, Color(0xFFE8862E), Color(0xFFFFF3E0), Tail.BUSHY),
            Character("bear", Res.string.char_bear, Res.drawable.bear, 90, Color(0xFFB07A4A), Color(0xFFD9B48C), Tail.NONE),
            Character("red_panda", Res.string.char_red_panda, Res.drawable.red_panda, 160, Color(0xFFD9602B), Color(0xFF4A2A1C), Tail.STRIPED),
        )

        /** Old saves may still say "capybara": that hero became the bear. */
        fun byId(id: String?) = All.firstOrNull { it.id == (if (id == "capybara") "bear" else id) }
    }
}
