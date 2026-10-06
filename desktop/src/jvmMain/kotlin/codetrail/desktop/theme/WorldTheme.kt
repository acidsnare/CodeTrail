package codetrail.desktop.theme

import androidx.compose.ui.graphics.Color
import codetrail.desktop.res.Res
import codetrail.desktop.res.goal_berries
import codetrail.desktop.res.goal_lake
import codetrail.desktop.res.goal_rocket
import codetrail.desktop.res.world_forest
import codetrail.desktop.res.world_islands
import codetrail.desktop.res.world_space
import org.jetbrains.compose.resources.StringResource

/**
 * Visual skin for a world. The engine knows only WALKABLE / BLOCKED / OBSTACLE;
 * a theme decides what those look like and what the goal is called.
 *
 * Rendering follows one recipe for every world: a deep gradient "sea" of BLOCKED cells with a faint
 * grid, a translucent shallow ring around the walkable "land", bright rounded land tiles with a rim,
 * and small decorations (tufts, shells) scattered on land.
 */
data class WorldTheme(
    val id: String,
    val name: StringResource,
    val goalName: StringResource,
    /** App chrome behind the board. */
    val background: Color,
    val dark: Boolean,
    /** BLOCKED cells, vertical gradient top -> bottom. */
    val seaTop: Color,
    val seaBottom: Color,
    val gridLine: Color,
    /** Translucent shallow band around land, drawn on the sea. */
    val halo: Color,
    /** WALKABLE cell body. */
    val land: Color,
    /** Thin rim around each land tile. */
    val landEdge: Color,
    /** Shapes: goal, obstacle, scattered props. */
    val art: WorldArt,
) {
    companion object {
        val Islands = WorldTheme(
            id = "islands",
            name = Res.string.world_islands,
            goalName = Res.string.goal_lake,
            background = Color(0xFF163B4A),
            dark = true,
            seaTop = Color(0xFF1FA2C9),
            seaBottom = Color(0xFF157A9E),
            gridLine = Color(0x1FFFFFFF),
            halo = Color(0xB35ED0E8),
            land = Color(0xFFF6E2B3),
            landEdge = Color(0xFFD9BC84),
            art = IslandsArt,
        )
        val Forest = WorldTheme(
            id = "forest",
            name = Res.string.world_forest,
            goalName = Res.string.goal_berries,
            background = Color(0xFF1E3321),
            dark = true,
            seaTop = Color(0xFF3F8C4B),
            seaBottom = Color(0xFF2F6E3A),
            gridLine = Color(0x1FFFFFFF),
            halo = Color(0x9980C27A),
            land = Color(0xFFE2CFA3),
            landEdge = Color(0xFFBFA573),
            art = ForestArt,
        )
        val Space = WorldTheme(
            id = "space",
            name = Res.string.world_space,
            goalName = Res.string.goal_rocket,
            background = Color(0xFF0E1026),
            dark = true,
            seaTop = Color(0xFF262C55),
            seaBottom = Color(0xFF161A3A),
            gridLine = Color(0x24FFFFFF),
            halo = Color(0x8C6B74B5),
            land = Color(0xFFB8C0E6),
            landEdge = Color(0xFF8A94C4),
            art = SpaceArt,
        )
        val All = listOf(Islands, Forest, Space)

        fun byId(id: String?) = All.firstOrNull { it.id == id }
    }
}
