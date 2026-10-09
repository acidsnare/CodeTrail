package codetrail.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import codetrail.app.AppState
import codetrail.app.res.Res
import codetrail.app.res.app_name
import codetrail.app.res.menu_continue
import codetrail.app.res.menu_play
import codetrail.app.res.menu_profiles
import codetrail.app.res.menu_quit
import codetrail.app.res.menu_settings
import codetrail.app.res.menu_stats
import codetrail.app.res.menu_subtitle
import codetrail.app.res.hero_change_hint
import codetrail.app.res.stars_total
import codetrail.app.theme.Character
import org.jetbrains.compose.resources.stringResource

@Composable
fun MenuScreen(app: AppState, onQuit: () -> Unit) {
    val profile = app.current
    val hero = profile?.hero() ?: Character.All.first()
    var pickHero by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MenuBackground)) {
        // Six buttons (Continue + Quit) plus the title must fit a 720-high scene on phones.
        // Buttons and hero are centred together as one group, with a fixed gap, so wide screens
        // do not open a hole between them.
        Row(
            Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(stringResource(Res.string.app_name), fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(stringResource(Res.string.menu_subtitle), fontSize = 22.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(28.dp))

                if (profile != null) {
                    if (app.canContinue) MenuButton(stringResource(Res.string.menu_continue), color = Accent, textColor = Color.White) { app.continueGame() }
                    MenuButton(stringResource(Res.string.menu_play), color = if (app.canContinue) Color.White else Accent, textColor = if (app.canContinue) Ink else Color.White) { app.goPlay() }
                }
                MenuButton(stringResource(Res.string.menu_profiles)) { app.goProfiles() }
                if (profile != null) MenuButton(stringResource(Res.string.menu_stats)) { app.goStats() }
                MenuButton(stringResource(Res.string.menu_settings)) { app.goSettings() }
                if (codetrail.app.Platform.canQuit) {
                    MenuButton(stringResource(Res.string.menu_quit), color = Color.White.copy(alpha = 0.15f), textColor = Color.White) { app.requestQuit() }
                }
            }

            Spacer(Modifier.width(140.dp))
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                // Idle "breathing": slow scale and a gentle sway so the portrait feels alive.
                val idle = rememberInfiniteTransition()
                val breath by idle.animateFloat(1f, 1.035f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse))
                val sway by idle.animateFloat(-2.5f, 2.5f, infiniteRepeatable(tween(3100, easing = FastOutSlowInEasing), RepeatMode.Reverse))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        characterPainter(hero), null,
                        Modifier
                            .size(380.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = profile != null) { pickHero = true }
                            .graphicsLayer { scaleX = breath; scaleY = breath; rotationZ = sway; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f) },
                    )
                    if (profile != null) {
                        Text(stringResource(Res.string.hero_change_hint), color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
                    }
                }
            }
        }
        // Player pill in the top-right corner, the same chrome as the game header: keeps the
        // player's identity apart from the action buttons. Tap to change the hero.
        if (profile != null) {
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Pill)
                    .clickable { pickHero = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.stars_total, profile.progress.totalStars), color = Star, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Avatar(hero, 44.dp, selected = true)
                Text(profile.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        if (pickHero && profile != null) {
            HeroPickerDialog(profile, onChoose = { app.chooseCharacter(it) }, onDismiss = { pickHero = false })
        }
    }
}
