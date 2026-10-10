@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalResourceApi::class)

package codetrail.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.resources.ComposeEnvironment
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.LocalComposeEnvironment
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ScriptQualifier
import org.jetbrains.compose.resources.ThemeQualifier

/**
 * Compose resources on iOS follow NSLocale.preferredLanguages, which cannot be changed while the
 * app runs. Instead the chosen language is pushed into the resource environment directly through
 * [ProvideResourceLanguage], which wraps the UI in MainViewController.
 */
private var chosenLanguage: String? by mutableStateOf(null)

@Composable
actual fun applyLanguage(language: AppLanguage) {
    val system = Locale.current.language
    chosenLanguage = language.tag ?: system.takeIf { it in AppLanguage.Supported } ?: "en"
}

/** Resource environment driven by the app's language setting rather than the system locale. */
@Composable
fun ProvideResourceLanguage(content: @Composable () -> Unit) {
    val environment = remember {
        object : ComposeEnvironment {
            @Composable
            override fun rememberEnvironment(): ResourceEnvironment {
                val language = chosenLanguage ?: Locale.current.language
                val density = LocalDensity.current.density
                return remember(language, density) {
                    ResourceEnvironment(
                        LanguageQualifier(language),
                        ScriptQualifier(""),
                        RegionQualifier(""),
                        ThemeQualifier.LIGHT,
                        DensityQualifier.selectByDensity(density),
                    )
                }
            }
        }
    }
    CompositionLocalProvider(LocalComposeEnvironment provides environment, content = content)
}
