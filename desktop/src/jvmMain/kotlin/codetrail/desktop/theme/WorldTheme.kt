package codetrail.desktop.theme

import androidx.compose.ui.graphics.Color
import codetrail.desktop.res.Res
import codetrail.desktop.res.*
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
    /** What the hero says after stepping onto BLOCKED terrain in this world. */
    val fallMessage: StringResource,
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
            fallMessage = Res.string.fall_islands,
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
            fallMessage = Res.string.fall_forest,
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
            fallMessage = Res.string.fall_space,
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
        val Ice = WorldTheme(
            id = "ice",
            name = Res.string.world_ice,
            goalName = Res.string.goal_igloo,
            fallMessage = Res.string.fall_ice,
            background = Color(0xFF1C3A52),
            dark = true,
            seaTop = Color(0xFF5FB6E0),
            seaBottom = Color(0xFF2E7FB0),
            gridLine = Color(0x00000000),
            halo = Color(0xB3BFE9F7),
            // The floe itself is drawn by IceArt.drawTileFace as a jagged shape, so the base tile is invisible.
            land = Color(0x00000000),
            landEdge = Color(0x00000000),
            art = IceArt,
        )
        val City = WorldTheme(
            id = "city",
            name = Res.string.world_city,
            goalName = Res.string.goal_house,
            fallMessage = Res.string.fall_city,
            background = Color(0xFF2F3B2A),
            dark = true,
            seaTop = Color(0xFF86BE74),
            seaBottom = Color(0xFF6FA860),
            gridLine = Color(0x14FFFFFF),
            halo = Color(0xCC5E8E4E),
            land = Color(0xFF8D6E63),
            landEdge = Color(0xFF5D4037),
            art = CityArt,
        )
        val Lava = WorldTheme(
            id = "lava",
            name = Res.string.world_lava,
            goalName = Res.string.goal_treasure,
            fallMessage = Res.string.fall_lava,
            background = Color(0xFF2A1410),
            dark = true,
            seaTop = Color(0xFFFFB03A),
            seaBottom = Color(0xFFF2541B),
            gridLine = Color(0x00000000),
            halo = Color(0xFFFFD27A),
            land = Color(0xFF3C3542),
            landEdge = Color(0xFF241F28),
            art = LavaArt,
        )
        val All = listOf(Islands, Forest, Space, Ice, City, Lava)

        fun byId(id: String?) = All.firstOrNull { it.id == id }
    }
}
