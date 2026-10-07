package codetrail.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Locale

/** Compose resources on the JVM read the default locale, so switching it is enough. */
@Composable
actual fun applyLanguage(language: AppLanguage) {
    val systemLocale = remember { Locale.getDefault() }
    val target = language.tag?.let { Locale.forLanguageTag(it) }
        ?: systemLocale.takeIf { it.language in AppLanguage.Supported }
        ?: Locale.ENGLISH
    if (Locale.getDefault() != target) Locale.setDefault(target)
}
