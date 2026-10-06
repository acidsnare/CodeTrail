package codetrail.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Replay
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
import codetrail.desktop.res.*
import org.jetbrains.compose.resources.stringResource

private val CardShape = RoundedCornerShape(12.dp)

@Composable
fun CommandCard(
    command: Command,
    modifier: Modifier = Modifier,
    size: Int = 56,
    highlighted: Boolean = false,
    failed: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val bg = when {
        failed -> Color(0xFFFFCDD2)
        highlighted -> Color(0xFFFFF59D)
        else -> Color.White
    }
    val border = if (highlighted || failed) Ink else Color(0xFFBDBDBD)
    Box(
        modifier
            .size(size.dp)
            .background(bg, CardShape)
            .border(if (highlighted) 3.dp else 2.dp, border, CardShape)
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
    is Command.Turn -> stringResource(Res.string.cmd_turn_degrees, c.degrees)
    Command.Jump -> stringResource(Res.string.cmd_jump)
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
        is Command.Turn -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Replay, label, Modifier.size((size * 0.4f).dp), tint = Ink)
            Text("${c.degrees}°", color = Ink, fontWeight = FontWeight.Bold, fontSize = (size * 0.26f).sp)
        }
        Command.Jump -> Icon(Icons.Default.MoveUp, label, Modifier.size(iconSize), tint = Ink)
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
    Box(
        Modifier
            .size(size.dp)
            .background(Color.White.copy(alpha = 0.6f), CardShape)
            .border(2.dp, Color(0xFFE0E0E0), CardShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("${index + 1}", color = Color(0xFFBDBDBD), fontSize = (size * 0.4f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    // Follows the surrounding content colour so it reads on both dark and light chrome.
    val content = LocalContentColor.current
    Box(
        Modifier
            .padding(2.dp)
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
            .border(2.dp, if (selected) Color.White else content.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, color = if (selected) Ink else content, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}
