package codetrail.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Compose resources in the browser follow navigator.language, which cannot be changed from
 * Kotlin. index.html overrides it before Compose starts from the "codetrail.lang" entry, so
 * switching the language here means storing the choice and reloading the page.
 */
@Composable
actual fun applyLanguage(language: AppLanguage) {
    LaunchedEffect(language) {
        val wanted = language.tag ?: ""
        val stored = Browser.get(LANG_KEY) ?: ""
        if (wanted != stored) {
            if (wanted.isEmpty()) Browser.remove(LANG_KEY) else Browser.set(LANG_KEY, wanted)
            Browser.reload()
        }
    }
}

internal const val LANG_KEY = "codetrail.lang"
