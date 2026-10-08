package codetrail.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** Languages the UI ships with. SYSTEM follows the OS locale, falling back to English. */
enum class AppLanguage(val tag: String?, val label: String) {
    // Auto first, then alphabetical by label: this is the order of the chips in Settings.
    SYSTEM(null, "Auto"),
    DA("da", "DA"),
    EN("en", "EN"),
    RO("ro", "RO"),
    RU("ru", "RU"),
    UA("uk", "UA");

    companion object {
        val Supported = entries.mapNotNull { it.tag }.toSet()
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SYSTEM }

/**
 * Applies the chosen language so Compose resources resolve to it, and recomposes the whole
 * subtree so every stringResource picks up the change.
 */
@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    applyLanguage(language)
    key(language) {
        CompositionLocalProvider(LocalAppLanguage provides language, content)
    }
}

/** Platform side: point the resource system at [language], or at the system language when SYSTEM. */
@Composable
expect fun applyLanguage(language: AppLanguage)
