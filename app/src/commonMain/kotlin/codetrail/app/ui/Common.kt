package codetrail.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.app.theme.Character
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.heightIn
import codetrail.app.sound.Sfx
import codetrail.app.sound.SoundPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import codetrail.core.progress.Profile
import codetrail.app.res.Res
import codetrail.app.res.hero_title
import org.jetbrains.compose.resources.stringResource

/** Sound player for UI clicks; screens read it instead of threading it through every call. */
val LocalSounds = staticCompositionLocalOf<SoundPlayer> { SoundPlayer.Silent }

val Ink = Color(0xFF2B1B14)
val MenuBackground = Color(0xFF163B4A)
val Panel = Color(0x33FFFFFF)
val Accent = Color(0xFF43A047)
val Accent2 = Color(0xFF6A4DBA)
val Star = Color(0xFFFFD54F)

val Pill = Color(0x26FFFFFF)

/** How much FitScene shrank the scene: 1 on a 1280x800 desktop window, ~0.57 on a phone. */
val LocalSceneScale = compositionLocalOf { 1f }

/**
 * Minimum height of tappable controls. Under a finger this is 44 physical dp, converted back
 * into scene units (the scene is scaled down on phones, so 48 scene dp would be only ~27 real
 * dp); under a mouse Material's 40dp is enough.
 */
val TapTarget: Dp
    @Composable get() = if (codetrail.app.Platform.touch) maxOf(48.dp, 44.dp / LocalSceneScale.current) else 40.dp

/** Cards and slots grow by this on touch screens so a finger lands on them; 1 under a mouse. */
val TouchBoost: Float
    @Composable get() = if (codetrail.app.Platform.touch && LocalSceneScale.current < 1f) 1.2f else 1f

/** Dim behind modal overlays: strong enough that the dialog does not blend into the screen below. */
val Scrim = Color.Black.copy(alpha = 0.7f)

@Composable
fun characterPainter(c: Character): Painter = painterResource(c.art)

/** Round portrait. Locked characters are greyed out with a lock and their star threshold. */
@Composable
fun Avatar(
    c: Character,
    size: Dp,
    selected: Boolean = false,
    unlocked: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val painter = characterPainter(c)
    val grey = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    val sounds = LocalSounds.current
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            // Translucent disc only: the hero art has a transparent background and should show the chrome behind it.
            .background(Color.White.copy(alpha = if (!unlocked) 0.25f else if (selected) 0.45f else 0.35f))
            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color(0x55FFFFFF), CircleShape)
            .then(if (onClick != null && unlocked) Modifier.clickable { sounds.play(Sfx.CLICK); onClick() } else Modifier)
            .padding(size * 0.08f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter,
            null,
            Modifier.fillMaxSize().then(if (unlocked) Modifier else Modifier.alpha(0.35f)),
            colorFilter = if (unlocked) null else grey,
        )
        if (!unlocked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, null, Modifier.size(size * 0.34f), tint = Color.White)
                Text("${c.unlockStars}★", fontSize = (size.value * 0.21f).sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Big rounded menu button. */
@Composable
fun MenuButton(text: String, color: Color = Color.White, textColor: Color = Ink, enabled: Boolean = true, width: Dp = 300.dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.6f, stiffness = 900f))
    val sounds = LocalSounds.current
    Button(
        onClick = { sounds.play(Sfx.CLICK); onClick() },
        enabled = enabled,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = textColor, disabledContainerColor = color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(width).heightIn(min = TapTarget).padding(vertical = 2.dp).graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
}

/** The hero a profile currently plays as: the chosen one if unlocked, otherwise the starter. */
fun Profile.hero(): Character =
    Character.byId(characterId)?.takeIf { progress.totalStars >= it.unlockStars } ?: Character.All.first()

/** Row of hero portraits with lock state, used on the Profiles screen and in the menu dialog. */
@Composable
fun HeroPicker(profile: Profile, size: Dp = 84.dp, onChoose: (Character) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        for (c in Character.All) {
            val unlocked = profile.progress.totalStars >= c.unlockStars
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(c, size, selected = profile.hero().id == c.id, unlocked = unlocked) { onChoose(c) }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(c.name), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Modal hero picker over any screen. Click outside or pick a hero to close. */
@Composable
fun HeroPickerDialog(profile: Profile, onChoose: (Character) -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Scrim).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .background(MenuBackground, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.hero_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(20.dp))
            HeroPicker(profile, size = 96.dp) { onChoose(it); onDismiss() }
        }
    }
}

/** Modal yes / no dialog used for quitting and resetting. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    cancelText: String,
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Scrim).clickable(onClick = onCancel), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(440.dp)
                .background(MenuBackground, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(body, fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuButton(cancelText, width = 180.dp, onClick = onCancel)
                MenuButton(confirmText, color = if (danger) Color(0xFFE53935) else Accent, textColor = Color.White, width = 180.dp, onClick = onConfirm)
            }
        }
    }
}

