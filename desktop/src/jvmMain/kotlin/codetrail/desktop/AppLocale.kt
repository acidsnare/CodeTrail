package codetrail.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/** Languages the UI ships with. SYSTEM follows the OS locale, falling back to English. */
enum class AppLanguage(val tag: String?, val label: String) {
    SYSTEM(null, "Auto"),
    EN("en", "EN"),
    RU("ru", "RU"),
    UA("uk", "UA"),
    DA("da", "DA");

    companion object {
        val Supported = setOf("en", "ru", "uk", "da")
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SYSTEM }

/**
 * Applies the chosen language to the JVM default locale, which Compose resources read from,
 * and recomposes the whole subtree so every stringResource picks up the change.
 */
@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val systemLocale = remember { Locale.getDefault() }
    val target = language.tag?.let { Locale.forLanguageTag(it) }
        ?: systemLocale.takeIf { it.language in AppLanguage.Supported }
        ?: Locale.ENGLISH
    if (Locale.getDefault() != target) Locale.setDefault(target)
    key(language) {
        CompositionLocalProvider(LocalAppLanguage provides language, content)
    }
}
