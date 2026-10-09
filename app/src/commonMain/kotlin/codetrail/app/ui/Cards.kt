package codetrail.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.core.command.Command
import codetrail.core.model.Dir
import codetrail.app.sound.Sfx
import codetrail.app.res.*
import org.jetbrains.compose.resources.stringResource

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun CommandCard(
    command: Command,
    modifier: Modifier = Modifier,
    size: Int = 56,
    highlighted: Boolean = false,
    failed: Boolean = false,
    hinted: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val bg = when {
        failed -> Color(0xFFFFCDD2)
        highlighted -> Color(0xFFFFF59D)
        hinted -> Color(0xFFC8E6C9)
        selected -> Color(0xFFEDE7F6)
        else -> Color.White
    }
    val border = when {
        highlighted || failed || hinted -> Ink
        selected -> Accent2
        else -> Color(0xFFBDBDBD)
    }
    Box(
        modifier
            .size(size.dp)
            .background(bg, CardShape)
            .border(if (highlighted || hinted || selected) 3.dp else 2.dp, border, CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        CardGlyph(command, size)
    }
}

@Composable
fun commandLabel(c: Command): String = when (c) {
    is Command.Move -> stringResource(
        when (c.dir) {
            Dir.NORTH -> Res.string.cmd_move_north
            Dir.EAST -> Res.string.cmd_move_east
            Dir.SOUTH -> Res.string.cmd_move_south
            Dir.WEST -> Res.string.cmd_move_west
        },
    )
    is Command.Forward -> stringResource(Res.string.cmd_forward, c.cells)
    Command.TurnLeft -> stringResource(Res.string.cmd_turn_left)
    Command.TurnRight -> stringResource(Res.string.cmd_turn_right)
    Command.Jump -> stringResource(Res.string.cmd_jump)
    is Command.Repeat -> stringResource(Res.string.cmd_repeat, c.times)
    Command.Call -> stringResource(Res.string.cmd_call)
}

@Composable
private fun CardGlyph(c: Command, size: Int) {
    val iconSize = (size * 0.55f).dp
    val label = commandLabel(c)
    when (c) {
        is Command.Move -> Icon(dirIcon(c.dir), label, Modifier.size(iconSize), tint = Ink)
        is Command.Forward -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, label, Modifier.size((size * 0.42f).dp), tint = Ink)
            Text("${c.cells}", color = Ink, fontWeight = FontWeight.Bold, fontSize = (size * 0.3f).sp)
        }
        Command.TurnLeft -> Icon(Icons.AutoMirrored.Filled.RotateLeft, label, Modifier.size(iconSize), tint = Ink)
        Command.TurnRight -> Icon(Icons.AutoMirrored.Filled.RotateRight, label, Modifier.size(iconSize), tint = Ink)
        Command.Jump -> Icon(Icons.Default.MoveUp, label, Modifier.size(iconSize), tint = Ink)
        is Command.Repeat -> RepeatGlyph(c.times, null, size)
        Command.Call -> CallGlyph(size)
    }
}

/** Block A call: a bold letter in a small rounded frame, the same look as the block panel's badge. */
@Composable
fun CallGlyph(size: Int, tint: Color = Ink) {
    Box(
        Modifier.size((size * 0.56f).dp).border(2.dp, tint, RoundedCornerShape((size * 0.14f).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("A", color = tint, fontWeight = FontWeight.Black, fontSize = (size * 0.36f).sp)
    }
}

/** Loop badge: a repeat arrow with "×N", or "k/N" while the loop is running. */
@Composable
fun RepeatGlyph(times: Int, iteration: Int?, size: Int, tint: Color = Ink) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Repeat, null, Modifier.size((size * 0.42f).dp), tint = tint)
        Text(
            if (iteration == null) "×$times" else "$iteration/$times",
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.26f).sp,
        )
    }
}

private fun dirIcon(d: Dir): ImageVector = when (d) {
    Dir.NORTH -> Icons.Default.ArrowUpward
    Dir.EAST -> Icons.AutoMirrored.Filled.ArrowForward
    Dir.SOUTH -> Icons.Default.ArrowDownward
    Dir.WEST -> Icons.AutoMirrored.Filled.ArrowBack
}

@Composable
fun EmptySlot(index: Int, size: Int = 56) {
    val tone = LocalContentColor.current
    Box(
        Modifier
            .size(size.dp)
            .background(tone.copy(alpha = 0.06f), CardShape)
            .border(2.dp, tone.copy(alpha = 0.22f), CardShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("${index + 1}", color = tone.copy(alpha = 0.35f), fontSize = (size * 0.34f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    // Follows the surrounding content colour so it reads on both dark and light chrome.
    val content = LocalContentColor.current
    val sounds = LocalSounds.current
    Box(
        Modifier
            .padding(2.dp)
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
            .border(2.dp, if (selected) Color.White else content.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .clickable { sounds.play(Sfx.CLICK); onClick() }
            .heightIn(min = TapTarget - 4.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Ink else content, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}
